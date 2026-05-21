package org.openmrs.module.ugandaemrsync.model;

import org.hibernate.annotations.Type;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.ConceptSource;
import org.openmrs.PatientIdentifierType;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "ugandaemrsync.SyncFhirProfile")
@Table(name = "sync_fhir_profile")
public class SyncFhirProfile extends BaseOpenmrsData implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "sync_fhir_profile_id")
    private Integer syncFhirProfileId;

    /**
     * Default constructor that initializes required fields.
     * Sets voided to false to satisfy database NOT NULL constraint.
     */
    public SyncFhirProfile() {
        setVoided(false);
    }

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "resource_types", length = 255)
    private String resourceTypes;

    @Column(name = "profile_enabled")
    private Boolean profileEnabled;

    @Column(name = "sync_data_ever_since")
    private Boolean syncDataEverSince;

    @Column(name = "data_to_sync_start_date")
    private Date dataToSyncStartDate;

    @ManyToOne
    @JoinColumn(name = "patient_identifier_type")
    private PatientIdentifierType patientIdentifierType;

    @Column(name = "keep_profile_identifier_only")
    private Boolean keepProfileIdentifierOnly;

    @Column(name = "number_of_resources_in_bundle", length = 11)
    private Integer numberOfResourcesInBundle;

    @Column(name = "duration_to_keep_synced_resources", length = 11)
    private Integer durationToKeepSyncedResources;

    @Column(name = "generate_bundle")
    private Boolean generateBundle;

    @Column(name = "is_case_based_profile")
    private Boolean isCaseBasedProfile;

    @Column(name = "case_based_primary_resource_type", length = 255)
    private String caseBasedPrimaryResourceType;

    @Column(name = "case_based_primary_resource_type_id", length = 255)
    private String caseBasedPrimaryResourceTypeId;

    @Column(name = "resource_search_parameter", length = 5000)
    @Type(type = "text")
    private String resourceSearchParameter;

    @ManyToOne
    @JoinColumn(name = "concept_source")
    private ConceptSource conceptSource;

    @Column(name = "url_end_point")
    private String url;

    @Column(name = "url_token")
    private String urlToken;

    @Column(name = "url_username")
    private String urlUserName;

    @Column(name = "url_password")
    private String urlPassword;

    @Column(name = "sync_limit")
    private Integer syncLimit;

    @Column(name = "token_expiry_date")
    private Date tokenExpiryDate;

    @Column(name = "token_type", length = 255)
    private String tokenType;

    @Column(name = "token_refresh_key", length = 255)
    private String tokenRefreshKey;

    @Column(name = "searchable")
    private Boolean searchable;

    @Column(name = "search_url", length = 255)
    private String searchURL;


    // ===========================
    // SCHEDULING CONFIGURATION
    // ===========================

    @Column(name = "schedule_enabled")
    private Boolean scheduleEnabled = false;

    @Column(name = "schedule_type", length = 50)
    private String scheduleType; // CRON, FIXED_RATE, FIXED_DELAY, MANUAL

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "fixed_rate_interval")
    private Long fixedRateInterval; // milliseconds

    @Column(name = "fixed_delay_interval")
    private Long fixedDelayInterval; // milliseconds

    @Column(name = "start_date_time")
    private Date startDateTime;

    @Column(name = "end_date_time")
    private Date endDateTime;

    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts = 3;

    @Column(name = "custom_task_class", length = 255)
    private String customTaskClass; // Fully qualified class name of custom task to run this profile

    @Column(name = "retry_interval")
    private Long retryInterval = 60000L; // 1 minute default

    @Column(name = "timeout_duration")
    private Long timeoutDuration = 300000L; // 5 minutes default

    @Column(name = "parallel_execution")
    private Boolean parallelExecution = false;

    @Column(name = "execution_priority")
    private Integer executionPriority = 5; // 1-10, 1 highest

    // ===========================
    // TASK CONFIGURATION
    // ===========================

    @Column(name = "task_name", length = 255)
    private String taskName; // Unique task name

    @Column(name = "task_description", length = 1000)
    private String taskDescription;

    @Column(name = "task_group", length = 100)
    private String taskGroup; // For grouping related tasks

    // ===========================
    // EXECUTION TRACKING
    // ===========================

    @Column(name = "last_execution_date")
    private Date lastExecutionDate;

    @Column(name = "next_execution_date")
    private Date nextExecutionDate;

    @Column(name = "last_execution_status", length = 50)
    private String lastExecutionStatus; // SUCCESS, FAILED, RUNNING

    @Column(name = "total_executions")
    private Long totalExecutions = 0L;

    @Column(name = "successful_executions")
    private Long successfulExecutions = 0L;

    @Column(name = "failed_executions")
    private Long failedExecutions = 0L;

    @Column(name = "average_execution_time")
    private Long averageExecutionTime = 0L;

    @Column(name = "last_execution_error", length = 2000)
    private String lastExecutionError;

    public int getSyncFhirProfileId() {
        return syncFhirProfileId;
    }

    public void setSyncFhirProfileId(Integer syncFhirProfileId) {
        this.syncFhirProfileId = syncFhirProfileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getProfileEnabled() {
        return profileEnabled;
    }

    public void setProfileEnabled(Boolean profileEnabled) {
        this.profileEnabled = profileEnabled;
    }

    public Boolean getSyncDataEverSince() {
        return syncDataEverSince;
    }

    public void setSyncDataEverSince(Boolean syncDataEverSince) {
        this.syncDataEverSince = syncDataEverSince;
    }

    public Date getDataToSyncStartDate() {
        return dataToSyncStartDate;
    }

    public void setDataToSyncStartDate(Date dataToSyncStartDate) {
        this.dataToSyncStartDate = dataToSyncStartDate;
    }

    public String getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(String resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public String getResourceSearchParameter() {
        return resourceSearchParameter;
    }

    public void setResourceSearchParameter(String resourceSearchParameter) {
        this.resourceSearchParameter = resourceSearchParameter;
    }

    public Boolean getGenerateBundle() {
        return generateBundle;
    }

    public void setGenerateBundle(Boolean generateBundle) {
        this.generateBundle = generateBundle;
    }


    public Integer getNumberOfResourcesInBundle() {
        return numberOfResourcesInBundle;
    }

    public void setNumberOfResourcesInBundle(Integer numberOfResourcesInBundle) {
        this.numberOfResourcesInBundle = numberOfResourcesInBundle;
    }

    public PatientIdentifierType getPatientIdentifierType() {
        return patientIdentifierType;
    }

    public void setPatientIdentifierType(PatientIdentifierType patientIdentifierType) {
        this.patientIdentifierType = patientIdentifierType;
    }

    public Integer getDurationToKeepSyncedResources() {
        return durationToKeepSyncedResources;
    }

    public void setDurationToKeepSyncedResources(Integer durationToKeepSyncedResources) {
        this.durationToKeepSyncedResources = durationToKeepSyncedResources;
    }

    public Boolean getIsCaseBasedProfile() {
        return isCaseBasedProfile;
    }

    public void setIsCaseBasedProfile(Boolean isCaseBasedProfile) {
        this.isCaseBasedProfile = isCaseBasedProfile;
    }

    public String getCaseBasedPrimaryResourceType() {
        return caseBasedPrimaryResourceType;
    }

    public void setCaseBasedPrimaryResourceType(String caseBasedPrimaryResourceType) {
        this.caseBasedPrimaryResourceType = caseBasedPrimaryResourceType;
    }

    public String getCaseBasedPrimaryResourceTypeId() {
        return caseBasedPrimaryResourceTypeId;
    }

    public void setCaseBasedPrimaryResourceTypeId(String caseBasedPrimaryResourceTypeId) {
        this.caseBasedPrimaryResourceTypeId = caseBasedPrimaryResourceTypeId;
    }

    public ConceptSource getConceptSource() {
        return conceptSource;
    }

    public void setConceptSource(ConceptSource conceptSource) {
        this.conceptSource = conceptSource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlToken() {
        return urlToken;
    }

    public void setUrlToken(String urlToken) {
        this.urlToken = urlToken;
    }

    public String getUrlUserName() {
        return urlUserName;
    }

    public void setUrlUserName(String urlUserName) {
        this.urlUserName = urlUserName;
    }

    public String getUrlPassword() {
        return urlPassword;
    }

    public void setUrlPassword(String urlPassword) {
        this.urlPassword = urlPassword;
    }

    @Override
    public Integer getId() {
        return this.syncFhirProfileId;
    }

    @Override
    public void setId(Integer id) {
    }

    public Integer getSyncLimit() {
        return syncLimit;
    }
    public void setSyncLimit(Integer syncLimit) {
        this.syncLimit = syncLimit;
    }

    public Boolean getKeepProfileIdentifierOnly() {
        return keepProfileIdentifierOnly;
    }

    public void setKeepProfileIdentifierOnly(Boolean keepProfileIdentifierOnly) {
        this.keepProfileIdentifierOnly = keepProfileIdentifierOnly;
    }

    public void setTokenExpiryDate(Date tokenExpiryDate) {
        this.tokenExpiryDate = tokenExpiryDate;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setTokenRefreshKey(String tokenRefreshKey) {
        this.tokenRefreshKey = tokenRefreshKey;
    }

    public Date getTokenExpiryDate() {
        return tokenExpiryDate;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getTokenRefreshKey() {
        return tokenRefreshKey;
    }

    public Boolean getSearchable() {
        return searchable;
    }

    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    public String getSearchURL() {
        return searchURL;
    }

    public void setSearchURL(String searchURL) {
        this.searchURL = searchURL;
    }



    // ===========================
    // GETTERS AND SETTERS - SCHEDULING
    // ===========================

    public Boolean getScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(Boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Long getFixedRateInterval() {
        return fixedRateInterval;
    }

    public void setFixedRateInterval(Long fixedRateInterval) {
        this.fixedRateInterval = fixedRateInterval;
    }

    public Long getFixedDelayInterval() {
        return fixedDelayInterval;
    }

    public void setFixedDelayInterval(Long fixedDelayInterval) {
        this.fixedDelayInterval = fixedDelayInterval;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Long getTimeoutDuration() {
        return timeoutDuration;
    }

    public void setTimeoutDuration(Long timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }

    public Boolean getParallelExecution() {
        return parallelExecution;
    }

    public void setParallelExecution(Boolean parallelExecution) {
        this.parallelExecution = parallelExecution;
    }

    public Integer getExecutionPriority() {
        return executionPriority;
    }

    public void setExecutionPriority(Integer executionPriority) {
        this.executionPriority = executionPriority;
    }

    // Custom Task Configuration
    public String getCustomTaskClass() {
        return customTaskClass;
    }

    public void setCustomTaskClass(String customTaskClass) {
        this.customTaskClass = customTaskClass;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getTaskGroup() {
        return taskGroup;
    }

    public void setTaskGroup(String taskGroup) {
        this.taskGroup = taskGroup;
    }

    public Date getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(Date lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }

    public Date getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(Date nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

    public String getLastExecutionStatus() {
        return lastExecutionStatus;
    }

    public void setLastExecutionStatus(String lastExecutionStatus) {
        this.lastExecutionStatus = lastExecutionStatus;
    }

    public Long getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(Long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public Long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public void setSuccessfulExecutions(Long successfulExecutions) {
        this.successfulExecutions = successfulExecutions;
    }

    public Long getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(Long failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public Long getAverageExecutionTime() {
        return averageExecutionTime;
    }

    public void setAverageExecutionTime(Long averageExecutionTime) {
        this.averageExecutionTime = averageExecutionTime;
    }

    public String getLastExecutionError() {
        return lastExecutionError;
    }

    public void setLastExecutionError(String lastExecutionError) {
        this.lastExecutionError = lastExecutionError;
    }
}
