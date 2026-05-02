package org.openmrs.module.ugandaemrsync.web.interceptor;

import org.openmrs.api.context.Context;

/**
 * Simplified security interceptor for UgandaEMR Sync module.
 * Provides authentication and authorization checking for REST resources.
 */
public class ResourceSecurityInterceptor {

    /**
     * Check if the current user has the required privilege.
     *
     * @param privilege The privilege to check
     * @throws RuntimeException if user lacks the required privilege
     */
    public static void requirePrivilege(String privilege) {
        if (!Context.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }

        if (!Context.hasPrivilege(privilege)) {
            String username = Context.getAuthenticatedUser() != null ?
                Context.getAuthenticatedUser().getUsername() : "unknown";

            throw new RuntimeException("Insufficient privileges. User '" + username +
                "' lacks required privilege: " + privilege);
        }
    }

    /**
     * Check if the current user is authenticated.
     *
     * @throws RuntimeException if user is not authenticated
     */
    public static void requireAuthentication() {
        if (!Context.isAuthenticated()) {
            throw new RuntimeException("Authentication required");
        }
    }
}
