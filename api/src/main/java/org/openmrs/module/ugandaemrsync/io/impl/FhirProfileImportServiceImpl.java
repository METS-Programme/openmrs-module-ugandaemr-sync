package org.openmrs.module.ugandaemrsync.io.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.io.ConflictResolution;
import org.openmrs.module.ugandaemrsync.io.FhirProfileImportService;
import org.openmrs.module.ugandaemrsync.io.ImportPreview;
import org.openmrs.module.ugandaemrsync.io.ImportResult;
import org.openmrs.module.ugandaemrsync.io.ProfileSummary;
import org.openmrs.module.ugandaemrsync.io.ValidationResult;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Implementation of FHIR profile import service
 */
public class FhirProfileImportServiceImpl implements FhirProfileImportService {

    private static final Log log = LogFactory.getLog(FhirProfileImportServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SyncFhirProfile importProfileFromJson(String jsonConfig) {
        try {
            JsonNode root = objectMapper.readTree(jsonConfig);

            // Handle wrapped export format
            JsonNode profileNode = root.has("export") ?
                    root.path("export").path("profiles").get(0) : root;

            SyncFhirProfile profile = convertJsonToProfile(profileNode);

            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
            SyncFhirProfile savedProfile = service.saveSyncFhirProfile(profile);

            // Log successful import with task class information
            if (savedProfile.getCustomTaskClass() != null && !savedProfile.getCustomTaskClass().isEmpty()) {
                log.info("Successfully imported profile '" + savedProfile.getName() + "' with custom task class: " + savedProfile.getCustomTaskClass());
            } else {
                log.info("Successfully imported profile '" + savedProfile.getName() + "' (will use generic scheduler)");
            }

            return savedProfile;

        } catch (Exception e) {
            log.error("Error importing profile from JSON", e);
            throw new RuntimeException("Failed to import profile: " + e.getMessage(), e);
        }
    }

    @Override
    public SyncFhirProfile importProfileFromYaml(String yamlConfig) {
        log.warn("YAML import not yet implemented, attempting as JSON");
        return importProfileFromJson(yamlConfig);
    }

    @Override
    public SyncFhirProfile importProfileFromFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            log.debug("Importing profile from file: " + file.getName());
            return importProfileFromJson(content);
        } catch (IOException e) {
            log.error("Error reading file: " + file.getAbsolutePath(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SyncFhirProfile> importProfilesFromJson(String jsonConfig) {
        List<SyncFhirProfile> profiles = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonConfig);
            JsonNode profilesArray = root.has("export") ?
                    root.path("export").path("profiles") : root.path("profiles");

            if (profilesArray.isArray()) {
                for (JsonNode profileNode : profilesArray) {
                    SyncFhirProfile profile = convertJsonToProfile(profileNode);
                    UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
                    profiles.add(service.saveSyncFhirProfile(profile));
                }
            }

            // Log import summary
            log.info(generateImportSummary(profiles));

        } catch (Exception e) {
            log.error("Error importing profiles from JSON", e);
            throw new RuntimeException("Failed to import profiles: " + e.getMessage(), e);
        }
        return profiles;
    }

    @Override
    public List<SyncFhirProfile> importProfilesFromYaml(String yamlConfig) {
        log.warn("YAML import not yet implemented, attempting as JSON");
        return importProfilesFromJson(yamlConfig);
    }

    @Override
    public ImportResult importProfileWithValidation(String jsonConfig) {
        ImportResult result = new ImportResult();
        ValidationResult validation = validateProfile(jsonConfig);
        result.setValidation(validation);

        if (validation.isValid()) {
            try {
                SyncFhirProfile profile = importProfileFromJson(jsonConfig);
                List<SyncFhirProfile> imported = new ArrayList<>();
                imported.add(profile);
                result.setImportedProfiles(imported);
                result.setSuccess(true);
            } catch (Exception e) {
                result.setSuccess(false);
                List<String> errors = new ArrayList<>();
                errors.add(e.getMessage());
                result.setErrors(errors);
            }
        } else {
            result.setSuccess(false);
            result.setErrors(validation.getErrors());
        }

        return result;
    }

    @Override
    public SyncFhirProfile importProfileWithConflictResolution(String jsonConfig, ConflictResolution strategy) {
        // Default import for now - conflict resolution can be enhanced later
        return importProfileFromJson(jsonConfig);
    }

    @Override
    public SyncFhirProfile importEncryptedProfile(String encryptedJson, String decryptionKey) {
        throw new UnsupportedOperationException("Encrypted import not yet implemented");
    }

    @Override
    public ImportPreview previewImport(String jsonConfig) {
        ImportPreview preview = new ImportPreview();
        List<ProfileSummary> summaries = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonConfig);
            JsonNode profilesArray = root.has("export") ?
                    root.path("export").path("profiles") : root.path("profiles");

            if (profilesArray.isArray()) {
                for (JsonNode profileNode : profilesArray) {
                    JsonNode metadata = profileNode.path("metadata");
                    ProfileSummary summary = new ProfileSummary();
                    summary.setName(metadata.path("name").asText());
                    summary.setCategory(metadata.path("category").asText());
                    summary.setDescription(metadata.path("description").asText());
                    summaries.add(summary);
                }
            }
        } catch (Exception e) {
            log.error("Error previewing import", e);
        }

