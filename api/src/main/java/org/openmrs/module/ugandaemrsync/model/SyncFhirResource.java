package org.openmrs.module.ugandaemrsync.model;


import org.hibernate.annotations.Type;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "ugandaemrsync.SyncFHIRResources")
@Table(name = "sync_fhir_resource")
public class SyncFhirResource extends BaseOpenmrsData implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "resource_id", length = 11)
    private int resourceId;

    /**
     * Default constructor that initializes required fields.
     * Sets voided to false to satisfy database NOT NULL constraint.
     */
    public SyncFhirResource() {
        setVoided(false);
    }

    @Column(name = "synced")
    private Boolean synced;

    @Column(name = "statusCode")
    private Integer statusCode;

    @Column(name = "status_code_detail")
    private String statusCodeDetail;

    @Column(name = "date_synced")
    private Date dateSynced;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "last_sync_attempt")
    private Date lastSyncAttempt;

    @Column(name = "consecutive_failure_count")
    private Integer consecutiveFailureCount;

    @Column(name = "last_sync_error_type", length = 100)
    private String lastSyncErrorType;

    @Column(name = "last_sync_error_message", length = 1000)
    private String lastSyncErrorMessage;

    @Column(name = "sync_retry_count")
    private Integer syncRetryCount;

    @Column(name = "last_successful_sync_date")
    private Date lastSuccessfulSyncDate;

    @ManyToOne
    @JoinColumn(name = "generator_profile")
    private SyncFhirProfile generatorProfile;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "resource", length = 1000000)
    @Type(type="text")
    private String resource;


    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public Boolean getSynced() {
        return synced;
    }

    public void setSynced(Boolean synced) {
        this.synced = synced;
    }

    public Date getDateSynced() {
        return dateSynced;
    }

    public void setDateSynced(Date dateSynced) {
        this.dateSynced = dateSynced;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }


    public SyncFhirProfile getGeneratorProfile() {
        return generatorProfile;
    }

    public void setGeneratorProfile(SyncFhirProfile generatorProfile) {
        this.generatorProfile = generatorProfile;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public Integer getId() {
        return this.resourceId;
    }

    @Override
    public void setId(Integer id) {
        this.resourceId = id;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusCodeDetail() {
        return statusCodeDetail;
    }

    public void setStatusCodeDetail(String statusCodeDetail) {
        this.statusCodeDetail = statusCodeDetail;
    }


    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Date getLastSyncAttempt() {
        return lastSyncAttempt;
    }

    public void setLastSyncAttempt(Date lastSyncAttempt) {
        this.lastSyncAttempt = lastSyncAttempt;
    }

    public Integer getConsecutiveFailureCount() {
        return consecutiveFailureCount;
    }

    public void setConsecutiveFailureCount(Integer consecutiveFailureCount) {
        this.consecutiveFailureCount = consecutiveFailureCount;
    }

    public String getLastSyncErrorType() {
        return lastSyncErrorType;
    }

    public void setLastSyncErrorType(String lastSyncErrorType) {
        this.lastSyncErrorType = lastSyncErrorType;
    }

    public String getLastSyncErrorMessage() {
        return lastSyncErrorMessage;
    }

    public void setLastSyncErrorMessage(String lastSyncErrorMessage) {
        this.lastSyncErrorMessage = lastSyncErrorMessage;
    }

    public Integer getSyncRetryCount() {
        return syncRetryCount;
    }

    public void setSyncRetryCount(Integer syncRetryCount) {
        this.syncRetryCount = syncRetryCount;
    }

    public Date getLastSuccessfulSyncDate() {
        return lastSuccessfulSyncDate;
    }

    public void setLastSuccessfulSyncDate(Date lastSuccessfulSyncDate) {
        this.lastSuccessfulSyncDate = lastSuccessfulSyncDate;
    }

    /**
     * Updates sync status on successful sync
     */
    public void markAsSuccessfullySynced(Date syncDate) {
        this.synced = true;
        this.dateSynced = syncDate;
        this.lastSyncAttempt = syncDate;
        this.lastSuccessfulSyncDate = syncDate;
        this.consecutiveFailureCount = 0;
        this.lastSyncErrorType = null;
        this.lastSyncErrorMessage = null;
    }

    /**
     * Updates sync status on failed sync attempt
     */
    public void markAsSyncFailed(Date attemptDate, SyncErrorType errorType, String errorMessage) {
        this.synced = false;
        this.lastSyncAttempt = attemptDate;
        this.lastSyncErrorType = errorType.name();
        this.lastSyncErrorMessage = errorMessage;

        // Increment consecutive failure count
        if (this.consecutiveFailureCount == null) {
            this.consecutiveFailureCount = 1;
        } else {
            this.consecutiveFailureCount++;
        }

        // Increment retry count
        if (this.syncRetryCount == null) {
            this.syncRetryCount = 1;
        } else {
            this.syncRetryCount++;
        }
    }

    /**
     * Checks if this resource has exceeded the maximum allowed consecutive failures
     */
    public boolean hasExceededMaxFailures(int maxAllowedFailures) {
        return this.consecutiveFailureCount != null && this.consecutiveFailureCount > maxAllowedFailures;
    }
}
