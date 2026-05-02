package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.Date;
import java.util.List;

/**
 * Custom FHIR Profile Task Example
 *
 * This is an example of a custom task that can be assigned to a specific FHIR profile
 * by setting the custom_task_class field in the sync_fhir_profile table.
 *
 * <p><b>When to use this pattern:</b></p>
 * <ul>
 *   <li>Profiles requiring specialized execution logic</li>
 *   <li>Profiles with unique scheduling requirements</li>
 *   <li>Profiles needing custom error handling or retry logic</li>
 *   <li>Profiles requiring complex data transformation</li>
 * </ul>
 *
 * <p><b>How to assign this task to a profile:</b></p>
 * <pre>{@code
 * UPDATE sync_fhir_profile SET
 *   custom_task_class = 'org.openmrs.module.ugandaemrsync.tasks.CustomFhirProfileTask'
 * WHERE name = 'Your Complex Profile';
 * }</pre>
 *
 * <p><b>Note:</b> When a profile has a custom_task_class assigned, it will NOT be
 * executed by the GenericFhirProfileSchedulerTask. Only this custom task will run it.</p>
 */
public class CustomFhirProfileTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(CustomFhirProfileTask.class);

    private String profileName;

    @Override
    public void execute() {
        log.info("Starting Custom FHIR Profile Task");

        try {
            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);

            // Get the profile this task is responsible for
            // You can either pass the profile name via task properties
            // or hardcode it if this task is dedicated to one profile
            SyncFhirProfile profile = getTargetProfile(service);

            if (profile == null) {
                log.warn("No target profile found for CustomFhirProfileTask");
                return;
            }

            if (!profile.getProfileEnabled()) {
                log.info("Profile " + profile.getName() + " is not enabled, skipping execution");
                return;
            }

            log.info("Executing custom logic for profile: " + profile.getName());

            // Execute the profile with custom logic
            executeProfileWithCustomLogic(profile);

            // Update last execution time
            profile.setLastExecutionDate(new Date());
            profile.setLastExecutionStatus("COMPLETED");
            service.saveSyncFhirProfile(profile);

        } catch (Exception e) {
            log.error("Error in Custom FHIR Profile Task", e);
            handleExecutionError(e);
        }
    }

    /**
     * Get the target profile for this custom task.
     * Override this method to implement your own profile lookup logic.
     */
    protected SyncFhirProfile getTargetProfile(UgandaEMRSyncService service) {
        // Option 1: Use a hardcoded profile name (simplest approach)
        List<SyncFhirProfile> profiles = service.getSyncFhirProfileByName("Your Complex Profile Name");
        if (profiles != null && !profiles.isEmpty()) {
            return profiles.get(0);
        }
        return null;

        // Option 2: Pass profile name as task property (more flexible)
        // List<SyncFhirProfile> profiles = service.getSyncFhirProfileByName(getTaskName());
        // if (profiles != null && !profiles.isEmpty()) {
        //     return profiles.get(0);
        // }
        // return null;

        // Option 3: Query for profiles that have this task assigned
        // return service.getSyncFhirProfileByCustomTaskClass(this.getClass().getName());
    }

    /**
     * Execute the profile with custom logic.
     * This is where you implement your specialized execution requirements.
     */
    protected void executeProfileWithCustomLogic(SyncFhirProfile profile) throws Exception {
        log.info("Executing custom logic for profile: " + profile.getName());

        // Example: Pre-execution validation
        validateProfileConfiguration(profile);

        // Example: Custom data preparation
        prepareData(profile);

        // Example: Custom execution logic
        executeProfile(profile);

        // Example: Post-execution processing
        processResults(profile);

        log.info("Custom execution completed successfully for profile: " + profile.getName());
    }

    /**
     * Example: Validate profile configuration before execution
     */
    protected void validateProfileConfiguration(SyncFhirProfile profile) throws Exception {
        log.debug("Validating configuration for profile: " + profile.getName());

        // Custom validation logic
        if (profile.getUrl() == null || profile.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Profile URL is required");
        }

        // Add your custom validation rules here
        // - Check required fields
        // - Validate data formats
        // - Verify external system connectivity
        // etc.
    }

    /**
     * Example: Prepare data before execution
     */
    protected void prepareData(SyncFhirProfile profile) throws Exception {
        log.debug("Preparing data for profile: " + profile.getName());

        // Custom data preparation logic
        // - Query data from database
        // - Apply transformations
        // - Filter records based on criteria
        // - Bundle resources
        // etc.
    }

    /**
     * Example: Execute the profile with custom logic
     */
    protected void executeProfile(SyncFhirProfile profile) throws Exception {
        log.debug("Executing profile: " + profile.getName());

        // Custom execution logic
        // - HTTP requests with custom headers
        // - Multi-step data submission
        // - Interaction with multiple external systems
        // - Complex error handling and retry logic
        // - Transaction management
        // etc.

        // Example: Send data to external system
        // sendToFhirServer(profile, preparedData);

        log.info("Profile execution completed: " + profile.getName());
    }

    /**
     * Example: Process results after execution
     */
    protected void processResults(SyncFhirProfile profile) throws Exception {
        log.debug("Processing results for profile: " + profile.getName());

        // Custom result processing logic
        // - Parse response from external system
        // - Update database with response data
        // - Trigger follow-up actions
        // - Send notifications
        // etc.
    }

    /**
     * Handle execution errors with custom logic
     */
    protected void handleExecutionError(Exception e) {
        log.error("Custom error handling for execution failure", e);

        // Custom error handling logic
        // - Detailed error logging
        // - Send alerts to administrators
        // - Update profile error status
        // - Trigger recovery procedures
        // - Apply retry logic with exponential backoff
        // etc.

        // Example: Update profile with error information
        try {
            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
            SyncFhirProfile profile = getTargetProfile(service);
            if (profile != null) {
                profile.setLastExecutionStatus("FAILED");
                profile.setLastExecutionError(e.getMessage());
                service.saveSyncFhirProfile(profile);
            }
        } catch (Exception ex) {
            log.error("Failed to update profile error status", ex);
        }
    }

    // Getters and Setters

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
}