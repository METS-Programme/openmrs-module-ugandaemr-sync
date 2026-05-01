package org.openmrs.module.ugandaemrsync.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncErrorType;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Service for tracking comprehensive error information during FHIR case-based resource generation.
 * Provides detailed tracking of case resource generation failures and success.
 */
@Component
public class SyncCaseGenerationTrackingService {

    private static final Log log = LogFactory.getLog(SyncCaseGenerationTrackingService.class);
    private static final int DEFAULT_MAX_CONSECUTIVE_GENERATION_FAILURES = 3;

    /**
     * Records a successful resource generation for a case
     */
    public void recordSuccessfulGeneration(SyncFhirCase syncFhirCase, SyncFhirResource generatedResource,
                                         UgandaEMRSyncService syncService) {
        try {
            Date generationDate = new Date();
            syncFhirCase.markAsGenerationSuccess(generationDate);
            syncService.saveSyncFHIRCase(syncFhirCase);

            log.info("Successfully generated resource for case: " + syncFhirCase.getCaseIdentifier() +
                    " | Resource ID: " + generatedResource.getId() +
                    " | Patient ID: " + (syncFhirCase.getPatient() != null ? syncFhirCase.getPatient().getId() : "Unknown") +
                    " | Date: " + generationDate);
        } catch (Exception e) {
            log.error("Failed to record successful generation for case: " + syncFhirCase.getCaseIdentifier(), e);
        }
    }

    /**
     * Records a failed resource generation attempt for a case
     */
    public void recordFailedGeneration(SyncFhirCase syncFhirCase, UgandaEMRSyncService syncService,
                                     SyncErrorType errorType, String errorMessage, Throwable exception) {
        try {
            Date attemptDate = new Date();
            String detailedErrorMessage = createDetailedErrorMessage(errorType, errorMessage, exception);

            syncFhirCase.markAsGenerationFailure(attemptDate, errorType, detailedErrorMessage);
            syncService.saveSyncFHIRCase(syncFhirCase);

            log.warn("Failed to generate resource for case: " + syncFhirCase.getCaseIdentifier() +
                    " | Patient ID: " + (syncFhirCase.getPatient() != null ? syncFhirCase.getPatient().getId() : "Unknown") +
                    " | Error Type: " + errorType.name() +
                    " | Consecutive Failures: " + syncFhirCase.getConsecutiveGenerationFailures() +
                    " | Error: " + detailedErrorMessage);

            // Alert if case has exceeded max consecutive failures
            if (syncFhirCase.hasExceededMaxGenerationFailures(DEFAULT_MAX_CONSECUTIVE_GENERATION_FAILURES)) {
                log.error("CRITICAL: Case " + syncFhirCase.getCaseIdentifier() +
                        " has exceeded " + DEFAULT_MAX_CONSECUTIVE_GENERATION_FAILURES +
                        " consecutive generation failures. Manual intervention may be required." +
                        " | Patient ID: " + (syncFhirCase.getPatient() != null ? syncFhirCase.getPatient().getId() : "Unknown") +
                        " | Last Error: " + detailedErrorMessage);
            }
        } catch (Exception e) {
            log.error("Failed to record generation failure for case: " + syncFhirCase.getCaseIdentifier(), e);
        }
    }

    /**
     * Records a resource generation failure during FHIR resource creation
     */
    public void recordFhirGenerationFailure(SyncFhirCase syncFhirCase, UgandaEMRSyncService syncService,
                                          Throwable exception, String contextInfo) {
        String errorMessage = "Failed to generate FHIR resource for case: " + contextInfo;
        recordFailedGeneration(syncFhirCase, syncService,
                            SyncErrorType.RESOURCE_GENERATION_FAILED, errorMessage, exception);
    }

    /**
     * Records a failure when generated resource is null or empty
     */
    public void recordNullOrEmptyResourceFailure(SyncFhirCase syncFhirCase, UgandaEMRSyncService syncService,
                                                String resourceType, String caseContext) {
        String errorMessage = "Generated " + resourceType + " resource was null or empty for case context: " + caseContext;
        recordFailedGeneration(syncFhirCase, syncService,
                            SyncErrorType.RESOURCE_GENERATION_FAILED, errorMessage, null);
    }

