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
                Integer priority1 = p1.getExecutionPriority() != null ? p1.getExecutionPriority() : 5;
                Integer priority2 = p2.getExecutionPriority() != null ? p2.getExecutionPriority() : 5;
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
        if (profile.getScheduleEnabled() != null && !profile.getScheduleEnabled()) {
            return false;
        }

        // Check if profile itself is enabled
        if (profile.getProfileEnabled() == null || !profile.getProfileEnabled()) {
            return false;
        }

        // Check execution window
        Date now = new Date();

        // Check start time
        if (profile.getStartDateTime() != null && now.before(profile.getStartDateTime())) {
            log.debug("Profile " + profile.getName() + " not yet started (starts at " + profile.getStartDateTime() + ")");
            return false;
        }

        // Check end time
        if (profile.getEndDateTime() != null && now.after(profile.getEndDateTime())) {
            log.debug("Profile " + profile.getName() + " has ended (ended at " + profile.getEndDateTime() + ")");
            return false;
        }

        // Check if already running (prevent concurrent execution unless parallel is enabled)
        if ("RUNNING".equals(profile.getLastExecutionStatus()) &&
            (profile.getParallelExecution() == null || !profile.getParallelExecution())) {
            log.debug("Profile " + profile.getName() + " is already running, skipping execution");
            return false;
        }

        // CRITICAL: Check if it's actually TIME to run this profile based on its schedule
        if (!isDueForExecution(profile, now)) {
            log.debug("Profile " + profile.getName() + " is not due for execution yet");
            return false;
        }

        return true;
    }

    /**
     * Check if a profile is due for execution based on its schedule type and last execution
     * This prevents long-running profiles from blocking other profiles from running
     */
    private boolean isDueForExecution(SyncFhirProfile profile, Date now) {
        String scheduleType = profile.getScheduleType();

        // If no schedule type is set, don't run automatically (manual only)
        if (scheduleType == null || scheduleType.isEmpty()) {
            return false;
        }

        // MANUAL schedules should never run automatically
        if ("MANUAL".equals(scheduleType)) {
            return false;
        }

        // Check if there's a next execution date set and use that
        if (profile.getNextExecutionDate() != null) {
            if (now.before(profile.getNextExecutionDate())) {
                // Not time yet
                return false;
            }
            // Time to run! The scheduler will set a new next execution date after this run
            return true;
        }

        // For other schedule types, check if enough time has passed since last execution
        Date lastExecution = profile.getLastExecutionDate();

        if (lastExecution == null) {
            // Never run before, it's due
            return true;
        }

        long timeSinceLastExecution = now.getTime() - lastExecution.getTime();

        // Check based on schedule type
        switch (scheduleType) {
            case "CRON":
                // For CRON, we'd need a cron parser to determine exact next run time
                // For simplicity, we'll use a minimum interval of 1 hour
                // In production, you'd parse the cron expression and check if now matches
                long minCronInterval = 60 * 60 * 1000L; // 1 hour minimum
                return timeSinceLastExecution >= minCronInterval;

            case "FIXED_RATE":
                // Check if the fixed rate interval has passed
                Long fixedRateInterval = profile.getFixedRateInterval();
                if (fixedRateInterval != null && fixedRateInterval > 0) {
                    return timeSinceLastExecution >= fixedRateInterval;
                }
                return false;

            case "FIXED_DELAY":
                // Check if the fixed delay interval has passed
                Long fixedDelayInterval = profile.getFixedDelayInterval();
                if (fixedDelayInterval != null && fixedDelayInterval > 0) {
                    return timeSinceLastExecution >= fixedDelayInterval;
                }
                return false;

            default:
                log.warn("Unknown schedule type for profile " + profile.getName() + ": " + scheduleType);
                return false;
        }
    }

    /**
     * Execute a single FHIR profile with comprehensive error handling and tracking
     */
    private void executeProfile(SyncFhirProfile profile) {
        log.info("Executing FHIR profile: " + profile.getName());

        UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
        SyncFHIRRecord syncFHIRRecord = new SyncFHIRRecord();

        // Update status to running
        profile.setLastExecutionStatus("RUNNING");
        profile.setLastExecutionError(null);
        saveProfile(service, profile);

        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            // Execute with timeout if configured
            if (profile.getTimeoutDuration() != null && profile.getTimeoutDuration() > 0) {
                executeWithTimeout(syncFHIRRecord, profile, profile.getTimeoutDuration());
            } else {
                executeProfileLogic(syncFHIRRecord, profile);
            }

            success = true;
            log.info("Successfully executed profile: " + profile.getName());

        } catch (Exception e) {
            log.error("Error executing profile: " + profile.getName(), e);

            // Update failure status
            String errorDetails = getStackTrace(e);
            profile.setLastExecutionStatus("FAILED");
            profile.setLastExecutionError(errorDetails);

            // Handle retry logic
            long failedAttempts = profile.getFailedExecutions() != null ? profile.getFailedExecutions() : 0;
            Integer maxRetries = profile.getMaxRetryAttempts();

            if (maxRetries != null && failedAttempts < maxRetries) {
                log.info("Scheduling retry for profile: " + profile.getName() + " (attempt " + (failedAttempts + 1) + "/" + maxRetries + ")");
                scheduleRetry(profile);
            }

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // Update final status and statistics
            if (success) {
                profile.setLastExecutionStatus("SUCCESS");
                profile.setSuccessfulExecutions(
                    (profile.getSuccessfulExecutions() != null ? profile.getSuccessfulExecutions() : 0) + 1
                );
                // Calculate next execution date for successful runs
                calculateNextExecutionDate(profile);
            } else {
                profile.setFailedExecutions(
                    (profile.getFailedExecutions() != null ? profile.getFailedExecutions() : 0) + 1
                );
                // Don't set next execution date for failures - retry logic handles that
            }

            profile.setTotalExecutions(
                (profile.getTotalExecutions() != null ? profile.getTotalExecutions() : 0) + 1
            );

            // Update average execution time
            long currentAvg = profile.getAverageExecutionTime() != null ? profile.getAverageExecutionTime() : 0;
            long totalExecs = profile.getTotalExecutions() != null ? profile.getTotalExecutions() : 1;
            long newAvgTime = ((currentAvg * (totalExecs - 1)) + executionTime) / totalExecs;
            profile.setAverageExecutionTime(newAvgTime);

            profile.setLastExecutionDate(new Date());

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
        Long retryInterval = profile.getRetryInterval();
        if (retryInterval == null || retryInterval <= 0) {
            retryInterval = 60000L; // Default 1 minute
        }

        Date nextExecution = new Date(System.currentTimeMillis() + retryInterval);
        profile.setNextExecutionDate(nextExecution);

        log.info("Scheduled retry for profile " + profile.getName() + " at " + nextExecution);
    }

    /**
     * Calculate and set the next execution date based on the profile's schedule type
     * This prevents profiles from running continuously and blocks other profiles
     */
    private void calculateNextExecutionDate(SyncFhirProfile profile) {
        String scheduleType = profile.getScheduleType();
        Date now = new Date();

        if (scheduleType == null || "MANUAL".equals(scheduleType)) {
            // No automatic next execution for manual schedules
            profile.setNextExecutionDate(null);
            return;
        }

        long nextExecutionDelay = 0;

        switch (scheduleType) {
            case "FIXED_RATE":
                // Execute every X milliseconds from the last execution
                Long fixedRateInterval = profile.getFixedRateInterval();
                if (fixedRateInterval != null && fixedRateInterval > 0) {
                    nextExecutionDelay = fixedRateInterval;
                } else {
                    // Default to 1 hour if not specified
                    nextExecutionDelay = 60 * 60 * 1000L;
                }
                break;

            case "FIXED_DELAY":
                // Execute X milliseconds after the PREVIOUS execution completes
                Long fixedDelayInterval = profile.getFixedDelayInterval();
                if (fixedDelayInterval != null && fixedDelayInterval > 0) {
                    nextExecutionDelay = fixedDelayInterval;
                } else {
                    // Default to 1 hour if not specified
                    nextExecutionDelay = 60 * 60 * 1000L;
                }
                break;

            case "CRON":
                // For CRON, we'd ideally parse the cron expression
                // For now, use a default of 1 hour (can be enhanced with a cron parser library)
                // In production, use a library like CronUtils or Quartz CronExpression
                String cronExpression = profile.getCronExpression();
                if (cronExpression != null && !cronExpression.isEmpty()) {
                    // Simple heuristic: if it's "0 0 * * * *" it's hourly, etc.
                    // For now, default to 1 hour
                    nextExecutionDelay = 60 * 60 * 1000L; // 1 hour default
                } else {
                    nextExecutionDelay = 60 * 60 * 1000L; // 1 hour default
                }
                break;

            default:
                log.warn("Unknown schedule type for profile " + profile.getName() + ": " + scheduleType);
                // Don't set next execution date
                return;
        }

        if (nextExecutionDelay > 0) {
            Date nextExecution = new Date(now.getTime() + nextExecutionDelay);
            profile.setNextExecutionDate(nextExecution);
            log.debug("Next execution for profile " + profile.getName() + " scheduled at " + nextExecution);
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