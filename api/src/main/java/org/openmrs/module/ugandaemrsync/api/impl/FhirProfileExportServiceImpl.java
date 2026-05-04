package org.openmrs.module.ugandaemrsync.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.FhirProfileExportService;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FHIR profile export service
 */
public class FhirProfileExportServiceImpl implements FhirProfileExportService {

	private static final Log log = LogFactory.getLog(FhirProfileExportServiceImpl.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String exportProfileToJson(SyncFhirProfile profile) {
		try {
			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", convertProfileToExportFormat(profile));
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting profile to JSON", e);
			throw new RuntimeException("Failed to export profile: " + e.getMessage(), e);
		}
	}

	@Override
	public String exportAllProfilesToJson() {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			List<SyncFhirProfile> profiles = service.getAllSyncFhirProfile();

			ObjectNode exportNode = objectMapper.createObjectNode();
			ArrayNode profilesArray = objectMapper.createArrayNode();

			for (SyncFhirProfile profile : profiles) {
				profilesArray.add(convertProfileToExportFormat(profile));
			}

			exportNode.set("profiles", profilesArray);

			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", exportNode);

			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting all profiles to JSON", e);
			throw new RuntimeException("Failed to export all profiles: " + e.getMessage(), e);
		}
	}

	@Override
	public String exportTaskTypeToJson(SyncTaskType taskType) {
		try {
			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", convertTaskTypeToExportFormat(taskType));
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting task type to JSON", e);
			throw new RuntimeException("Failed to export task type: " + e.getMessage(), e);
		}
	}

