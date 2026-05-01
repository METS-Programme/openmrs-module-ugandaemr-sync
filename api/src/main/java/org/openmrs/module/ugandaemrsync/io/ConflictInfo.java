package org.openmrs.module.ugandaemrsync.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Conflict information
 */
public class ConflictInfo {
	private String profileName;
	private String conflictType;
	private String description;
	private List<String> resolutionOptions;

	public ConflictInfo() {
		this.resolutionOptions = new ArrayList<>();
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getConflictType() {
		return conflictType;
	}

	public void setConflictType(String conflictType) {
		this.conflictType = conflictType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getResolutionOptions() {
		return resolutionOptions;
	}

	public void setResolutionOptions(List<String> resolutionOptions) {
		this.resolutionOptions = resolutionOptions;
	}
}