        preview.setProfilesToImport(summaries);
        return preview;
    }

    @Override
    public ImportResult importAllConfiguration(String exportJson) {
        ImportResult result = new ImportResult();
        List<SyncFhirProfile> importedProfiles = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(exportJson);
            JsonNode exportNode = root.path("export");

            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);

            // Import profiles
            JsonNode profilesArray = exportNode.path("profiles");
            if (profilesArray.isArray()) {
                for (JsonNode profileNode : profilesArray) {
                    try {
                        SyncFhirProfile profile = convertJsonToProfile(profileNode);
                        importedProfiles.add(service.saveSyncFhirProfile(profile));
                    } catch (Exception e) {
                        warnings.add("Failed to import profile: " + e.getMessage());
                        log.warn("Failed to import profile", e);
                    }
                }
            }

            // Import task types
            JsonNode taskTypesArray = exportNode.path("taskTypes");
            if (taskTypesArray.isArray()) {
                for (JsonNode taskTypeNode : taskTypesArray) {
                    try {
                        SyncTaskType taskType = convertJsonToTaskType(taskTypeNode);
                        service.saveSyncTaskType(taskType);
                    } catch (Exception e) {
                        warnings.add("Failed to import task type: " + e.getMessage());
                        log.warn("Failed to import task type", e);
                    }
                }
            }

            result.setImportedProfiles(importedProfiles);
            result.setWarnings(warnings);
            result.setSuccess(true);

            // Log import summary
            if (!importedProfiles.isEmpty()) {
                log.info(generateImportSummary(importedProfiles));
            }

        } catch (Exception e) {
            log.error("Error importing all configuration", e);
            result.setSuccess(false);
            List<String> errors = new ArrayList<>();
            errors.add(e.getMessage());
            result.setErrors(errors);
        }

        return result;
    }

    @Override
    public ValidationResult validateProfile(String jsonConfig) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonConfig);
            JsonNode metadata = root.has("export") ?
                    root.path("export").path("profiles").get(0).path("metadata") :
                    root.path("metadata");
            JsonNode config = root.has("export") ?
                    root.path("export").path("profiles").get(0).path("configuration") :
                    root.path("configuration");

            // Validate required fields
            if (!metadata.has("name") || metadata.path("name").asText().isEmpty()) {
                errors.add("Profile name is required");
            }
            if (!root.has("configuration")) {
                errors.add("Configuration section is required");
            }

            // Validate custom task class if specified
            if (config.has("customTaskClass")) {
                String customTaskClass = config.path("customTaskClass").asText();
                if (customTaskClass != null && !customTaskClass.isEmpty()) {
                    // Check if it's a valid Java class name format
                    if (!customTaskClass.matches("^[a-zA-Z_$][a-zA-Z\\d_$]*(\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*$")) {
                        errors.add("Invalid custom task class format: " + customTaskClass);
                    } else {
                        // Log the custom task class assignment
                        log.debug("Profile '" + metadata.path("name").asText() + "' will use custom task class: " + customTaskClass);
                    }
                }
            }

            result.setValid(errors.isEmpty());
            result.setErrors(errors);
            result.setWarnings(warnings);

        } catch (Exception e) {
            result.setValid(false);
            errors.add("Invalid JSON: " + e.getMessage());
            result.setErrors(errors);
        }

        return result;
    }

    /**
     * Import profiles from configuration directory on startup
     *
     * @param configPath Base configuration directory path (e.g., /path/to/configuration)
     *                   Expected subdirectories: syncprofile/ and synctasktype/
     *                   If null, reads from global property ugandaemrsync.configuration.directory
     */
    public void importConfigurationsFromDirectory(String configPath) {
        // If no path provided, use global property default
        if (configPath == null) {
            configPath = getConfigDirectoryPath();
        }

        // Import from syncprofile and synctasktype subdirectories directly
        // (not under hie/ - the global property can point to the exact location)
        Path baseConfigDir = Paths.get(configPath);
        if (!Files.exists(baseConfigDir)) {
            log.info("Configuration directory not found: " + baseConfigDir);
            return;
        }

        log.info("Importing configurations from: " + baseConfigDir);

        // Import profiles
        Path profileDir = baseConfigDir.resolve("syncprofile");
        List<SyncFhirProfile> importedProfiles = new ArrayList<>();
        if (Files.exists(profileDir)) {
            try {
                Files.list(profileDir).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        SyncFhirProfile profile = importProfileFromFile(p.toFile());
                        importedProfiles.add(profile);
                        log.info("Imported profile: " + profile.getName());
                    } catch (Exception e) {
                        log.warn("Failed to import profile from " + p.getFileName(), e);
                    }
                });

                // Log import summary
                if (!importedProfiles.isEmpty()) {
                    log.info(generateImportSummary(importedProfiles));
                }

            } catch (IOException e) {
                log.error("Error listing profile files", e);
            }
        }

        // Import task types
        Path taskTypeDir = baseConfigDir.resolve("synctasktype");
        if (Files.exists(taskTypeDir)) {
            try {
                Files.list(taskTypeDir).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        SyncTaskType taskType = importTaskTypeFromFile(p.toFile());
                        log.info("Imported task type: " + taskType.getName());
                    } catch (Exception e) {
                        log.warn("Failed to import task type from " + p.getFileName(), e);
                    }
                });
            } catch (IOException e) {
                log.error("Error listing task type files", e);
            }
        }
    }

    /**
     * Get the configuration directory path from global property.
     * First checks if the global property is set to an absolute path.
     * If not, resolves relative to OpenMRS application data directory.
     * Falls back to default "configuration/hie" if property is not set.
     *
     * @return Resolved configuration directory path
     */
    private String getConfigDirectoryPath() {
        org.openmrs.api.AdministrationService adminService = Context.getService(org.openmrs.api.AdministrationService.class);
        String configDir = adminService.getGlobalProperty("ugandaemrsync.configuration.directory");

        if (configDir == null || configDir.trim().isEmpty()) {
            configDir = "configuration/hie"; // Default
        }

        // Check if it's an absolute path
        Path path = Paths.get(configDir);
        if (path.isAbsolute()) {
            return configDir;
        }

        // Relative path - resolve against OpenMRS application data directory
        String openmrsDataDir = org.openmrs.util.OpenmrsUtil.getApplicationDataDirectory();
        return Paths.get(openmrsDataDir, configDir).toString();
    }

    /**
     * Import configurations from classpath resources (bundled with module)
     *
     * @return true if configurations were found and imported, false otherwise
     */
    public boolean importConfigurationsFromClasspath() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            int importCount = 0;
            List<SyncFhirProfile> importedProfiles = new ArrayList<>();

            // Import profiles from classpath
            String profilePath = "configuration/hie/syncprofile/";
            Enumeration<URL> profileResources = classLoader.getResources(profilePath);

            if (profileResources.hasMoreElements()) {
                URL profileUrl = profileResources.nextElement();
                if ("file".equals(profileUrl.getProtocol())) {
                    File profileDir = new File(profileUrl.toURI());
                    if (profileDir.exists() && profileDir.isDirectory()) {
                        File[] profileFiles = profileDir.listFiles((dir, name) -> name.endsWith(".json"));
                        if (profileFiles != null && profileFiles.length > 0) {
                            log.info("Found " + profileFiles.length + " profile files in classpath");
                            for (File file : profileFiles) {
                                try {
                                    SyncFhirProfile profile = importProfileFromFile(file);
                                    importedProfiles.add(profile);
                                    log.info("Imported profile from classpath: " + profile.getName());
                                    importCount++;
                                } catch (Exception e) {
                                    log.warn("Failed to import profile from classpath: " + file.getName(), e);
                                }
                            }
                        }
                    }
                }
            }

            // Log import summary
            if (!importedProfiles.isEmpty()) {
                log.info(generateImportSummary(importedProfiles));
            }

            // Import task types from classpath
            String taskTypePath = "configuration/hie/synctasktype/";
            Enumeration<URL> taskTypeResources = classLoader.getResources(taskTypePath);

            if (taskTypeResources.hasMoreElements()) {
                URL taskTypeUrl = taskTypeResources.nextElement();
                if ("file".equals(taskTypeUrl.getProtocol())) {
                    File taskTypeDir = new File(taskTypeUrl.toURI());
                    if (taskTypeDir.exists() && taskTypeDir.isDirectory()) {
                        File[] taskTypeFiles = taskTypeDir.listFiles((dir, name) -> name.endsWith(".json"));
                        if (taskTypeFiles != null && taskTypeFiles.length > 0) {
                            log.info("Found " + taskTypeFiles.length + " task type files in classpath");
                            for (File file : taskTypeFiles) {
                                try {
                                    SyncTaskType taskType = importTaskTypeFromFile(file);
                                    log.info("Imported task type from classpath: " + taskType.getName());
                                    importCount++;
                                } catch (Exception e) {
                                    log.warn("Failed to import task type from classpath: " + file.getName(), e);
                                }
                            }
                        }
                    }
                }
            }

            return importCount > 0;

        } catch (Exception e) {
            log.warn("Failed to import configurations from classpath", e);
            return false;
        }
    }

    private SyncTaskType importTaskTypeFromFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JsonNode root = objectMapper.readTree(content);
            SyncTaskType taskType = convertJsonToTaskType(root);

            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
            return service.saveSyncTaskType(taskType);
        } catch (Exception e) {
            log.error("Error importing task type from file: " + file.getAbsolutePath(), e);
            throw new RuntimeException("Failed to import task type: " + e.getMessage(), e);
        }
    }

    private SyncFhirProfile convertJsonToProfile(JsonNode profileNode) {
        try {
            JsonNode metadata = profileNode.path("metadata");
            JsonNode config = profileNode.path("configuration");

            String uuid = metadata.path("id").asText(null);
            SyncFhirProfile profile;

            // Assume profile is new until proven otherwise
            profile = new SyncFhirProfile();
            boolean isNewProfile = true;

            // Try to find existing profile by UUID
            if (uuid != null && !uuid.isEmpty()) {
                try {
                    SyncFhirProfile existingProfile = Context.getService(UgandaEMRSyncService.class)
                            .getSyncFhirProfileByUUID(uuid);
                    if (existingProfile != null) {
                        // Found existing profile, use it instead of creating new
                        profile = existingProfile;
                        isNewProfile = false;
                        log.debug("Updating existing profile: " + profile.getName() + " (UUID: " + uuid + ")");
                    } else {
                        // No existing profile found, set UUID on new profile
                        profile.setUuid(uuid);
                        log.debug("Creating new profile with UUID: " + uuid);
                    }
                } catch (Exception e) {
                    log.warn("Error checking for existing profile by UUID: " + uuid + ", creating new profile", e);
                    profile.setUuid(uuid);
                }
            } else {
                log.debug("Creating new profile without UUID");
            }

        // Set endpoint fields with safe handling
        // For new profiles: set all fields from JSON
        // For existing profiles: only update if JSON value is not null/empty
        if (config.has("endpoint")) {
            JsonNode endpoint = config.path("endpoint");

            // Handle URL safely
            if (endpoint.has("url")) {
                String url = endpoint.path("url").asText(null);
                // Only set URL if it's not null and not empty, or if this is a new profile
                if (isNewProfile || (url != null && !url.isEmpty())) {
                    profile.setUrl(url);
                }
            }

            // Only set credentials for new profiles, never overwrite existing credentials
            if (isNewProfile) {
                if (endpoint.has("username")) {
                    String username = endpoint.path("username").asText(null);
                    if (username != null && !username.isEmpty()) {
                        profile.setUrlUserName(username);
                    }
                }
                if (endpoint.has("password")) {
                    String password = endpoint.path("password").asText(null);
                    if (password != null && !password.isEmpty()) {
                        profile.setUrlPassword(password);
                    }
                }
                if (endpoint.has("token")) {
                    String token = endpoint.path("token").asText(null);
                    if (token != null && !token.isEmpty()) {
                        profile.setUrlToken(token);
                    }
                }
            }
        }

        // Handle profile enabled status
        if (config.has("profileEnabled")) {
            profile.setProfileEnabled(config.path("profileEnabled").asBoolean(true));
        }

        // Handle patient identifier type safely
        if (config.has("patientIdentifierType")) {
            String patientIdentifierTypeStr = config.path("patientIdentifierType").asText(null);
            if (patientIdentifierTypeStr != null && !patientIdentifierTypeStr.isEmpty()) {
                try {
                    Integer patientIdentifierTypeId = Integer.parseInt(patientIdentifierTypeStr);
                    // Only try to set if it's a valid ID
                    if (patientIdentifierTypeId > 0) {
                        // Note: Setting the actual PatientIdentifierType object would require service lookup
                        // For now, we'll just log this and let the service layer handle it
                        log.debug("Profile '" + profile.getName() + "' configured with patient identifier type ID: " + patientIdentifierTypeId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid patient identifier type format for profile '" + profile.getName() + "': " + patientIdentifierTypeStr);
                }
            }
        }

        // Set profile metadata and configuration with safe handling

        // Profile name (always set from metadata)
        String name = metadata.path("name").asText();
        if (name != null && !name.isEmpty()) {
            profile.setName(name);
        }

        // Resource types (update if provided)
        if (config.has("resourceTypes")) {
            String resourceTypes = config.path("resourceTypes").asText(null);
            if (resourceTypes != null && !resourceTypes.isEmpty()) {
                profile.setResourceTypes(resourceTypes);
            }
        }

        // Case-based profile configuration (update if provided)
        if (config.has("isCaseBasedProfile")) {
            profile.setIsCaseBasedProfile(config.path("isCaseBasedProfile").asBoolean(false));
        }

        if (config.has("caseBasedPrimaryResourceType")) {
            String caseBasedPrimaryResourceType = config.path("caseBasedPrimaryResourceType").asText(null);
            if (caseBasedPrimaryResourceType != null && !caseBasedPrimaryResourceType.isEmpty()) {
                profile.setCaseBasedPrimaryResourceType(caseBasedPrimaryResourceType);
            }
        }

        if (config.has("caseBasedPrimaryResourceTypeId")) {
            String caseBasedPrimaryResourceTypeId = config.path("caseBasedPrimaryResourceTypeId").asText(null);
            if (caseBasedPrimaryResourceTypeId != null && !caseBasedPrimaryResourceTypeId.isEmpty()) {
                profile.setCaseBasedPrimaryResourceTypeId(caseBasedPrimaryResourceTypeId);
            }
        }

        // Resource search parameter (update if provided)
        if (config.has("resourceSearchParameter")) {
            JsonNode searchParamNode = config.path("resourceSearchParameter");
            if (!searchParamNode.isMissingNode() && !searchParamNode.isNull()) {
                // Use toString() to handle both String and Object/Array nodes
                // asText() returns empty string for Object/Array nodes, losing the data
                profile.setResourceSearchParameter(searchParamNode.toString());
            }
        }

        // Bundle generation configuration (update if provided)
        if (config.has("generateBundle")) {
            profile.setGenerateBundle(config.path("generateBundle").asBoolean(true));
        }

        if (config.has("numberOfResourcesInBundle")) {
            int numberOfResourcesInBundle = config.path("numberOfResourcesInBundle").asInt(50);
            if (numberOfResourcesInBundle > 0) {
                profile.setNumberOfResourcesInBundle(numberOfResourcesInBundle);
            }
        }

        if (config.has("keepProfileIdentifierOnly")) {
            profile.setKeepProfileIdentifierOnly(config.path("keepProfileIdentifierOnly").asBoolean(false));
        }

        if (config.has("durationToKeepSyncedResources")) {
            int durationToKeepSyncedResources = config.path("durationToKeepSyncedResources").asInt(30);
            if (durationToKeepSyncedResources >= 0) {
                profile.setDurationToKeepSyncedResources(durationToKeepSyncedResources);
            }
        }

        // Set custom task class if specified
        String customTaskClass = config.path("customTaskClass").asText(null);
        if (customTaskClass != null && !customTaskClass.isEmpty()) {
            profile.setCustomTaskClass(customTaskClass);
            log.debug("Set custom task class for profile '" + profile.getName() + "': " + customTaskClass);
        }

        // Set additional configuration fields with safe handling
        if (config.has("syncLimit")) {
            int syncLimit = config.path("syncLimit").asInt(50);
            if (syncLimit >= 0) {
                profile.setSyncLimit(syncLimit);
            }
        }

        if (config.has("searchable")) {
            profile.setSearchable(config.path("searchable").asBoolean(false));
        }

        if (config.has("searchURL")) {
            String searchURL = config.path("searchURL").asText(null);
            if (searchURL != null && !searchURL.isEmpty()) {
                profile.setSearchURL(searchURL);
            }
        }

        // Set scheduling configuration if present
        if (config.has("scheduleEnabled")) {
            profile.setScheduleEnabled(config.path("scheduleEnabled").asBoolean(false));
        }

        if (config.has("scheduleType")) {
            String scheduleType = config.path("scheduleType").asText(null);
            if (scheduleType != null && !scheduleType.isEmpty()) {
                profile.setScheduleType(scheduleType);
            }
        }

        if (config.has("cronExpression")) {
            String cronExpression = config.path("cronExpression").asText(null);
            if (cronExpression != null && !cronExpression.isEmpty()) {
                profile.setCronExpression(cronExpression);
            }
        }

        if (config.has("fixedRateInterval")) {
            JsonNode fixedRateNode = config.path("fixedRateInterval");
            if (!fixedRateNode.isMissingNode() && !fixedRateNode.isNull()) {
                try {
                    Long fixedRateInterval = fixedRateNode.asLong();
                    if (fixedRateInterval > 0) {
                        profile.setFixedRateInterval(fixedRateInterval);
                    }
                } catch (Exception e) {
                    log.warn("Invalid fixedRateInterval value for profile '" + profile.getName() + "'");
                }
            }
        }

        if (config.has("fixedDelayInterval")) {
            JsonNode fixedDelayNode = config.path("fixedDelayInterval");
            if (!fixedDelayNode.isMissingNode() && !fixedDelayNode.isNull()) {
                try {
                    Long fixedDelayInterval = fixedDelayNode.asLong();
                    if (fixedDelayInterval > 0) {
                        profile.setFixedDelayInterval(fixedDelayInterval);
                    }
                } catch (Exception e) {
                    log.warn("Invalid fixedDelayInterval value for profile '" + profile.getName() + "'");
                }
            }
        }

        if (config.has("taskName")) {
            String taskName = config.path("taskName").asText(null);
            if (taskName != null && !taskName.isEmpty()) {
                profile.setTaskName(taskName);
            }
        }

        if (config.has("taskDescription")) {
            String taskDescription = config.path("taskDescription").asText(null);
            if (taskDescription != null && !taskDescription.isEmpty()) {
                profile.setTaskDescription(taskDescription);
            }
        }

        if (config.has("taskGroup")) {
            String taskGroup = config.path("taskGroup").asText(null);
            if (taskGroup != null && !taskGroup.isEmpty()) {
                profile.setTaskGroup(taskGroup);
            }
        }

        if (config.has("executionPriority")) {
            Integer executionPriority = config.path("executionPriority").asInt(5);
            if (executionPriority >= 1 && executionPriority <= 10) {
                profile.setExecutionPriority(executionPriority);
            }
        }

        if (config.has("parallelExecution")) {
            profile.setParallelExecution(config.path("parallelExecution").asBoolean(false));
        }

        if (config.has("timeoutDuration")) {
            JsonNode timeoutNode = config.path("timeoutDuration");
            if (!timeoutNode.isMissingNode() && !timeoutNode.isNull()) {
                try {
                    Long timeoutDuration = timeoutNode.asLong();
                    if (timeoutDuration > 0) {
                        profile.setTimeoutDuration(timeoutDuration);
                    }
                } catch (Exception e) {
                    log.warn("Invalid timeoutDuration value for profile '" + profile.getName() + "'");
                }
            }
        }

        if (config.has("maxRetryAttempts")) {
            Integer maxRetryAttempts = config.path("maxRetryAttempts").asInt(3);
            if (maxRetryAttempts >= 0) {
                profile.setMaxRetryAttempts(maxRetryAttempts);
            }
        }

        if (config.has("retryInterval")) {
            JsonNode retryIntervalNode = config.path("retryInterval");
            if (!retryIntervalNode.isMissingNode() && !retryIntervalNode.isNull()) {
                try {
                    Long retryInterval = retryIntervalNode.asLong();
                    if (retryInterval > 0) {
                        profile.setRetryInterval(retryInterval);
                    }
                } catch (Exception e) {
                    log.warn("Invalid retryInterval value for profile '" + profile.getName() + "'");
                }
            }
        }

        // Log successful conversion
        log.debug("Successfully converted profile configuration: " + profile.getName());

        return profile;

        } catch (Exception e) {
            log.error("Error converting JSON to profile object", e);
            throw new RuntimeException("Failed to convert profile configuration: " + e.getMessage(), e);
        }
    }

    private SyncTaskType convertJsonToTaskType(JsonNode taskTypeNode) {
        JsonNode metadata = taskTypeNode.path("metadata");
        JsonNode config = taskTypeNode.path("configuration");

        String uuid = metadata.path("id").asText(null);
        SyncTaskType taskType;

        // Assume task type is new until proven otherwise
        taskType = new SyncTaskType();
        boolean isNewTaskType = true;

        // Try to find existing task type by UUID
        if (uuid != null && !uuid.isEmpty()) {
            SyncTaskType existingTaskType = Context.getService(UgandaEMRSyncService.class)
                    .getSyncTaskTypeByUUID(uuid);
            if (existingTaskType != null) {
                // Found existing task type, use it instead of creating new
                taskType = existingTaskType;
                isNewTaskType = false;
            } else {
                // No existing task type found, set UUID on new task type
                taskType.setUuid(uuid);
            }
        }

        // Set endpoint fields
        // For new task types: set all fields from JSON
        // For existing task types: only update if JSON value is not null/empty
        if (config.has("endpoint")) {
            JsonNode endpoint = config.path("endpoint");
            String url = endpoint.has("url") ? endpoint.path("url").asText(null) : null;

            // Only set URL if it's not null and not empty, or if this is a new task type
            if (isNewTaskType || (url != null && !url.isEmpty())) {
                taskType.setUrl(url);
            }

            // Only set credentials for new task types, never overwrite existing credentials
            if (isNewTaskType) {
                if (endpoint.has("username")) {
                    taskType.setUrlUserName(endpoint.path("username").asText(null));
                }
                if (endpoint.has("password")) {
                    taskType.setUrlPassword(endpoint.path("password").asText(null));
                }
                if (endpoint.has("token")) {
                    taskType.setUrlToken(endpoint.path("token").asText(null));
                }
            }
        }

        // Set task type metadata and configuration
        taskType.setName(config.path("name").asText());
        taskType.setDataType(config.path("dataType").asText(null));
        taskType.setDataTypeId(config.path("dataTypeId").asText(null));

        return taskType;
    }

    /**
     * Generate a summary report of imported profiles and their task class assignments
     *
     * @param profiles List of profiles to summarize
     * @return Summary string describing the task class distribution
     */
    public String generateImportSummary(List<SyncFhirProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return "No profiles to summarize";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("FHIR Profile Import Summary:\n");
        summary.append("=========================\n");

        int customTaskCount = 0;
        int genericTaskCount = 0;

        for (SyncFhirProfile profile : profiles) {
            String taskClassInfo;
            if (profile.getCustomTaskClass() != null && !profile.getCustomTaskClass().isEmpty()) {
                taskClassInfo = "Custom: " + profile.getCustomTaskClass();
                customTaskCount++;
            } else {
                taskClassInfo = "Generic Scheduler";
                genericTaskCount++;
            }

            summary.append(String.format("- %s: %s\n", profile.getName(), taskClassInfo));
        }

        summary.append("\n");
        summary.append(String.format("Total Profiles: %d\n", profiles.size()));
        summary.append(String.format("Custom Task Classes: %d\n", customTaskCount));
        summary.append(String.format("Generic Scheduler: %d\n", genericTaskCount));

        return summary.toString();
    }
}
