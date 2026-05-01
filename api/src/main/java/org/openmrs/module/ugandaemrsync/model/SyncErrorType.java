package org.openmrs.module.ugandaemrsync.model;

/**
 * Enumeration of sync error types for better categorization and tracking.
 */
public enum SyncErrorType {
    RESOURCE_GENERATION_FAILED("Failed to generate FHIR resource from database record"),
    JSON_TRANSFORMATION_FAILED("Failed to transform resource to JSON format"),
    HTTP_CONNECTION_FAILED("Failed to establish HTTP connection"),
    HTTP_AUTHENTICATION_FAILED("Authentication failed with remote server"),
    HTTP_SERVER_ERROR("Remote server returned error (5xx)"),
    HTTP_CLIENT_ERROR("Invalid request sent to server (4xx)"),
    NETWORK_TIMEOUT("Network timeout during sync"),
    DATA_VALIDATION_FAILED("Resource data validation failed"),
    CONFIGURATION_ERROR("Sync profile configuration error"),
    UNKNOWN_ERROR("Unknown error occurred");

    private final String description;

    SyncErrorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}