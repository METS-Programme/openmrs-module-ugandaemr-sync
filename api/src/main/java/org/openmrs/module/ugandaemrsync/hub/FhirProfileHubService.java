package org.openmrs.module.ugandaemrsync.hub;

import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;

import java.util.List;

/**
 * Service for interacting with central FHIR profile hub
 */
public interface FhirProfileHubService {

    /**
     * Push profile to central hub
     */
    HubResponse pushProfileToHub(Integer profileId, HubConfiguration hubConfig);

    /**
     * Pull profile from central hub by ID
     */
    SyncFhirProfile pullProfileFromHub(String profileId, HubConfiguration hubConfig);

    /**
     * List all available profiles from hub
     */
    List<ProfileMetadata> listHubProfiles(HubConfiguration hubConfig);

    /**
     * Search profiles in hub
     */
    List<ProfileMetadata> searchHubProfiles(String query, HubConfiguration hubConfig);

    /**
     * Download profile JSON from hub
     */
    String downloadProfileFromHub(String profileId, HubConfiguration hubConfig);

    /**
     * Upload profile JSON to hub
     */
    HubResponse uploadProfileToHub(String profileJson, HubConfiguration hubConfig);

    /**
     * Sync profiles with hub (push local, pull remote)
     */
    SyncResult syncProfilesWithHub(HubConfiguration hubConfig, SyncOptions options);

    /**
     * Get profile version history from hub
     */
    List<ProfileVersion> getProfileHistory(String profileId, HubConfiguration hubConfig);

    /**
     * Rollback profile to specific version
     */
    SyncFhirProfile rollbackProfileVersion(String profileId, String version, HubConfiguration hubConfig);
}

/**
 * Hub configuration
 */
class HubConfiguration {
    private String hubUrl;
    private String apiKey;
    private String username;
    private String password;
    private Integer timeout;
    private boolean verifySsl;

    public String getHubUrl() { return hubUrl; }
    public void setHubUrl(String hubUrl) { this.hubUrl = hubUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public boolean isVerifySsl() { return verifySsl; }
    public void setVerifySsl(boolean verifySsl) { this.verifySsl = verifySsl; }
}

/**
 * Hub response
 */
class HubResponse {
    private boolean success;
    private String message;
    private String profileId;
    private String version;
    private int statusCode;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
}

/**
 * Profile metadata from hub
 */
class ProfileMetadata {
    private String id;
    private String name;
    private String description;
    private String category;
    private String version;
    private String author;
    private String createdAt;
    private String updatedAt;
    private List<String> tags;
    private int downloads;
    private double rating;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public int getDownloads() { return downloads; }
    public void setDownloads(int downloads) { this.downloads = downloads; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}

/**
 * Sync result
 */
class SyncResult {
    private boolean success;
    private List<String> pushedProfiles;
    private List<String> pulledProfiles;
    private List<String> errors;
    private String syncTimestamp;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public List<String> getPushedProfiles() { return pushedProfiles; }
    public void setPushedProfiles(List<String> pushedProfiles) { this.pushedProfiles = pushedProfiles; }
    public List<String> getPulledProfiles() { return pulledProfiles; }
    public void setPulledProfiles(List<String> pulledProfiles) { this.pulledProfiles = pulledProfiles; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public String getSyncTimestamp() { return syncTimestamp; }
    public void setSyncTimestamp(String syncTimestamp) { this.syncTimestamp = syncTimestamp; }
}


/**
 * Profile version
 */
class ProfileVersion {
    private String version;
    private String createdAt;
    private String createdBy;
    private String changeLog;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getChangeLog() { return changeLog; }
    public void setChangeLog(String changeLog) { this.changeLog = changeLog; }
}