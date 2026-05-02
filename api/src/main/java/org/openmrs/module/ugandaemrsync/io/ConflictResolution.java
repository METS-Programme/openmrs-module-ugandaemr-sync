package org.openmrs.module.ugandaemrsync.io;

/**
 * Conflict resolution strategies
 */
public enum ConflictResolution {
    SKIP,           // Skip import if profile exists
    OVERWRITE,      // Replace existing profile
    MERGE,          // Merge configurations
    RENAME,         // Import with new name (append timestamp)
    VERSION,        // Keep both, add version suffix
    ASK_USER        // Prompt user for resolution
}