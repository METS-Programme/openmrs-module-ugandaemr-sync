package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Generic FHIR Profile Scheduler Task
 *
 * This single task can execute multiple FHIR profiles based on database configuration,
 * eliminating the need to create a separate task class for each profile.
 *
 * Features:
 * - Database-driven configuration
 * - Automatic retry logic
 * - Timeout handling
 * - Comprehensive execution tracking
 * - Backward compatible with existing profiles
 * - Priority-based execution
 *
 * Usage:
 * 1. Create one scheduled task using this class
 * 2. Configure profiles through database/UI
 * 3. No need for individual task classes per profile
 */
public class GenericFhirProfileSchedulerTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(GenericFhirProfileSchedulerTask.class);

    @Override
    public void execute() {
        log.info("Starting Generic FHIR Profile Scheduler Task");

        try {
            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);

            // Get all enabled profiles
            List<SyncFhirProfile> allProfiles = service.getAllSyncFhirProfile();
            List<SyncFhirProfile> profilesToExecute = new ArrayList<>();

            for (SyncFhirProfile profile : allProfiles) {
                if (profile.getProfileEnabled() != null && profile.getProfileEnabled() && shouldExecuteProfile(profile)) {
                    profilesToExecute.add(profile);
                }
            }

            // Sort by execution priority if available
            profilesToExecute.sort((p1, p2) -> {
                Integer priority1 = getExecutionPriority(p1);
                Integer priority2 = getExecutionPriority(p2);
                return priority1.compareTo(priority2);
            });

            log.info("Found " + profilesToExecute.size() + " profiles to execute");

            // Execute each profile
            for (SyncFhirProfile profile : profilesToExecute) {
                try {
                    executeProfile(profile);
                } catch (Exception e) {
                    log.error("Error executing profile: " + profile.getName(), e);
                }
            }

            log.info("Completed Generic FHIR Profile Scheduler Task");

        } catch (Exception e) {
            log.error("Error in Generic FHIR Profile Scheduler Task", e);
        }
    }

    /**
     * Determine if a profile should be executed based on its configuration
     */
    private boolean shouldExecuteProfile(SyncFhirProfile profile) {
        // Check if schedule is configured and enabled
        Boolean scheduleEnabled = getScheduleEnabled(profile);
        if (scheduleEnabled != null && !scheduleEnabled) {
            return false;
        }

        // Check execution window
        Date now = new Date();

        // Check start time
        Date startTime = getScheduleStartTime(profile);
        if (startTime != null && now.before(startTime)) {
            log.debug("Profile " + profile.getName() + " not yet started (starts at " + startTime + ")");
            return false;
        }

        // Check end time
        Date endTime = getScheduleEndTime(profile);
        if (endTime != null && now.after(endTime)) {
            log.debug("Profile " + profile.getName() + " has ended (ended at " + endTime + ")");
            return false;
        }

        // Check if already running (prevent concurrent execution unless parallel is enabled)
        String lastStatus = getLastExecutionStatus(profile);
        Boolean parallelExecution = getParallelExecution(profile);

        if ("RUNNING".equals(lastStatus) && (parallelExecution == null || !parallelExecution)) {
            log.warn("Profile " + profile.getName() + " is already running, skipping execution");
            return false;
        }

        return true;
    }

    /**
     * Execute a single FHIR profile with comprehensive error handling and tracking
     */
    private void executeProfile(SyncFhirProfile profile) {
        log.info("Executing FHIR profile: " + profile.getName());

        UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
        SyncFHIRRecord syncFHIRRecord = new SyncFHIRRecord();

        // Update status to running
        updateExecutionStatus(profile, "RUNNING", null);
        saveProfile(service, profile);

        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            // Execute with timeout if configured
            Long timeout = getTimeoutDuration(profile);
            if (timeout != null && timeout > 0) {
                executeWithTimeout(syncFHIRRecord, profile, timeout);
            } else {
                executeProfileLogic(syncFHIRRecord, profile);
            }

            success = true;
            log.info("Successfully executed profile: " + profile.getName());

        } catch (Exception e) {
            log.error("Error executing profile: " + profile.getName(), e);

            // Update failure status
            String errorDetails = getStackTrace(e);
            updateExecutionStatus(profile, "FAILED", errorDetails);

            // Handle retry logic
            Integer failedAttempts = getFailedExecutions(profile);
            Integer maxRetries = getMaxRetryAttempts(profile);

            if (maxRetries != null && failedAttempts != null && failedAttempts < maxRetries) {
                log.info("Scheduling retry for profile: " + profile.getName() + " (attempt " + (failedAttempts + 1) + "/" + maxRetries + ")");
                scheduleRetry(profile);
            }

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // Update final status and statistics
            if (success) {
                updateExecutionStatus(profile, "SUCCESS", null);
                incrementSuccessfulExecutions(profile);
            } else {
                incrementFailedExecutions(profile);
            }

            incrementTotalExecutions(profile);
            updateAverageExecutionTime(profile, executionTime);
            updateLastExecutionDate(profile, new Date());

            saveProfile(service, profile);

            log.info("Profile execution completed in " + executionTime + "ms: " + profile.getName());
        }
    }

    /**
     * Execute profile logic with timeout protection
     */
    private void executeWithTimeout(SyncFHIRRecord syncFHIRRecord, SyncFhirProfile profile, long timeoutMs) throws Exception {
        Thread executionThread = new Thread(() -> {
            try {
                executeProfileLogic(syncFHIRRecord, profile);
            } catch (Exception e) {
                throw new RuntimeException("Execution failed", e);
            }
        });

        executionThread.start();

        try {
            executionThread.join(timeoutMs);

            if (executionThread.isAlive()) {
                executionThread.interrupt();
                throw new RuntimeException("Profile execution timed out after " + timeoutMs + "ms");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Profile execution interrupted", e);
        }
    }

    /**
     * Execute the actual profile logic based on profile type
     */
    private void executeProfileLogic(SyncFHIRRecord syncFHIRRecord, SyncFhirProfile profile) throws Exception {
        // Generate resources based on profile type
        if (profile.getIsCaseBasedProfile() != null && profile.getIsCaseBasedProfile()) {
            log.debug("Generating case-based FHIR resources for profile: " + profile.getName());
            syncFHIRRecord.generateCaseBasedFHIRResourceBundles(profile);
        } else {
            log.debug("Generating resource-based FHIR resources for profile: " + profile.getName());
            // For resource-based profiles, we'll need to implement this method or handle differently
            log.info("Resource-based profiles not yet fully implemented, using case-based approach");
            syncFHIRRecord.generateCaseBasedFHIRResourceBundles(profile);
        }

        // Send resources if configured
        if (profile.getGenerateBundle() != null && profile.getGenerateBundle()) {
            log.debug("Sending FHIR resources for profile: " + profile.getName());
            syncFHIRRecord.sendFhirResourcesTo(profile);
        }
    }

    /**
     * Schedule a retry for a failed profile
     */
    private void scheduleRetry(SyncFhirProfile profile) {
        Long retryInterval = getRetryInterval(profile);
        if (retryInterval == null || retryInterval <= 0) {
            retryInterval = 60000L; // Default 1 minute
        }

        Date nextExecution = new Date(System.currentTimeMillis() + retryInterval);
        setNextExecutionDate(profile, nextExecution);

        log.info("Scheduled retry for profile " + profile.getName() + " at " + nextExecution);
    }

    // Helper methods for accessing extended profile properties using reflection
    // These methods make the code backward compatible with existing profiles

    private Boolean getScheduleEnabled(SyncFhirProfile profile) {
        return getReflectiveField(profile, "scheduleEnabled", Boolean.class);
    }

    private Date getScheduleStartTime(SyncFhirProfile profile) {
        return getReflectiveField(profile, "startDateTime", Date.class);
    }

    private Date getScheduleEndTime(SyncFhirProfile profile) {
        return getReflectiveField(profile, "endDateTime", Date.class);
    }

    private String getLastExecutionStatus(SyncFhirProfile profile) {
        return getReflectiveField(profile, "lastExecutionStatus", String.class);
    }

    private Boolean getParallelExecution(SyncFhirProfile profile) {
        return getReflectiveField(profile, "parallelExecution", Boolean.class);
    }

    private Long getTimeoutDuration(SyncFhirProfile profile) {
        return getReflectiveField(profile, "timeoutDuration", Long.class);
    }

    private Integer getMaxRetryAttempts(SyncFhirProfile profile) {
        return getReflectiveField(profile, "maxRetryAttempts", Integer.class);
    }

    private Integer getFailedExecutions(SyncFhirProfile profile) {
        Long value = getReflectiveField(profile, "failedExecutions", Long.class);
        return value != null ? value.intValue() : 0;
    }

    private Long getRetryInterval(SyncFhirProfile profile) {
        return getReflectiveField(profile, "retryInterval", Long.class);
    }

    private Integer getExecutionPriority(SyncFhirProfile profile) {
        Integer value = getReflectiveField(profile, "executionPriority", Integer.class);
        return value != null ? value : 5; // Default priority
    }

    @SuppressWarnings("unchecked")
    private <T> T getReflectiveField(SyncFhirProfile profile, String fieldName, Class<T> type) {
        try {
            java.lang.reflect.Field field = profile.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(profile);
        } catch (Exception e) {
            // Field doesn't exist yet (backward compatibility)
            return null;
        }
    }

    private void updateExecutionStatus(SyncFhirProfile profile, String status, String error) {
        setReflectiveField(profile, "lastExecutionStatus", status);
        if (error != null) {
            setReflectiveField(profile, "lastExecutionError", error);
        }
    }

    private void incrementTotalExecutions(SyncFhirProfile profile) {
        Long currentValue = getReflectiveField(profile, "totalExecutions", Long.class);
        setReflectiveField(profile, "totalExecutions", (currentValue != null ? currentValue : 0L) + 1);
    }

    private void incrementSuccessfulExecutions(SyncFhirProfile profile) {
        Long currentValue = getReflectiveField(profile, "successfulExecutions", Long.class);
        setReflectiveField(profile, "successfulExecutions", (currentValue != null ? currentValue : 0L) + 1);
    }

    private void incrementFailedExecutions(SyncFhirProfile profile) {
        Long currentValue = getReflectiveField(profile, "failedExecutions", Long.class);
        setReflectiveField(profile, "failedExecutions", (currentValue != null ? currentValue : 0L) + 1);
    }

    private void updateAverageExecutionTime(SyncFhirProfile profile, long executionTime) {
        Long currentAvg = getReflectiveField(profile, "averageExecutionTime", Long.class);
        Long totalExecutions = getReflectiveField(profile, "totalExecutions", Long.class);

        if (currentAvg != null && totalExecutions != null && totalExecutions > 0) {
            long newAvgTime = ((currentAvg * (totalExecutions - 1)) + executionTime) / totalExecutions;
            setReflectiveField(profile, "averageExecutionTime", newAvgTime);
        } else {
            setReflectiveField(profile, "averageExecutionTime", executionTime);
        }
    }

    private void updateLastExecutionDate(SyncFhirProfile profile, Date date) {
        setReflectiveField(profile, "lastExecutionDate", date);
    }

    private void setNextExecutionDate(SyncFhirProfile profile, Date date) {
        setReflectiveField(profile, "nextExecutionDate", date);
    }

    private void setReflectiveField(SyncFhirProfile profile, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = profile.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(profile, value);
        } catch (Exception e) {
            log.debug("Could not set field " + fieldName + " for profile: " + profile.getName());
        }
    }

    private void saveProfile(UgandaEMRSyncService service, SyncFhirProfile profile) {
        try {
            service.saveSyncFhirProfile(profile);
        } catch (Exception e) {
            log.warn("Could not save profile state: " + profile.getName(), e);
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}