	@Override
	public String exportAllTaskTypesToJson() {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			List<SyncTaskType> taskTypes = service.getAllSyncTaskType();

			ObjectNode exportNode = objectMapper.createObjectNode();
			ArrayNode taskTypesArray = objectMapper.createArrayNode();

			for (SyncTaskType taskType : taskTypes) {
				taskTypesArray.add(convertTaskTypeToExportFormat(taskType));
			}

			exportNode.set("taskTypes", taskTypesArray);

			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", exportNode);

			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting all task types to JSON", e);
			throw new RuntimeException("Failed to export all task types: " + e.getMessage(), e);
		}
	}

	@Override
	public String exportAllConfigurationToJson() {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			List<SyncFhirProfile> profiles = service.getAllSyncFhirProfile();
			List<SyncTaskType> taskTypes = service.getAllSyncTaskType();

			ObjectNode exportNode = objectMapper.createObjectNode();

			// Add profiles
			ArrayNode profilesArray = objectMapper.createArrayNode();
			for (SyncFhirProfile profile : profiles) {
				profilesArray.add(convertProfileToExportFormat(profile));
			}
			exportNode.set("profiles", profilesArray);

			// Add task types
			ArrayNode taskTypesArray = objectMapper.createArrayNode();
			for (SyncTaskType taskType : taskTypes) {
				taskTypesArray.add(convertTaskTypeToExportFormat(taskType));
			}
			exportNode.set("taskTypes", taskTypesArray);

			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", exportNode);

			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting all configuration to JSON", e);
			throw new RuntimeException("Failed to export all configuration: " + e.getMessage(), e);
		}
	}

	@Override
	public String exportProfilesByUuids(List<String> uuids) {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			List<SyncFhirProfile> profiles = new ArrayList<>();

			for (String uuid : uuids) {
				SyncFhirProfile profile = service.getSyncFhirProfileByUUID(uuid);
				if (profile != null) {
					profiles.add(profile);
				}
			}

			ObjectNode exportNode = objectMapper.createObjectNode();
			ArrayNode profilesArray = objectMapper.createArrayNode();

			for (SyncFhirProfile profile : profiles) {
				profilesArray.add(convertProfileToExportFormat(profile));
			}

			exportNode.set("profiles", profilesArray);

			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", exportNode);

			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting profiles by UUIDs to JSON", e);
			throw new RuntimeException("Failed to export profiles by UUIDs: " + e.getMessage(), e);
		}
	}

	@Override
	public String exportTaskTypesByUuids(List<String> uuids) {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			List<SyncTaskType> taskTypes = new ArrayList<>();

			for (String uuid : uuids) {
				SyncTaskType taskType = service.getSyncTaskTypeByUUID(uuid);
				if (taskType != null) {
					taskTypes.add(taskType);
				}
			}

			ObjectNode exportNode = objectMapper.createObjectNode();
			ArrayNode taskTypesArray = objectMapper.createArrayNode();

			for (SyncTaskType taskType : taskTypes) {
				taskTypesArray.add(convertTaskTypeToExportFormat(taskType));
			}

			exportNode.set("taskTypes", taskTypesArray);

			ObjectNode root = objectMapper.createObjectNode();
			root.set("export", exportNode);

			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		} catch (Exception e) {
			log.error("Error exporting task types by UUIDs to JSON", e);
			throw new RuntimeException("Failed to export task types by UUIDs: " + e.getMessage(), e);
		}
	}

	private JsonNode convertProfileToExportFormat(SyncFhirProfile profile) {
		ObjectNode profileNode = objectMapper.createObjectNode();

		// Metadata section
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("id", profile.getUuid());
		metadata.put("name", profile.getName());
		metadata.put("category", "DEFAULT");
		metadata.put("description", "FHIR sync profile for " + profile.getName());
		metadata.put("version", "1.0");
		profileNode.set("metadata", metadata);

		// Configuration section
		ObjectNode config = objectMapper.createObjectNode();
		config.put("name", profile.getName());
		config.put("resourceTypes", profile.getResourceTypes());
		config.put("isCaseBasedProfile", profile.getIsCaseBasedProfile());
		config.put("caseBasedPrimaryResourceType", profile.getCaseBasedPrimaryResourceType());
		config.put("caseBasedPrimaryResourceTypeId", profile.getCaseBasedPrimaryResourceTypeId());

		// Parse resourceSearchParameter if it exists
		if (profile.getResourceSearchParameter() != null) {
			try {
				JsonNode searchParam = objectMapper.readTree(profile.getResourceSearchParameter());
				config.set("resourceSearchParameter", searchParam);
			} catch (Exception e) {
				log.warn("Failed to parse resourceSearchParameter for profile: " + profile.getName(), e);
			}
		}

		config.put("generateBundle", profile.getGenerateBundle());
		config.put("numberOfResourcesInBundle", profile.getNumberOfResourcesInBundle());
		config.put("profileEnabled", profile.getProfileEnabled());
		config.put("keepProfileIdentifierOnly", profile.getKeepProfileIdentifierOnly());
		config.put("durationToKeepSyncedResources", profile.getDurationToKeepSyncedResources());
		config.put("syncLimit", profile.getSyncLimit());
		config.put("searchable", profile.getSearchable());
		config.put("searchURL", profile.getSearchURL() != null ? profile.getSearchURL() : "");
		config.put("patientIdentifierType", profile.getPatientIdentifierType() != null ? profile.getPatientIdentifierType().toString() : "4");
		config.put("patientIdentifierTypeName", "");

		// Endpoint section
		ObjectNode endpoint = objectMapper.createObjectNode();
		endpoint.put("url", profile.getUrl() != null ? profile.getUrl() : "");
		endpoint.put("username", profile.getUrlUserName());
		endpoint.put("password", profile.getUrlPassword());
		endpoint.put("token", profile.getUrlToken());
		config.set("endpoint", endpoint);

		profileNode.set("configuration", config);

		return profileNode;
	}

	private JsonNode convertTaskTypeToExportFormat(SyncTaskType taskType) {
		ObjectNode taskTypeNode = objectMapper.createObjectNode();

		// Metadata section
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("id", taskType.getUuid());
		metadata.put("name", taskType.getName());
		metadata.put("description", "Sync task type for " + taskType.getName());
		taskTypeNode.set("metadata", metadata);

		// Configuration section
		ObjectNode config = objectMapper.createObjectNode();
		config.put("name", taskType.getName());
		config.put("dataType", taskType.getDataType());
		config.put("dataTypeId", taskType.getDataTypeId());

		// Endpoint section
		ObjectNode endpoint = objectMapper.createObjectNode();
		endpoint.put("url", taskType.getUrl() != null ? taskType.getUrl() : "");
		endpoint.put("username", taskType.getUrlUserName());
		endpoint.put("password", taskType.getUrlPassword());
		endpoint.put("token", taskType.getUrlToken());
		config.set("endpoint", endpoint);

		taskTypeNode.set("configuration", config);

		return taskTypeNode;
	}
}
