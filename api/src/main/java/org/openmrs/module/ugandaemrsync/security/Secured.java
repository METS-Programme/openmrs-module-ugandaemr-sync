package org.openmrs.module.ugandaemrsync.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Security annotation for REST resources to enforce authentication, authorization, and rate limiting.
 *
 * <p>This annotation provides declarative security for REST resources by specifying:</p>
 * <ul>
 *   <li><b>Authentication:</b> Whether user must be logged in</li>
 *   <li><b>Authorization:</b> Which privileges are required to access the resource</li>
 *   <li><b>Rate Limiting:</b> Maximum requests per minute per user</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Require authentication only
 * @Secured(authenticated = true)
 * public class MyResource extends DelegatingCrudResource<MyClass> {
 *     // ...
 * }
 *
 * // Require specific privilege
 * @Secured(privilege = SyncPrivileges.VIEW_SYNC_TASKS)
 * public SyncTask getByUniqueId(String uniqueId) {
 *     // ...
 * }
 *
 * // Require multiple privileges (user must have at least one)
 * @Secured(privilege = {SyncPrivileges.VIEW_SYNC_TASKS, SyncPrivileges.MANAGE_SYNC_TASKS})
 * public List<SyncTask> getAll() {
 *     // ...
 * }
 *
 * // Apply rate limiting (60 requests per minute)
 * @Secured(authenticated = true, rateLimit = 60)
 * public List<SyncTask> search(String query) {
 *     // ...
 * }
 * }</pre>
 *
 * <p><b>Security Enforcement:</b></p>
 * <ul>
 *   <li>Authentication is checked using {@code Context.isAuthenticated()}</li>
 *   <li>Authorization is checked using {@code Context.hasPrivilege(String[])}</li>
 *   <li>Rate limiting is enforced per user ID</li>
 *   <li>All security violations are logged for audit purposes</li>
 * </ul>
 *
 * <p><b>Default Values:</b></p>
 * <ul>
 *   <li><code>authenticated</code>: true (authentication required by default)</li>
 *   <li><code>privilege</code>: {} (no specific privilege required)</li>
 *   <li><code>rateLimit</code>: 0 (no rate limiting)</li>
 * </ul>
 *
 * @see ResourceSecurityInterceptor
 * @see SyncPrivileges
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Secured {

    /**
     * Required privilege(s) to access the resource.
     * If multiple privileges are specified, the user must have at least one of them.
     * If no privileges are specified (empty array), only authentication is checked.
     *
     * @return Array of required privilege names
     */
    String[] privilege() default {};

    /**
     * Whether authentication is required to access the resource.
     * When set to true, the user must be logged in via {@code Context.isAuthenticated()}.
     * When set to false, the resource is publicly accessible (not recommended for sensitive data).
     *
     * @return true if authentication is required, false otherwise
     */
    boolean authenticated() default true;

    /**
     * Rate limit in requests per minute per authenticated user.
     * When set to 0 (default), no rate limiting is applied.
     * When set to a positive value, users exceeding this limit will receive HTTP 429 (Too Many Requests).
     *
     * <p>Rate limiting is enforced per user ID. Unauthenticated requests cannot be rate limited.</p>
     *
     * <p><b>Recommended Values:</b></p>
     * <ul>
     *   <li>API endpoints: 60-100 requests per minute</li>
     *   <li>Data export endpoints: 20-30 requests per minute</li>
     *   <li>Administrative endpoints: 10-20 requests per minute</li>
     * </ul>
     *
     * @return Maximum requests per minute per user, or 0 for no limit
     */
    int rateLimit() default 0;
}