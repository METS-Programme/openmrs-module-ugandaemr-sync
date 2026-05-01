package org.openmrs.module.ugandaemrsync.hub;

import org.openmrs.module.ugandaemrsync.io.ConflictResolution;

/**
 * Sync options
 */
public class SyncOptions {
    private boolean pushLocalProfiles;
    private boolean pullRemoteProfiles;
    private boolean resolveConflicts;
    private ConflictResolution conflictResolution;
    private java.util.List<String> profileIds;

    public boolean isPushLocalProfiles() { return pushLocalProfiles; }
    public void setPushLocalProfiles(boolean pushLocalProfiles) { this.pushLocalProfiles = pushLocalProfiles; }
    public boolean isPullRemoteProfiles() { return pullRemoteProfiles; }
    public void setPullRemoteProfiles(boolean pullRemoteProfiles) { this.pullRemoteProfiles = pullRemoteProfiles; }
    public boolean isResolveConflicts() { return resolveConflicts; }
    public void setResolveConflicts(boolean resolveConflicts) { this.resolveConflicts = resolveConflicts; }
    public ConflictResolution getConflictResolution() { return conflictResolution; }
    public void setConflictResolution(ConflictResolution conflictResolution) { this.conflictResolution = conflictResolution; }
    public java.util.List<String> getProfileIds() { return profileIds; }
    public void setProfileIds(java.util.List<String> profileIds) { this.profileIds = profileIds; }
}