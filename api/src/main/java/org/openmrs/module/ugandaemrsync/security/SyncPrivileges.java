package org.openmrs.module.ugandaemrsync.security;

/**
 * Centralized privilege definitions for UgandaEMR Sync module.
 *
 * <p>All privileges follow the naming convention: "UgandaemrSync: [Action] [Resource]"</p>
 * <p>This convention ensures:</p>
 * <ul>
 *   <li>Clear and consistent privilege names</li>
 *   <li>Easy to understand what each privilege allows</li>
 *   <li>Simple privilege assignment in OpenMRS user roles</li>
 *   <li>Good audit trail and security logging</li>
 * </ul>
 *
 * <p><b>Privilege Categories:</b></p>
 * <ul>
 *   <li><b>Sync Task Management:</b> Control access to sync task operations</li>
 *   <li><b>FHIR Resource Management:</b> Control access to FHIR resource synchronization</li>
 *   <li><b>Lab Results:</b> Control access to laboratory result operations</li>
 *   <li><b>Referrals:</b> Control access to referral management</li>
 *   <li><b>Reports:</b> Control access to reporting and data export</li>
 *   <li><b>System Administration:</b> Control access to module configuration and statistics</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // In REST resources
 * @Secured(privilege = SyncPrivileges.VIEW_SYNC_TASKS)
 * public SyncTask getByUniqueId(String uniqueId) {
 *     // ...
 * }
 *
 * // In security checks
 * if (Context.hasPrivilege(SyncPrivileges.MANAGE_SYNC_TASKS)) {
 *     // Allow operation
 * }
 * }</pre>
 *
 * <p><b>Privilege Assignment:</b></p>
 * <p>These privileges must be assigned to OpenMRS roles via the Administration UI:</p>
 * <ol>
 *   <li>Go to Administration → Users → Roles</li>
 *   <li>Edit or create a role</li>
 *   <li>Select the appropriate privileges from the "UgandaemrSync" section</li>
 *   <li>Save the role</li>
 * </ol>
 *
 * @see Secured
 * @see ResourceSecurityInterceptor
 */
public class SyncPrivileges {

    // ========== Sync Task Management ==========

    /**
     * Allows viewing sync task history and status.
     * Users with this privilege can see sync task information but cannot create or modify tasks.
     */
    public static final String VIEW_SYNC_TASKS = "UgandaemrSync: View Sync Tasks";

    /**
     * Allows creating and managing sync tasks.
     * Users with this privilege can create, update, and delete sync tasks.
     * This privilege should be granted to system administrators and data managers.
     */
    public static final String MANAGE_SYNC_TASKS = "UgandaemrSync: Manage Sync Tasks";

    /**
     * Allows viewing sync task type configurations.
     * Users with this privilege can see task type definitions and settings.
     */
    public static final String VIEW_SYNC_TASK_TYPES = "UgandaemrSync: View Sync Task Types";

    /**
     * Allows managing sync task type configurations.
     * Users with this privilege can create and modify sync task types.
     * This privilege should be granted to system administrators only.
     */
    public static final String MANAGE_SYNC_TASK_TYPES = "UgandaemrSync: Manage Sync Task Types";

    // ========== FHIR Resource Management ==========

    /**
     * Allows viewing FHIR sync resources.
     * Users with this privilege can see FHIR resources but cannot modify them.
     */
    public static final String VIEW_FHIR_RESOURCES = "UgandaemrSync: View FHIR Resources";

    /**
     * Allows managing FHIR sync resources.
     * Users with this privilege can create, update, and delete FHIR resources.
     * This privilege should be granted to data managers and system administrators.
     */
    public static final String MANAGE_FHIR_RESOURCES = "UgandaemrSync: Manage FHIR Resources";

    /**
     * Allows viewing FHIR profile configurations.
     * Users with this privilege can see FHIR profile settings and mappings.
     */
    public static final String VIEW_FHIR_PROFILES = "UgandaemrSync: View FHIR Profiles";

    /**
     * Allows managing FHIR profile configurations.
     * Users with this privilege can create and modify FHIR profiles.
     * This privilege should be granted to system administrators only.
     */
    public static final String MANAGE_FHIR_PROFILES = "UgandaemrSync: Manage FHIR Profiles";

    /**
     * Allows viewing FHIR case-based profiles.
     * Users with this privilege can see FHIR case resources but cannot modify them.
     * This should be granted to clinicians and data managers.
     */
    public static final String VIEW_FHIR_CASES = "UgandaemrSync: View FHIR Cases";

    /**
     * Allows managing FHIR case-based profiles.
     * Users with this privilege can create, update, and delete FHIR case resources.
     * This should be granted to data managers and system administrators.
     */
    public static final String MANAGE_FHIR_CASES = "UgandaemrSync: Manage FHIR Cases";

    // ========== Lab Results ==========

    /**
     * Allows viewing lab results from external systems.
     * Users with this privilege can view laboratory results but cannot modify them.
     * This should be granted to clinicians, nurses, and lab staff.
     */
    public static final String VIEW_LAB_RESULTS = "UgandaemrSync: View Lab Results";

    /**
     * Allows managing lab results from external systems.
     * Users with this privilege can import, update, and delete lab results.
     * This should be granted to lab staff and data managers.
     */
    public static final String MANAGE_LAB_RESULTS = "UgandaemrSync: Manage Lab Results";

    /**
     * Allows sending lab orders to external systems like CPHL.
     * Users with this privilege can create and send viral load orders.
     * This should be granted to clinicians and lab staff.
     */
    public static final String MANAGE_LAB_ORDERS = "UgandaemrSync: Manage Lab Orders";

    // ========== Referrals ==========

