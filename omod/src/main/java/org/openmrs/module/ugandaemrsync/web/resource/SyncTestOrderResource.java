package org.openmrs.module.ugandaemrsync.web.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.dto.exception.ConnectionException;
import org.openmrs.module.ugandaemrsync.dto.exception.ValidationException;
import org.openmrs.module.ugandaemrsync.exception.UgandaEMRSyncException;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.security.Secured;
import org.openmrs.module.ugandaemrsync.security.SyncPrivileges;
import org.openmrs.module.ugandaemrsync.util.SyncLogger;
import org.openmrs.module.ugandaemrsync.validation.ValidationUtils;
import org.openmrs.module.ugandaemrsync.web.resource.DTO.SyncTestOrderSync;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.*;

/**
 * REST resource for syncing test orders to CPHL (Central Public Health Laboratories).
 * Handles validation, processing, and error management for test order synchronization.
 */
@Resource(name = RestConstants.VERSION_1 + "/syncTestOrder", supportedClass = SyncTestOrderSync.class, supportedOpenmrsVersions = {"1.9.* - 9.*"})
@Secured(authenticated = true)
public class SyncTestOrderResource extends DelegatingCrudResource<SyncTestOrderSync> {

    private static final Log log = LogFactory.getLog(SyncTestOrderResource.class);
    private static final SyncLogger syncLogger = SyncLogger.getLogger(SyncTestOrderResource.class);