    /**
     * Records a JSON transformation failure during case resource processing
     */
    public void recordJsonTransformationFailure(SyncFhirCase syncFhirCase, UgandaEMRSyncService syncService,
                                              Throwable exception, String transformationContext) {
        String errorMessage = "Failed to transform case resource to JSON: " + transformationContext;
        recordFailedGeneration(syncFhirCase, syncService,
                            SyncErrorType.JSON_TRANSFORMATION_FAILED, errorMessage, exception);
    }

    /**
     * Records a data validation failure for case resource generation
     */
    public void recordDataValidationError(SyncFhirCase syncFhirCase, UgandaEMRSyncService syncService,
                                        String validationError) {
        String errorMessage = "Data validation failed for case: " + validationError;
        recordFailedGeneration(syncFhirCase, syncService,
                            SyncErrorType.DATA_VALIDATION_FAILED, errorMessage, null);
    }

    /**
     * Records a configuration error affecting case resource generation
     */
    public void recordConfigurationError(SyncFhirCase syncFhirCase, UgandaEMRSyncService syncService,
                                       String configIssue) {
        String errorMessage = "Configuration error preventing case resource generation: " + configIssue;
        recordFailedGeneration(syncFhirCase, syncService,
                            SyncErrorType.CONFIGURATION_ERROR, errorMessage, null);
    }

    /**
     * Gets comprehensive generation status report for a case
     */
    public String getCaseGenerationReport(SyncFhirCase syncFhirCase) {
        StringBuilder report = new StringBuilder();

        report.append("=== CASE GENERATION STATUS REPORT ===\n");
        report.append(syncFhirCase.getGenerationStatusSummary());
        report.append("\n=== END REPORT ===\n");

        return report.toString();
    }

    /**
     * Creates a detailed error message including exception information
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
     * Checks if a case should be skipped due to excessive consecutive failures
     */
    public boolean shouldSkipCaseDueToFailures(SyncFhirCase syncFhirCase, int maxAllowedFailures) {
        if (syncFhirCase.hasExceededMaxGenerationFailures(maxAllowedFailures)) {
            log.warn("Skipping case " + syncFhirCase.getCaseIdentifier() +
                    " due to excessive consecutive failures (" + syncFhirCase.getConsecutiveGenerationFailures() + ")" +
                    " | Patient ID: " + (syncFhirCase.getPatient() != null ? syncFhirCase.getPatient().getId() : "Unknown"));
            return true;
        }
        return false;
    }

    /**
     * Provides summary statistics for multiple cases
     */
    public String getCasesGenerationSummary(java.util.List<SyncFhirCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return "No cases to analyze";
        }

        int totalCases = cases.size();
        int successCount = 0;
        int failureCount = 0;
        int pendingCount = 0;
        int highFailureCases = 0;

        for (SyncFhirCase caseItem : cases) {
            String status = caseItem.getResourceGenerationStatus();
            if ("SUCCESS".equals(status)) {
                successCount++;
            } else if ("FAILED".equals(status)) {
                failureCount++;
                if (caseItem.hasExceededMaxGenerationFailures(DEFAULT_MAX_CONSECUTIVE_GENERATION_FAILURES)) {
                    highFailureCases++;
                }
            } else {
                pendingCount++;
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("=== CASE GENERATION SUMMARY ===\n");
        summary.append("Total Cases: ").append(totalCases).append("\n");
        summary.append("Successful: ").append(successCount).append(" (").append(successCount * 100 / totalCases).append("%)\n");
        summary.append("Failed: ").append(failureCount).append(" (").append(failureCount * 100 / totalCases).append("%)\n");
        summary.append("Pending/Unknown: ").append(pendingCount).append(" (").append(pendingCount * 100 / totalCases).append("%)\n");
        summary.append("High Failure Cases (>3 consecutive): ").append(highFailureCases).append("\n");
        summary.append("=== END SUMMARY ===\n");

        return summary.toString();
    }
}