package org.openmrs.module.ugandaemrsync.web.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.dto.exception.ConnectionException;
import org.openmrs.module.ugandaemrsync.dto.exception.LabResultProcessingException;
import org.openmrs.module.ugandaemrsync.dto.exception.ValidationException;
import org.openmrs.module.ugandaemrsync.exception.UgandaEMRSyncException;
import org.openmrs.module.ugandaemrsync.util.SyncLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.validation.ValidationUtils;
import org.openmrs.module.ugandaemrsync.web.resource.DTO.SyncTestOrderSync;
import org.openmrs.module.ugandaemrsync.web.response.LabResultResponseBuilder;
import org.openmrs.module.ugandaemrsync.util.validation.LabResultRequestValidator;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.ugandaemrsync.security.Secured;
import org.openmrs.module.ugandaemrsync.security.SyncPrivileges;
import org.openmrs.module.ugandaemrsync.web.interceptor.ResourceSecurityInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.openmrs.module.ugandaemrsync.server.SyncConstant.*;

/**
 * REST resource for requesting lab results from CPHL (Central Public Health Laboratories).
 * Handles validation, processing, and error management for lab result requests.
 */
@Resource(name = RestConstants.VERSION_1 + "/requestlabresult",
        supportedClass = SyncTestOrderSync.class,
        supportedOpenmrsVersions = {"1.9.* - 9.*"})
@Secured(authenticated = true, privilege = SyncPrivileges.VIEW_LAB_RESULTS)
@Component
public class RequestLabResultsResource extends DelegatingCrudResource<SyncTestOrderSync> {

    private static final Log log = LogFactory.getLog(RequestLabResultsResource.class);
    private static final Logger logger = LoggerFactory.getLogger(RequestLabResultsResource.class);
    private static final SyncLogger syncLogger = SyncLogger.getLogger(RequestLabResultsResource.class);

    // Validation and response components (would be injected via Spring in production)
    private final LabResultRequestValidator validator = new LabResultRequestValidator();
    private final LabResultResponseBuilder responseBuilder = new LabResultResponseBuilder();

