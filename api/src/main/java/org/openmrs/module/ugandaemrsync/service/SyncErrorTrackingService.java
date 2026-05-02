package org.openmrs.module.ugandaemrsync.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncErrorType;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Service for tracking comprehensive sync error information.
 * Provides methods to record sync attempts, successes, and failures with detailed error tracking.
 */
@Component
public class SyncErrorTrackingService {

    private static final Log log = LogFactory.getLog(SyncErrorTrackingService.class);
    private static final int DEFAULT_MAX_CONSECUTIVE_FAILURES = 5;

    /**
     * Records a successful sync attempt with timestamp and status updates
     */
    public void recordSuccessfulSync(SyncFhirResource resource, UgandaEMRSyncService syncService) {
        try {
            Date syncDate = new Date();
            resource.markAsSuccessfullySynced(syncDate);
            syncService.saveFHIRResource(resource);

            log.info("Successfully synced resource: " + resource.getUuid() +
                    " at " + syncDate);
        } catch (Exception e) {
            log.error("Failed to record successful sync for resource: " + resource.getUuid(), e);
        }
    }

    /**
     * Records a failed sync attempt with detailed error information
     */
    public void recordFailedSync(SyncFhirResource resource, UgandaEMRSyncService syncService,
                                SyncErrorType errorType, String errorMessage, Throwable exception) {
        try {
            Date attemptDate = new Date();
            String detailedErrorMessage = createDetailedErrorMessage(errorType, errorMessage, exception);

            resource.markAsSyncFailed(attemptDate, errorType, detailedErrorMessage);
            syncService.saveFHIRResource(resource);

            log.warn("Failed to sync resource: " + resource.getUuid() +
                    " | Error Type: " + errorType.name() +
                    " | Consecutive Failures: " + resource.getConsecutiveFailureCount() +
                    " | Error: " + detailedErrorMessage);

            // Alert if resource has exceeded max consecutive failures
            if (resource.hasExceededMaxFailures(DEFAULT_MAX_CONSECUTIVE_FAILURES)) {
                log.error("CRITICAL: Resource " + resource.getUuid() +
                        " has exceeded " + DEFAULT_MAX_CONSECUTIVE_FAILURES +
                        " consecutive failures. Manual intervention may be required.");
            }
        } catch (Exception e) {
            log.error("Failed to record sync failure for resource: " + resource.getUuid(), e);
        }
    }

    /**
     * Records a sync attempt with HTTP status code details
     */
    public void recordHttpSyncResult(SyncFhirResource resource, UgandaEMRSyncService syncService,
                                    int statusCode, String statusMessage, boolean successful) {
        try {
            Date attemptDate = new Date();
            resource.setStatusCode(statusCode);
            resource.setStatusCodeDetail(statusMessage);

            if (successful) {
                recordSuccessfulSync(resource, syncService);
            } else {
                SyncErrorType errorType = mapHttpStatusCodeToErrorType(statusCode);
                resource.markAsSyncFailed(attemptDate, errorType, statusMessage);
                syncService.saveFHIRResource(resource);

                log.warn("HTTP sync failed for resource: " + resource.getUuid() +
                        " | HTTP Status: " + statusCode +
                        " | Error Type: " + errorType.name() +
                        " | Message: " + statusMessage);
            }
        } catch (Exception e) {
            log.error("Failed to record HTTP sync result for resource: " + resource.getUuid(), e);
        }
    }

    /**
     * Records a resource generation failure (during FHIR/JSON creation phase)
     */
    public void recordResourceGenerationFailure(SyncFhirResource resource, UgandaEMRSyncService syncService,
                                              Throwable exception, String contextInfo) {
        String errorMessage = "Failed to generate resource: " + contextInfo;
        recordFailedSync(resource, syncService, SyncErrorType.RESOURCE_GENERATION_FAILED,
                        errorMessage, exception);
    }

