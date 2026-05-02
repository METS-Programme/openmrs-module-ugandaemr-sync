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
			return service.saveSyncFhirProfile(profile);

		}
		catch (Exception e) {
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
			return importProfileFromJson(content);
		}
		catch (IOException e) {
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
		}
		catch (Exception e) {
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
			}
			catch (Exception e) {
				result.setSuccess(false);
				List<String> errors = new ArrayList<>();
				errors.add(e.getMessage());
				result.setErrors(errors);
			}
		}
		else {
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
		}
		catch (Exception e) {
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
					}
					catch (Exception e) {
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
					}
					catch (Exception e) {
						warnings.add("Failed to import task type: " + e.getMessage());
						log.warn("Failed to import task type", e);
					}
				}
			}

			result.setImportedProfiles(importedProfiles);
			result.setWarnings(warnings);
			result.setSuccess(true);

		}
		catch (Exception e) {
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

			// Validate required fields
			if (!metadata.has("name") || metadata.path("name").asText().isEmpty()) {
				errors.add("Profile name is required");
			}
			if (!root.has("configuration")) {
				errors.add("Configuration section is required");
			}

			result.setValid(errors.isEmpty());
			result.setErrors(errors);
			result.setWarnings(warnings);

		}
		catch (Exception e) {
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
	 *                   Expected subdirectories: hie/syncprofile/ and hie/synctasktype/
	 */
	public void importConfigurationsFromDirectory(String configPath) {
		Path configDir = Paths.get(configPath, "hie");
		if (!Files.exists(configDir)) {
			log.info("Configuration directory not found: " + configDir);
			return;
		}

		log.info("Importing configurations from: " + configDir);

		// Import profiles
		Path profileDir = configDir.resolve("syncprofile");
		if (Files.exists(profileDir)) {
			try {
				Files.list(profileDir).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
					try {
						SyncFhirProfile profile = importProfileFromFile(p.toFile());
						log.info("Imported profile: " + profile.getName());
					}
					catch (Exception e) {
						log.warn("Failed to import profile from " + p.getFileName(), e);
					}
				});
			}
			catch (IOException e) {
				log.error("Error listing profile files", e);
			}
		}

		// Import task types
		Path taskTypeDir = configDir.resolve("synctasktype");
		if (Files.exists(taskTypeDir)) {
			try {
				Files.list(taskTypeDir).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
					try {
						SyncTaskType taskType = importTaskTypeFromFile(p.toFile());
						log.info("Imported task type: " + taskType.getName());
					}
					catch (Exception e) {
						log.warn("Failed to import task type from " + p.getFileName(), e);
					}
				});
			}
			catch (IOException e) {
				log.error("Error listing task type files", e);
			}
		}
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
									log.info("Imported profile from classpath: " + profile.getName());
									importCount++;
								}
								catch (Exception e) {
									log.warn("Failed to import profile from classpath: " + file.getName(), e);
								}
							}
						}
					}
				}
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
								}
								catch (Exception e) {
									log.warn("Failed to import task type from classpath: " + file.getName(), e);
								}
							}
						}
					}
				}
			}

			return importCount > 0;

		}
		catch (Exception e) {
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
		}
		catch (Exception e) {
			log.error("Error importing task type from file: " + file.getAbsolutePath(), e);
			throw new RuntimeException("Failed to import task type: " + e.getMessage(), e);
		}
	}

	private SyncFhirProfile convertJsonToProfile(JsonNode profileNode) {
		SyncFhirProfile profile = new SyncFhirProfile();

		JsonNode metadata = profileNode.path("metadata");
		JsonNode config = profileNode.path("configuration");

		profile.setName(metadata.path("name").asText());

		profile.setResourceTypes(config.path("resourceTypes").asText(null));
		profile.setIsCaseBasedProfile(config.path("isCaseBasedProfile").asBoolean(false));
		profile.setCaseBasedPrimaryResourceType(config.path("caseBasedPrimaryResourceType").asText(null));
		profile.setResourceSearchParameter(config.path("resourceSearchParameter").asText(null));
		profile.setGenerateBundle(config.path("generateBundle").asBoolean(true));
		profile.setNumberOfResourcesInBundle(config.path("numberOfResourcesInBundle").asInt(50));
		profile.setProfileEnabled(config.path("profileEnabled").asBoolean(true));
		profile.setKeepProfileIdentifierOnly(config.path("keepProfileIdentifierOnly").asBoolean(false));
		profile.setDurationToKeepSyncedResources(config.path("durationToKeepSyncedResources").asInt(30));

		if (config.has("endpoint")) {
			JsonNode endpoint = config.path("endpoint");
			profile.setUrl(endpoint.path("url").asText(null));
			profile.setUrlUserName(endpoint.path("username").asText(null));
			profile.setUrlPassword(endpoint.path("password").asText(null));
			profile.setUrlToken(endpoint.path("token").asText(null));
		}

		return profile;
	}

	private SyncTaskType convertJsonToTaskType(JsonNode taskTypeNode) {
		SyncTaskType taskType = new SyncTaskType();

		JsonNode config = taskTypeNode.path("configuration");

		taskType.setName(config.path("name").asText());
		taskType.setDataType(config.path("dataType").asText(null));
		taskType.setDataTypeId(config.path("dataTypeId").asText(null));

		if (config.has("endpoint")) {
			JsonNode endpoint = config.path("endpoint");
			taskType.setUrl(endpoint.path("url").asText(null));
			taskType.setUrlUserName(endpoint.path("username").asText(null));
			taskType.setUrlPassword(endpoint.path("password").asText(null));
			taskType.setUrlToken(endpoint.path("token").asText(null));
		}

		return taskType;
	}
}