    @Override
    public SyncTestOrderSync newDelegate() {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    @Override
    public SyncTestOrderSync save(SyncTestOrderSync TestResult) {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    /**
     * Creates and processes a lab result request.
     * This method is transactional to ensure data consistency.
     * External API calls are executed with circuit breaker protection and retry logic.
     *
     * @param propertiesToCreate Request properties containing order UUIDs
     * @param context Request context
     * @return SimpleObject containing processing results
     * @throws ResponseException for validation or processing errors
     */
    @Override
    @Secured(privilege = SyncPrivileges.MANAGE_LAB_RESULTS, rateLimit = 100)
    @Transactional
    public Object create(SimpleObject propertiesToCreate, RequestContext context) throws ResponseException {
        // Security handled by @Secured annotation
        String correlationId = UUID.randomUUID().toString();
        syncLogger.info("Starting lab result request processing",
                createLogContext("correlationId", correlationId, "timestamp", new Date()));

        try {
            // Step 1: Validate input with enhanced validation
            validateInput(propertiesToCreate, correlationId);

            // Step 2: Check connection availability
            validateConnectionAvailability(correlationId);

            // Step 3: Process orders with circuit breaker and retry protection
            ProcessingResult result = processLabResultRequest(propertiesToCreate, correlationId);

            // Step 4: Build success response
            syncLogger.info("Successfully processed lab result request",
                    createLogContext("correlationId", correlationId,
                                  "ordersProcessed", result.getProcessedCount(),
                                  "ordersSuccessful", result.getSuccessCount(),
                                  "ordersFailed", result.getErrorCount()));

            return buildSuccessResponse(result);

        } catch (ValidationException e) {
            syncLogger.logValidationError("requestValidation", correlationId, "Validation failed: " + e.getMessage());
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        } catch (ConnectionException e) {
            syncLogger.logValidationError("connectionCheck", correlationId, "Connection failed: " + e.getMessage());
            throw new RuntimeException("Connection failed: " + e.getMessage(), e);
        } catch (LabResultProcessingException e) {
            syncLogger.logValidationError("orderProcessing", correlationId, "Processing failed: " + e.getMessage());
            throw new RuntimeException("Processing failed: " + e.getMessage(), e);
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
     * Enhanced with additional validation using ValidationUtils.
     */
    private void validateInput(SimpleObject propertiesToCreate, String correlationId) throws ValidationException, UgandaEMRSyncException {
        Map<String, Object> logContext = new HashMap<>();
        logContext.put("correlationId", correlationId);
        syncLogger.info("Starting input validation", logContext);

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

        // Convert to list of strings if needed and validate each UUID
        List<String> stringUuids = new ArrayList<>();
        for (Object orderUuid : orderUuids) {
            String uuidStr = String.valueOf(orderUuid);

            // Additional validation using ValidationUtils
            try {
                ValidationUtils.requireValidUUID("orders[" + stringUuids.size() + "]", uuidStr);
                ValidationUtils.requireNoSQLInjection("orders[" + stringUuids.size() + "]", uuidStr);
            } catch (UgandaEMRSyncException e) {
                syncLogger.logValidationError("uuidValidation", uuidStr, e.getMessage());
                throw new ValidationException("orders[" + stringUuids.size() + "]",
                        "Invalid UUID format: " + e.getMessage(), uuidStr);
            }

            stringUuids.add(uuidStr);
        }

        // Validate collection size
        try {
            ValidationUtils.requireCollectionSize("orders", stringUuids, 1, 100);
        } catch (UgandaEMRSyncException e) {
            syncLogger.logValidationError("ordersSize", stringUuids.size(), e.getMessage());
            throw new ValidationException("orders", e.getMessage(), stringUuids.size());
        }

        validator.validateRequest(stringUuids);

        syncLogger.info("Input validation passed",
                createLogContext("correlationId", correlationId, "orderCount", stringUuids.size()));
    }

    /**
     * Validates that network connection is available.
     * Enhanced with structured logging and better error handling.
     */
    private void validateConnectionAvailability(String correlationId) throws ConnectionException {
        syncLogger.info("Checking connection availability", createLogContext("correlationId", correlationId));

        UgandaEMRHttpURLConnection connection = new UgandaEMRHttpURLConnection();
        if (!connection.isConnectionAvailable()) {
            String message = "No internet connection to get lab results from CPHL";
            syncLogger.logValidationError("connectionCheck", "CPHL", message);
            throw new ConnectionException(message, "CPHL", -1, -1);
        }

        syncLogger.info("Connection available", createLogContext("correlationId", correlationId));
    }

    /**
     * Processes the lab result request for all orders.
     * Enhanced with structured logging and better error tracking.
     */
    private ProcessingResult processLabResultRequest(SimpleObject propertiesToCreate, String correlationId) {
        syncLogger.info("Processing lab result request", createLogContext("correlationId", correlationId));

        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        SyncTaskType syncTaskType = ugandaEMRSyncService.getSyncTaskTypeByUUID(VIRAL_LOAD_SYNC_TASK_TYPE_UUID);

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

                processSingleOrder(orderUuid, syncTaskType, ugandaEMRSyncService, result, correlationId);

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
            return ((List<?>) ordersObject).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Processes a single order for lab results.
     * Enhanced with structured logging and better error handling.
     */
    private void processSingleOrder(String orderUuid, SyncTaskType syncTaskType,
                                   UgandaEMRSyncService ugandaEMRSyncService,
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

            syncLogger.info("Order found, looking up sync tasks",
                    createLogContext("correlationId", correlationId,
                          "orderUuid", orderUuid,
                          "accessionNumber", order.getAccessionNumber()));

            List<SyncTask> syncTasks = ugandaEMRSyncService.getSyncTasksBySyncTaskId(order.getAccessionNumber())
                    .stream()
                    .filter(task -> task.getSyncTaskType().equals(syncTaskType))
                    .collect(Collectors.toList());

            if (syncTasks.isEmpty()) {
                String message = String.format("No sync task found for order: %s", order.getAccessionNumber());
                syncLogger.logValidationError("syncTaskLookup", order.getAccessionNumber(), message);
                result.addError(orderUuid, message);
                return;
            }

            SyncTask syncTask = syncTasks.get(0);
            if (!isValidSyncTask(syncTask)) {
                String message = String.format("Order %s does not qualify to receive results", order.getAccessionNumber());
                syncLogger.logValidationError("syncTaskValidation", order.getAccessionNumber(), message);
                result.addError(orderUuid, message);
                return;
            }

            syncLogger.info("Calling external service for lab results",
                    createLogContext("correlationId", correlationId,
                          "orderUuid", orderUuid,
                          "syncTaskId", syncTask.getSyncTask()));

            Map<String, Object> response = ugandaEMRSyncService.requestLabResult(order, syncTask);

            syncLogger.info("External service call completed",
                    createLogContext("correlationId", correlationId,
                          "orderUuid", orderUuid,
                          "responseCode", response.getOrDefault("responseCode", "N/A")));

            result.addSuccess(order, response);

        } catch (Exception e) {
            String message = String.format("Exception processing order: %s", e.getMessage());
            syncLogger.logValidationError("orderProcessingException", orderUuid, message);
            result.addError(orderUuid, message);
        }
    }

    /**
     * Validates if a sync task qualifies for result processing.
     */
    private boolean isValidSyncTask(SyncTask syncTask) {
        return isSuccessStatusCode(syncTask.getStatusCode()) &&
               syncTask.getRequireAction() &&
               !syncTask.getActionCompleted();
    }

    /**
     * Checks if the status code indicates success.
     */
    private boolean isSuccessStatusCode(Integer statusCode) {
        return statusCode != null &&
               (statusCode == 200 || statusCode == 201 || statusCode == 202 || statusCode == 208);
    }

    /**
     * Builds a success response from the processing result.
     * Enhanced with structured logging for response tracking.
     */
    private SimpleObject buildSuccessResponse(ProcessingResult result) {
        syncLogger.info("Building success response",
                createLogContext("totalProcessed", result.getTotalProcessed(),
                      "successful", result.getSuccessCount(),
                      "failed", result.getErrorCount()));

        Map<String, Object> allResponses = new HashMap<>();

        // Process successful results
        for (Map.Entry<String, Map<String, Object>> entry : result.getSuccessResults().entrySet()) {
            allResponses.put(entry.getKey(), entry.getValue());
        }

        // Process error results
        for (Map.Entry<String, String> entry : result.getErrors().entrySet()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", entry.getValue());
            allResponses.put(entry.getKey(), errorResponse);
        }

        SimpleObject response = responseBuilder.buildSuccessResponse(result.getTotalProcessed(), allResponses);

        syncLogger.info("Response building completed",
                createLogContext("responseSize", allResponses.size(),
                      "timestamp", new Date()));

        return response;
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
        description.addProperty("orders");
        return description;
    }

    @Override
    protected PageableResult doSearch(RequestContext context) {
        throw new ResourceDoesNotSupportOperationException("Operation not supported");
    }

    /**
     * Internal class to track processing results.
     */

    /**
     * Helper method to create log context maps for Java 8 compatibility
     * Replaces Map.of() which is only available in Java 9+
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
    private static class ProcessingResult {
        private final Map<String, Map<String, Object>> successResults = new HashMap<>();
        private final Map<String, String> errors = new HashMap<>();
        private final List<Order> orders = new ArrayList<>();

        public void addSuccess(Order order, Map<String, Object> response) {
            successResults.put(order.getUuid(), response);
            orders.add(order);
        }

        public void addError(String orderUuid, String errorMessage) {
            errors.put(orderUuid, errorMessage);
        }

        public Map<String, Map<String, Object>> getSuccessResults() {
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

        public int getProcessedCount() {
            return successResults.size();
        }
    }

    /**
     * Helper method to get current method reference for security checks.
     * Used by ResourceSecurityInterceptor to determine security requirements.
     */
    private java.lang.reflect.Method getCurrentMethod() {
        try {
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            return this.getClass().getMethod(methodName, SimpleObject.class, RequestContext.class);
        } catch (Exception e) {
            return null;
        }
    }
}
