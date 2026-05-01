package org.openmrs.module.ugandaemrsync.io;

import java.util.List;

/**
 * Import preview
 */
public class ImportPreview {
	private List<ProfileSummary> profilesToImport;
	private List<ConflictInfo> conflicts;
	private List<String> validationWarnings;

	public List<ProfileSummary> getProfilesToImport() {
		return profilesToImport;
	}

	public void setProfilesToImport(List<ProfileSummary> profilesToImport) {
		this.profilesToImport = profilesToImport;
	}

	public List<ConflictInfo> getConflicts() {
		return conflicts;
	}

	public void setConflicts(List<ConflictInfo> conflicts) {
		this.conflicts = conflicts;
	}

	public List<String> getValidationWarnings() {
		return validationWarnings;
	}

	public void setValidationWarnings(List<String> validationWarnings) {
		this.validationWarnings = validationWarnings;
	}
}
