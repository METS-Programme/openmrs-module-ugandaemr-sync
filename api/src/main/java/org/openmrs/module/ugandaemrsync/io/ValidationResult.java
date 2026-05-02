package org.openmrs.module.ugandaemrsync.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result
 */
public class ValidationResult {
	private boolean valid;
	private List<String> errors;
	private List<String> warnings;
	private List<String> info;

	public ValidationResult() {
		this.errors = new ArrayList<>();
		this.warnings = new ArrayList<>();
		this.info = new ArrayList<>();
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
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

	public List<String> getInfo() {
		return info;
	}

	public void setInfo(List<String> info) {
		this.info = info;
	}
}
