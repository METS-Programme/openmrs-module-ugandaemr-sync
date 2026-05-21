package org.openmrs.module.ugandaemrsync.model;

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

@Entity(name = "ugandaemrsync.SyncFHIRCase")
@Table(name = "sync_fhir_case")
public class SyncFhirCase extends BaseOpenmrsData implements Serializable {

    @Id
    @GeneratedValue
    @Column(name = "case_id",length = 11)
    private Integer caseId;

    /**
     * Default constructor that initializes required fields.
     * Sets voided to false to satisfy database NOT NULL constraint.
     */
    public SyncFhirCase() {
        setVoided(false);
    }

    @Column(name = "case_identifier",length = 255)
    private String caseIdentifier;

    @ManyToOne
    @JoinColumn(name = "patient")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "profile")
    private SyncFhirProfile profile;

    @Column(name = "last_date_updated")
    private Date lastUpdateDate;

    @Column(name = "last_generation_attempt")
    private Date lastGenerationAttempt;

    @Column(name = "consecutive_generation_failures")
    private Integer consecutiveGenerationFailures;

    @Column(name = "last_generation_error_type", length = 100)
    private String lastGenerationErrorType;

    @Column(name = "last_generation_error_message", length = 1000)
    private String lastGenerationErrorMessage;

    @Column(name = "total_generation_attempts")
    private Integer totalGenerationAttempts;

    @Column(name = "last_successful_generation_date")
    private Date lastSuccessfulGenerationDate;

    @Column(name = "resource_generation_status")
    private String resourceGenerationStatus; // SUCCESS, FAILED, PENDING

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getCaseIdentifier() {
        return caseIdentifier;
    }

    public void setCaseIdentifier(String caseIdentifier) {
        this.caseIdentifier = caseIdentifier;
    }

    public SyncFhirProfile getProfile() {
        return profile;
    }

    public void setProfile(SyncFhirProfile profile) {
        this.profile = profile;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    @Override
    public Integer getId() {
        return this.getCaseId();
    }

    @Override
    public void setId(Integer id) {
        this.caseId = id;
    }

    public Date getLastGenerationAttempt() {
        return lastGenerationAttempt;
    }

    public void setLastGenerationAttempt(Date lastGenerationAttempt) {
        this.lastGenerationAttempt = lastGenerationAttempt;
    }

    public Integer getConsecutiveGenerationFailures() {
        return consecutiveGenerationFailures;
    }

    public void setConsecutiveGenerationFailures(Integer consecutiveGenerationFailures) {
        this.consecutiveGenerationFailures = consecutiveGenerationFailures;
    }

    public String getLastGenerationErrorType() {
        return lastGenerationErrorType;
    }

    public void setLastGenerationErrorType(String lastGenerationErrorType) {
        this.lastGenerationErrorType = lastGenerationErrorType;
    }

    public String getLastGenerationErrorMessage() {
        return lastGenerationErrorMessage;
    }

    public void setLastGenerationErrorMessage(String lastGenerationErrorMessage) {
        this.lastGenerationErrorMessage = lastGenerationErrorMessage;
    }

    public Integer getTotalGenerationAttempts() {
        return totalGenerationAttempts;
    }

    public void setTotalGenerationAttempts(Integer totalGenerationAttempts) {
        this.totalGenerationAttempts = totalGenerationAttempts;
    }

    public Date getLastSuccessfulGenerationDate() {
        return lastSuccessfulGenerationDate;
    }

    public void setLastSuccessfulGenerationDate(Date lastSuccessfulGenerationDate) {
        this.lastSuccessfulGenerationDate = lastSuccessfulGenerationDate;
    }

    public String getResourceGenerationStatus() {
        return resourceGenerationStatus;
    }

    public void setResourceGenerationStatus(String resourceGenerationStatus) {
        this.resourceGenerationStatus = resourceGenerationStatus;
    }

    /**
     * Marks a successful resource generation with timestamp and status updates
     */
    public void markAsGenerationSuccess(Date generationDate) {
        // Ensure voided is properly initialized (fix for legacy data where voided might be null)
        if (this.getVoided() == null) {
            this.setVoided(false);
        }
        this.lastGenerationAttempt = generationDate;
        this.lastSuccessfulGenerationDate = generationDate;
        this.resourceGenerationStatus = "SUCCESS";
        this.consecutiveGenerationFailures = 0;
        this.lastGenerationErrorType = null;
        this.lastGenerationErrorMessage = null;
        this.lastUpdateDate = generationDate;
    }

    /**
     * Marks a failed resource generation attempt with detailed error information
     */
    public void markAsGenerationFailure(Date attemptDate, SyncErrorType errorType, String errorMessage) {
        // Ensure voided is properly initialized (fix for legacy data where voided might be null)
        if (this.getVoided() == null) {
            this.setVoided(false);
        }
        this.lastGenerationAttempt = attemptDate;
        this.resourceGenerationStatus = "FAILED";
        this.lastGenerationErrorType = errorType.name();
        this.lastGenerationErrorMessage = errorMessage;

        // Increment consecutive failure count
        if (this.consecutiveGenerationFailures == null) {
            this.consecutiveGenerationFailures = 1;
        } else {
            this.consecutiveGenerationFailures++;
        }

        // Increment total attempts
        if (this.totalGenerationAttempts == null) {
            this.totalGenerationAttempts = 1;
        } else {
            this.totalGenerationAttempts++;
        }
    }

    /**
     * Checks if this case has exceeded the maximum allowed consecutive generation failures
     */
    public boolean hasExceededMaxGenerationFailures(int maxAllowedFailures) {
        return this.consecutiveGenerationFailures != null &&
               this.consecutiveGenerationFailures > maxAllowedFailures;
    }

    /**
     * Gets comprehensive generation status information for this case
     */
    public String getGenerationStatusSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("Case ID: ").append(this.caseIdentifier).append("\n");
        summary.append("Patient ID: ").append(this.getPatient() != null ? this.getPatient().getId() : "Unknown").append("\n");
        summary.append("Generation Status: ").append(this.resourceGenerationStatus != null ? this.resourceGenerationStatus : "UNKNOWN").append("\n");

        if (this.lastGenerationAttempt != null) {
            summary.append("Last Generation Attempt: ").append(this.lastGenerationAttempt).append("\n");
        }

        if (this.lastSuccessfulGenerationDate != null) {
            summary.append("Last Successful Generation: ").append(this.lastSuccessfulGenerationDate).append("\n");
        }

        if (this.consecutiveGenerationFailures != null && this.consecutiveGenerationFailures > 0) {
            summary.append("Consecutive Failures: ").append(this.consecutiveGenerationFailures).append("\n");
        }

        if (this.totalGenerationAttempts != null && this.totalGenerationAttempts > 0) {
            summary.append("Total Generation Attempts: ").append(this.totalGenerationAttempts).append("\n");
        }

        if (this.lastGenerationErrorType != null) {
            summary.append("Last Error Type: ").append(this.lastGenerationErrorType).append("\n");
        }

        if (this.lastGenerationErrorMessage != null) {
            summary.append("Last Error Message: ").append(this.lastGenerationErrorMessage).append("\n");
        }

        return summary.toString();
    }
}
