package org.openmrs.module.ugandaemrsync.io;

/**
 * Profile summary for preview
 */
public class ProfileSummary {
	private String name;
	private String category;
	private String description;
	private boolean willUpdate;
	private boolean conflicts;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isWillUpdate() {
		return willUpdate;
	}

	public void setWillUpdate(boolean willUpdate) {
		this.willUpdate = willUpdate;
	}

	public boolean isConflicts() {
		return conflicts;
	}

	public void setConflicts(boolean conflicts) {
		this.conflicts = conflicts;
	}
}
