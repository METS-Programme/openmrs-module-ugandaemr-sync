package org.openmrs.module.ugandaemrsync.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing the result of a viral load CSV upload operation
 */
public class ViralLoadUploadResult {

    private boolean success;
    private String errorMessage;
    private String healthCenterNameValidator;
    private int processedCount;
    private int successCount;
    private List<String> noEncounterFound = new ArrayList<>();
    private List<String> noPatientFound = new ArrayList<>();
    private List<String> patientResultNotReleased = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getHealthCenterNameValidator() {
        return healthCenterNameValidator;
    }

    public void setHealthCenterNameValidator(String healthCenterNameValidator) {
        this.healthCenterNameValidator = healthCenterNameValidator;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public List<String> getNoEncounterFound() {
        return noEncounterFound;
    }

    public void setNoEncounterFound(List<String> noEncounterFound) {
        this.noEncounterFound = noEncounterFound;
    }

    public List<String> getNoPatientFound() {
        return noPatientFound;
    }

    public void setNoPatientFound(List<String> noPatientFound) {
        this.noPatientFound = noPatientFound;
    }

    public List<String> getPatientResultNotReleased() {
        return patientResultNotReleased;
    }

    public void setPatientResultNotReleased(List<String> patientResultNotReleased) {
        this.patientResultNotReleased = patientResultNotReleased;
    }
}
