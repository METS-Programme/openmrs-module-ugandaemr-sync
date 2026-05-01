package org.openmrs.module.ugandaemrsync.io;

import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;

import java.util.List;

/**
 * Import result
 */
public class ImportResult {
	private boolean success;
	private List<SyncFhirProfile> importedProfiles;
	private List<String> errors;
	private List<String> warnings;
	private ValidationResult validation;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public List<SyncFhirProfile> getImportedProfiles() {
		return importedProfiles;
	}

	public void setImportedProfiles(List<SyncFhirProfile> importedProfiles) {
		this.importedProfiles = importedProfiles;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	public ValidationResult getValidation() {
		return validation;
	}

	public void setValidation(ValidationResult validation) {
		this.validation = validation;
	}
}