    @Override
    public SyncTestOrderSync newDelegate() {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public SyncTestOrderSync save(SyncTestOrderSync TestResult) {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    /**
     * Creates and processes test order synchronization requests.
     * This method validates input, checks connectivity, and syncs orders to CPHL.
     *
     * @param propertiesToCreate Request properties containing order UUIDs
     * @param context Request context
     * @return SimpleObject containing processing results with success/error details
     * @throws ResponseException for validation or processing errors
     */
    @Override
    @Secured(privilege = SyncPrivileges.MANAGE_LAB_ORDERS, rateLimit = 50)
    public Object create(SimpleObject propertiesToCreate, RequestContext context) throws ResponseException {
        String correlationId = UUID.randomUUID().toString();
        syncLogger.info("Starting test order sync processing",
                createLogContext("correlationId", correlationId, "timestamp", new Date()));

        try {
            // Step 1: Validate input
            validateInput(propertiesToCreate, correlationId);

            // Step 2: Check connection availability
            validateConnectionAvailability(correlationId);

            // Step 3: Process orders
            ProcessingResult result = processTestOrderSync(propertiesToCreate, correlationId);

            // Step 4: Build and return response
            syncLogger.info("Successfully processed test order sync",
                    createLogContext("correlationId", correlationId,
                                  "ordersProcessed", result.getTotalProcessed(),
                                  "ordersSuccessful", result.getSuccessCount(),
                                  "ordersFailed", result.getErrorCount()));

            return buildResponse(result);

        } catch (ValidationException e) {
            syncLogger.logValidationError("requestValidation", correlationId, "Validation failed: " + e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        } catch (ConnectionException e) {
            syncLogger.logValidationError("connectionCheck", correlationId, "Connection failed: " + e.getMessage());
            throw new RuntimeException("Connection failed: " + e.getMessage(), e);
        } catch (UgandaEMRSyncException e) {
            syncLogger.logValidationError("syncOperation", correlationId, "Sync operation failed: " + e.getMessage());
            throw new RuntimeException("Sync operation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            syncLogger.logValidationError("unexpectedError", correlationId, "Internal error: " + e.getMessage());
            throw new RuntimeException("Internal error: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the input request properties.
     */
    private void validateInput(SimpleObject propertiesToCreate, String correlationId) throws ValidationException, UgandaEMRSyncException {
        syncLogger.info("Starting input validation", createLogContext("correlationId", correlationId));

        if (propertiesToCreate == null) {
            syncLogger.logValidationError("requestBody", null, "Request body cannot be null");
            throw new ValidationException("request", "Request body cannot be null", null);
        }

        Object ordersObject = propertiesToCreate.get("orders");
        if (!(ordersObject instanceof List)) {
            syncLogger.logValidationError("orders", ordersObject, "Orders must be a list");
            throw new ValidationException("orders", "Orders must be a list", ordersObject);
        }

        @SuppressWarnings("unchecked")
        List<String> orderUuids = (List<String>) ordersObject;

        if (orderUuids.isEmpty()) {
            syncLogger.logValidationError("orders", orderUuids, "Orders list cannot be empty");
            throw new ValidationException("orders", "Orders list cannot be empty", orderUuids);
        }

        // Validate each UUID
        for (int i = 0; i < orderUuids.size(); i++) {
            String uuidStr = String.valueOf(orderUuids.get(i));
            try {
                ValidationUtils.requireValidUUID("orders[" + i + "]", uuidStr);
                ValidationUtils.requireNoSQLInjection("orders[" + i + "]", uuidStr);
            } catch (UgandaEMRSyncException e) {
                syncLogger.logValidationError("uuidValidation", uuidStr, e.getMessage());
                throw new ValidationException("orders[" + i + "]",
                        "Invalid UUID format: " + e.getMessage(), uuidStr);
            }
        }

        // Validate collection size
        ValidationUtils.requireCollectionSize("orders", orderUuids, 1, 100);

        syncLogger.info("Input validation passed",
                createLogContext("correlationId", correlationId, "orderCount", orderUuids.size()));
    }

    /**
     * Validates that network connection is available.
     */
    private void validateConnectionAvailability(String correlationId) throws ConnectionException {
        syncLogger.info("Checking connection availability", createLogContext("correlationId", correlationId));

        UgandaEMRHttpURLConnection connection = new UgandaEMRHttpURLConnection();
        if (!connection.isConnectionAvailable()) {
            String message = "No internet connection to send orders to CPHL";
            syncLogger.logValidationError("connectionCheck", "CPHL", message);
            throw new ConnectionException(message, "CPHL", -1, -1);
        }

        syncLogger.info("Connection available", createLogContext("correlationId", correlationId));
    }

    /**
     * Processes the test order synchronization for all orders.
     */
    private ProcessingResult processTestOrderSync(SimpleObject propertiesToCreate, String correlationId) {
        syncLogger.info("Processing test order sync", createLogContext("correlationId", correlationId));

        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        List<String> orderUuids = extractOrderUuids(propertiesToCreate);
        ProcessingResult result = new ProcessingResult();

        syncLogger.info("Starting batch order processing",
                createLogContext("correlationId", correlationId, "totalOrders", orderUuids.size()));

        for (int i = 0; i < orderUuids.size(); i++) {
            String orderUuid = orderUuids.get(i);
            try {
                syncLogger.info("Processing individual order",
                        createLogContext("correlationId", correlationId,
                              "orderUuid", orderUuid,
                              "progress", String.format("%d/%d", i + 1, orderUuids.size())));

                processSingleOrder(orderUuid, ugandaEMRSyncService, result, correlationId);

            } catch (Exception e) {
                String errorMsg = String.format("Failed to process order: %s", e.getMessage());
                syncLogger.logValidationError("orderProcessing", orderUuid, errorMsg);
                result.addError(orderUuid, errorMsg);
            }
        }

        syncLogger.info("Batch processing complete",
                createLogContext("correlationId", correlationId,
                      "successful", result.getSuccessCount(),
                      "failed", result.getErrorCount(),
                      "total", result.getTotalProcessed()));

        return result;
    }

    /**
     * Extracts order UUIDs from the request properties.
     */
    private List<String> extractOrderUuids(SimpleObject propertiesToCreate) {
        Object ordersObject = propertiesToCreate.get("orders");
        if (ordersObject instanceof List) {
            List<String> uuids = new ArrayList<>();
            for (Object item : (List<?>) ordersObject) {
                uuids.add(String.valueOf(item));
            }
            return uuids;
        }
        return Collections.emptyList();
    }

    /**
     * Processes a single order for synchronization.
     */
    private void processSingleOrder(String orderUuid, UgandaEMRSyncService ugandaEMRSyncService,
                                   ProcessingResult result, String correlationId) {

        syncLogger.info("Processing single order", createLogContext("correlationId", correlationId, "orderUuid", orderUuid));

        try {
            Order order = Context.getOrderService().getOrderByUuid(orderUuid);
            if (order == null) {
                String message = String.format("Order not found: %s", orderUuid);
                syncLogger.logValidationError("orderLookup", orderUuid, message);
                result.addError(orderUuid, message);
                return;
            }

            syncLogger.info("Order found, sending sync request",
                    createLogContext("correlationId", correlationId,
                          "orderUuid", orderUuid,
                          "accessionNumber", order.getAccessionNumber()));

            // Now returns SyncTask instead of Map
            SyncTask syncTask = ugandaEMRSyncService.sendSingleViralLoadOrder(order);

            syncLogger.info("Sync request completed",
                    createLogContext("correlationId", correlationId,
                          "orderUuid", orderUuid,
                          "syncTaskStatusCode", syncTask.getStatusCode(),
                          "syncTaskStatus", syncTask.getStatus()));

            // Check statusCode - no string matching needed!
            if (isSuccessStatusCode(syncTask.getStatusCode())) {
                // Success - include response details from SyncTask
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("responseCode", syncTask.getStatusCode());
                responseData.put("responseMessage", syncTask.getStatus());
                result.addSuccess(order, responseData);
            } else {
                // Failure
                String errorMessage = String.format("Order %s: %s (HTTP %d)",
                        order.getAccessionNumber(),
                        syncTask.getStatus(),
                        syncTask.getStatusCode());
                syncLogger.logValidationError("syncRequestFailure", orderUuid, errorMessage);
                result.addError(orderUuid, errorMessage);
            }

        } catch (org.openmrs.module.ugandaemrsync.exception.UgandaEMRSyncException e) {
            String message = String.format("Sync failed: %s", e.getMessage());
            syncLogger.logValidationError("syncException", orderUuid, message);
            result.addError(orderUuid, message);
        } catch (Exception e) {
            String message = String.format("Exception processing order: %s", e.getMessage());
            syncLogger.logValidationError("orderProcessingException", orderUuid, message);
            result.addError(orderUuid, message);
        }
    }

    /**
     * Check if a status code indicates success.
     * Matches the logic in UgandaEMRSyncServiceImpl.
     */
    private boolean isSuccessStatusCode(Integer statusCode) {
        return statusCode != null &&
               (statusCode == 200 || statusCode == 201 || statusCode == 202 || statusCode == 208);
    }

    /**
     * Builds response from the processing result.
     * Returns success response if all orders succeeded, otherwise throws exception.
     */
    private SimpleObject buildResponse(ProcessingResult result) {
        syncLogger.info("Building response",
                createLogContext("totalProcessed", result.getTotalProcessed(),
                      "successful", result.getSuccessCount(),
                      "failed", result.getErrorCount()));

        // If there are any failures, throw an exception with details
        if (result.getErrorCount() > 0) {
            String errorMsg = String.format("Failed to sync %d of %d orders. Details: %s",
                    result.getErrorCount(),
                    result.getTotalProcessed(),
                    result.getErrors().values());
            throw new RuntimeException(errorMsg);
        }

        // All succeeded - build success response
        Map<String, Object> allResponses = new HashMap<>();

        // Process successful results - SyncTask objects
        for (Map.Entry<String, org.openmrs.module.ugandaemrsync.model.SyncTask> entry : result.getSuccessResults().entrySet()) {
            org.openmrs.module.ugandaemrsync.model.SyncTask syncTask = entry.getValue();
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("accessionNumber", syncTask.getSyncTask());
            taskData.put("statusCode", syncTask.getStatusCode());
            taskData.put("status", syncTask.getStatus());
            if (syncTask.getDateSent() != null) {
                taskData.put("dateSent", syncTask.getDateSent().toString());
            }
            allResponses.put(entry.getKey(), taskData);
        }

        // Build success response
        SimpleObject response = new SimpleObject();
        response.put("status", "success");
        response.put("message", String.format("Successfully synced %d orders", result.getSuccessCount()));
        response.put("orders", allResponses);

        syncLogger.info("Response building completed",
                createLogContext("responseSize", allResponses.size(),
                      "timestamp", new Date()));

        return response;
    }

    /**
     * Helper method to create log context maps.
     */
    private Map<String, Object> createLogContext(Object... keyValuePairs) {
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                context.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
            }
        }
        return context;
    }

    /**
     * Internal class to track processing results.
     * Changed to track SyncTask objects for success results.
     */
    private static class ProcessingResult {
        private final Map<String, org.openmrs.module.ugandaemrsync.model.SyncTask> successResults = new HashMap<>();
        private final Map<String, String> errors = new HashMap<>();
        private final List<Order> orders = new ArrayList<>();

        public void addSuccess(Order order, Map<String, Object> response) {
            // Create a lightweight SyncTask-like response for REST output
            // The actual SyncTask is persisted in the database
            org.openmrs.module.ugandaemrsync.model.SyncTask syncTask = new org.openmrs.module.ugandaemrsync.model.SyncTask();
            syncTask.setSyncTask(order.getAccessionNumber());
            syncTask.setStatusCode((Integer) response.get("responseCode"));
            syncTask.setStatus((String) response.get("responseMessage"));
            successResults.put(order.getUuid(), syncTask);
            orders.add(order);
        }

        public void addError(String orderUuid, String errorMessage) {
            errors.put(orderUuid, errorMessage);
        }

        public Map<String, org.openmrs.module.ugandaemrsync.model.SyncTask> getSuccessResults() {
            return successResults;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public int getSuccessCount() {
            return successResults.size();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getTotalProcessed() {
            return successResults.size() + errors.size();
        }
    }

    @Override
    public Object update(String uuid, SimpleObject propertiesToUpdate, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public SyncTestOrderSync getByUniqueId(String uniqueId) {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public NeedsPaging<SyncTestOrderSync> doGetAll(RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public List<Representation> getAvailableRepresentations() {
        return Arrays.asList(Representation.DEFAULT, Representation.FULL);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        if (rep instanceof DefaultRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("orderList");
            description.addProperty("responseList");
            description.addSelfLink();
            return description;
        } else if (rep instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("orderList", Representation.REF);
            description.addProperty("responseList", Representation.REF);
            description.addSelfLink();
            description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            return description;
        } else if (rep instanceof RefRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("orderList", Representation.REF);
            description.addProperty("responseList", Representation.REF);
            description.addSelfLink();
            return description;
        }
        return null;
    }

    @Override
    protected void delete(SyncTestOrderSync TestResult, String s, RequestContext requestContext) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public void purge(SyncTestOrderSync TestResult, RequestContext requestContext) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("visit");
        return description;
    }

    @Override
    protected PageableResult doSearch(RequestContext context) {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }
}
