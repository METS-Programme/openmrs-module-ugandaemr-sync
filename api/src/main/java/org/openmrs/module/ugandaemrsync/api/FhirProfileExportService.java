package org.openmrs.module.ugandaemrsync.api;

import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;

import java.util.List;

/**
 * Service interface for exporting FHIR profiles and task types to JSON format
 */
public interface FhirProfileExportService {

	/**
	 * Export a single profile to JSON format
	 *
	 * @param profile the profile to export
	 * @return JSON string in import format
	 */
	String exportProfileToJson(SyncFhirProfile profile);

	/**
	 * Export all profiles to JSON format
	 *
	 * @return JSON string containing all profiles in import format
	 */
	String exportAllProfilesToJson();

	/**
	 * Export a single task type to JSON format
	 *
	 * @param taskType the task type to export
	 * @return JSON string in import format
	 */
	String exportTaskTypeToJson(SyncTaskType taskType);

	/**
	 * Export all task types to JSON format
	 *
	 * @return JSON string containing all task types in import format
	 */
	String exportAllTaskTypesToJson();

	/**
	 * Export all profiles and task types to a complete configuration JSON
	 *
	 * @return JSON string containing all profiles and task types in import format
	 */
	String exportAllConfigurationToJson();

	/**
	 * Export profiles by UUIDs
	 *
	 * @param uuids list of profile UUIDs to export
	 * @return JSON string containing specified profiles in import format
	 */
	String exportProfilesByUuids(List<String> uuids);

	/**
	 * Export task types by UUIDs
	 *
	 * @param uuids list of task type UUIDs to export
	 * @return JSON string containing specified task types in import format
	 */
	String exportTaskTypesByUuids(List<String> uuids);
}
