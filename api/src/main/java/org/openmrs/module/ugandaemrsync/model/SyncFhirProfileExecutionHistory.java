package org.openmrs.module.ugandaemrsync.model;

import org.openmrs.BaseOpenmrsObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * SyncFhirProfileExecutionHistory tracks the execution history of FHIR profile sync operations.
 * This provides an audit trail for debugging and monitoring profile performance.
 */
@Entity(name = "ugandaemrsync.SyncFhirProfileExecutionHistory")
@Table(name = "sync_fhir_profile_execution_history")
public class SyncFhirProfileExecutionHistory extends BaseOpenmrsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_history_id")
    private Integer executionHistoryId;

    @ManyToOne
    @JoinColumn(name = "sync_fhir_profile_id", nullable = false)
    private SyncFhirProfile profile;

    @Column(name = "execution_start_time", nullable = false)
    private Date executionStartTime;

    @Column(name = "execution_end_time")
    private Date executionEndTime;

    @Column(name = "execution_status", nullable = false, length = 50)
    private String executionStatus; // SUCCESS, FAILED, TIMEOUT

    @Column(name = "resources_generated")
    private Integer resourcesGenerated;

    @Column(name = "cases_processed")
    private Integer casesProcessed;

    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    // ===========================
    // GETTERS AND SETTERS
    // ===========================

    @Override
    public Integer getId() {
        return executionHistoryId;
    }

    @Override
    public void setId(Integer id) {
        this.executionHistoryId = id;
    }

    public Integer getExecutionHistoryId() {
        return executionHistoryId;
    }

    public void setExecutionHistoryId(Integer executionHistoryId) {
        this.executionHistoryId = executionHistoryId;
    }

    public SyncFhirProfile getProfile() {
        return profile;
    }

    public void setProfile(SyncFhirProfile profile) {
        this.profile = profile;
    }

    public Date getExecutionStartTime() {
        return executionStartTime;
    }

    public void setExecutionStartTime(Date executionStartTime) {
        this.executionStartTime = executionStartTime;
    }

    public Date getExecutionEndTime() {
        return executionEndTime;
    }

    public void setExecutionEndTime(Date executionEndTime) {
        this.executionEndTime = executionEndTime;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public Integer getResourcesGenerated() {
        return resourcesGenerated;
    }

    public void setResourcesGenerated(Integer resourcesGenerated) {
        this.resourcesGenerated = resourcesGenerated;
    }

    public Integer getCasesProcessed() {
        return casesProcessed;
    }

    public void setCasesProcessed(Integer casesProcessed) {
        this.casesProcessed = casesProcessed;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
