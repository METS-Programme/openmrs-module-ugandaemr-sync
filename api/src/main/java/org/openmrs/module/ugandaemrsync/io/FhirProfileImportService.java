package org.openmrs.module.ugandaemrsync.io;

import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;

import java.io.File;
import java.util.List;

/**
 * Service for importing FHIR sync profiles and task types from various formats
 */
public interface FhirProfileImportService {

    /**
     * Import profile from JSON string
     */
    SyncFhirProfile importProfileFromJson(String jsonConfig);

    /**
     * Import profile from YAML string
     */
    SyncFhirProfile importProfileFromYaml(String yamlConfig);

    /**
     * Import profile from file
     */
    SyncFhirProfile importProfileFromFile(File file);

    /**
     * Import multiple profiles from JSON
     */
    List<SyncFhirProfile> importProfilesFromJson(String jsonConfig);

    /**
     * Import multiple profiles from YAML
     */
    List<SyncFhirProfile> importProfilesFromYaml(String yamlConfig);

    /**
     * Import profile with validation
     */
    ImportResult importProfileWithValidation(String jsonConfig);

    /**
     * Import profile with conflict resolution
     */
    SyncFhirProfile importProfileWithConflictResolution(String jsonConfig, ConflictResolution strategy);

    /**
     * Import encrypted profile
     */
    SyncFhirProfile importEncryptedProfile(String encryptedJson, String decryptionKey);

    /**
     * Preview import without committing
     */
    ImportPreview previewImport(String jsonConfig);

    /**
     * Import all configuration from export
     */
    ImportResult importAllConfiguration(String exportJson);

    /**
     * Validate profile configuration
     */
    ValidationResult validateProfile(String jsonConfig);
}