    /**
     * Allows viewing referral information.
     * Users with this privilege can see referral details but cannot modify them.
     * This should be granted to clinicians and nursing staff.
     */
    public static final String VIEW_REFERRALS = "UgandaemrSync: View Referrals";

    /**
     * Allows viewing referral orders for CPHL.
     * Users with this privilege can see referral orders but cannot modify them.
     * This should be granted to clinicians and nursing staff.
     */
    public static final String VIEW_REFERRAL_ORDERS = "UgandaemrSync: View Referral Orders";

    /**
     * Allows managing referral information.
     * Users with this privilege can create, update, and delete referrals.
     * This should be granted to clinicians and referral coordinators.
     */
    public static final String MANAGE_REFERRALS = "UgandaemrSync: Manage Referrals";

    // ========== Reports ==========

    /**
     * Allows viewing sync reports and statistics.
     * Users with this privilege can access reports about sync operations and statistics.
     * This should be granted to managers and supervisors.
     */
    public static final String VIEW_REPORTS = "UgandaemrSync: View Reports";

    /**
     * Allows sending reports to external systems.
     * Users with this privilege can trigger report generation and transmission.
     * This should be granted to data managers and system administrators.
     */
    public static final String SEND_REPORTS = "UgandaemrSync: Send Reports";

    /**
     * Allows viewing sync statistics and metrics.
     * Users with this privilege can access detailed statistics about sync operations.
     * This should be granted to managers and technical staff.
     */
    public static final String VIEW_SYNC_STATS = "UgandaemrSync: View Sync Statistics";

    // ========== System Administration ==========

    /**
     * Allows managing sync module configuration.
     * Users with this privilege can modify global properties and system settings.
     * This privilege should be granted to system administrators only.
     */
    public static final String MANAGE_SYNC_CONFIG = "UgandaemrSync: Manage Sync Configuration";

    // ========== Privilege Groups for Common Access Patterns ==========

    /**
     * Read-only privileges for viewing data without ability to modify.
     * This group is appropriate for clinicians, nurses, and other staff who need
     * to view information but should not change sync configurations.
     */
    public static final String[] READ_ONLY_PRIVILEGES = {
        VIEW_SYNC_TASKS,
        VIEW_SYNC_TASK_TYPES,
        VIEW_FHIR_RESOURCES,
        VIEW_FHIR_PROFILES,
        VIEW_FHIR_CASES,
        VIEW_LAB_RESULTS,
        VIEW_REFERRALS,
        VIEW_REFERRAL_ORDERS,
        VIEW_REPORTS,
        VIEW_SYNC_STATS
    };

    /**
     * Full access privileges for complete module management.
     * This group is appropriate for system administrators and data managers
     * who need complete control over the sync module.
     */
    public static final String[] FULL_ACCESS_PRIVILEGES = {
        VIEW_SYNC_TASKS,
        MANAGE_SYNC_TASKS,
        VIEW_SYNC_TASK_TYPES,
        MANAGE_SYNC_TASK_TYPES,
        VIEW_FHIR_RESOURCES,
        MANAGE_FHIR_RESOURCES,
        VIEW_FHIR_PROFILES,
        MANAGE_FHIR_PROFILES,
        VIEW_FHIR_CASES,
        MANAGE_FHIR_CASES,
        VIEW_LAB_RESULTS,
        MANAGE_LAB_RESULTS,
        MANAGE_LAB_ORDERS,
        VIEW_REFERRALS,
        MANAGE_REFERRALS,
        VIEW_REFERRAL_ORDERS,
        VIEW_REPORTS,
        SEND_REPORTS,
        VIEW_SYNC_STATS,
        MANAGE_SYNC_CONFIG
    };

    /**
     * Data manager privileges for managing sync operations and data.
     * This group is appropriate for data managers who need to manage sync
     * operations but should not modify system configuration.
     */
    public static final String[] DATA_MANAGER_PRIVILEGES = {
        VIEW_SYNC_TASKS,
        MANAGE_SYNC_TASKS,
        VIEW_FHIR_RESOURCES,
        MANAGE_FHIR_RESOURCES,
        VIEW_FHIR_PROFILES,
        MANAGE_FHIR_PROFILES,
        VIEW_FHIR_CASES,
        MANAGE_FHIR_CASES,
        VIEW_LAB_RESULTS,
        MANAGE_LAB_RESULTS,
        MANAGE_LAB_ORDERS,
        VIEW_REFERRALS,
        MANAGE_REFERRALS,
        VIEW_REFERRAL_ORDERS,
        VIEW_REPORTS,
        SEND_REPORTS,
        VIEW_SYNC_STATS
    };

    /**
     * Clinician privileges for viewing relevant patient data.
     * This group is appropriate for clinicians and nursing staff who need
     * to view patient information but should not modify sync operations.
     */
    public static final String[] CLINICIAN_PRIVILEGES = {
        VIEW_SYNC_TASKS,
        VIEW_FHIR_RESOURCES,
        VIEW_FHIR_CASES,
        VIEW_LAB_RESULTS,
        MANAGE_LAB_ORDERS,
        VIEW_REFERRALS,
        VIEW_REFERRAL_ORDERS,
        VIEW_SYNC_STATS
    };

    // ========== Utility Methods ==========

    /**
     * Get all defined privilege names.
     *
     * @return Array of all privilege names defined in this class
     */
    public static String[] getAllPrivileges() {
        return FULL_ACCESS_PRIVILEGES;
    }

    /**
     * Check if a privilege name is valid.
     *
     * @param privilegeName The privilege name to validate
     * @return true if the privilege is defined in this class, false otherwise
     */
    public static boolean isValidPrivilege(String privilegeName) {
        for (String privilege : FULL_ACCESS_PRIVILEGES) {
            if (privilege.equals(privilegeName)) {
                return true;
            }
        }
        return false;
    }
}