    /**
     * Records a JSON transformation failure
     */
    public void recordJsonTransformationFailure(SyncFhirResource resource, UgandaEMRSyncService syncService,
                                              Throwable exception, String contextInfo) {
        String errorMessage = "Failed to transform resource to JSON: " + contextInfo;
        recordFailedSync(resource, syncService, SyncErrorType.JSON_TRANSFORMATION_FAILED,
                        errorMessage, exception);
    }

    /**
     * Records a configuration error
     */
    public void recordConfigurationError(SyncFhirResource resource, UgandaEMRSyncService syncService,
                                       String configIssue) {
        String errorMessage = "Configuration error: " + configIssue;
        recordFailedSync(resource, syncService, SyncErrorType.CONFIGURATION_ERROR,
                        errorMessage, null);
    }

    /**
     * Creates a detailed error message including exception stack trace if available
     */
    private String createDetailedErrorMessage(SyncErrorType errorType, String baseMessage, Throwable exception) {
        StringBuilder message = new StringBuilder();
        message.append(errorType.getDescription()).append(": ");
        message.append(baseMessage);

        if (exception != null) {
            message.append(" | Exception: ").append(exception.getClass().getSimpleName());
            if (exception.getMessage() != null) {
                message.append(" - ").append(exception.getMessage());
            }
        }

        return message.toString();
    }

    /**
     * Maps HTTP status codes to specific error types for better categorization
     */
    private SyncErrorType mapHttpStatusCodeToErrorType(int statusCode) {
        if (statusCode >= 400 && statusCode < 500) {
            if (statusCode == 401 || statusCode == 403) {
                return SyncErrorType.HTTP_AUTHENTICATION_FAILED;
            }
            return SyncErrorType.HTTP_CLIENT_ERROR;
        } else if (statusCode >= 500 && statusCode < 600) {
            return SyncErrorType.HTTP_SERVER_ERROR;
        } else if (statusCode == -1) { // Custom code for connection failures
            return SyncErrorType.HTTP_CONNECTION_FAILED;
        } else {
            return SyncErrorType.UNKNOWN_ERROR;
        }
    }

    /**
     * Gets comprehensive sync status information for a resource
     */
    public String getSyncStatusSummary(SyncFhirResource resource) {
        StringBuilder summary = new StringBuilder();

        summary.append("Resource UUID: ").append(resource.getUuid()).append("\n");
        summary.append("Synced: ").append(resource.getSynced() != null ? resource.getSynced() : "false").append("\n");

        if (resource.getLastSyncAttempt() != null) {
            summary.append("Last Sync Attempt: ").append(resource.getLastSyncAttempt()).append("\n");
        }

        if (resource.getDateSynced() != null && resource.getSynced()) {
            summary.append("Last Successful Sync: ").append(resource.getDateSynced()).append("\n");
        }

        if (resource.getConsecutiveFailureCount() != null && resource.getConsecutiveFailureCount() > 0) {
            summary.append("Consecutive Failures: ").append(resource.getConsecutiveFailureCount()).append("\n");
        }

        if (resource.getSyncRetryCount() != null && resource.getSyncRetryCount() > 0) {
            summary.append("Total Retry Attempts: ").append(resource.getSyncRetryCount()).append("\n");
        }

        if (resource.getLastSyncErrorType() != null) {
            summary.append("Last Error Type: ").append(resource.getLastSyncErrorType()).append("\n");
        }

        if (resource.getLastSyncErrorMessage() != null) {
            summary.append("Last Error Message: ").append(resource.getLastSyncErrorMessage()).append("\n");
        }

        if (resource.getStatusCode() != null) {
            summary.append("HTTP Status Code: ").append(resource.getStatusCode()).append("\n");
        }

        if (resource.getStatusCodeDetail() != null) {
            summary.append("HTTP Status Detail: ").append(resource.getStatusCodeDetail()).append("\n");
        }

        return summary.toString();
    }
}