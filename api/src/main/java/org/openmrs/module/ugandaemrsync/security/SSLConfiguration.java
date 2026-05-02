package org.openmrs.module.ugandaemrsync.security;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.exception.UgandaEMRSyncException;
import org.openmrs.module.ugandaemrsync.logging.StructuredLogger;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.ssl.SSLContextBuilder;

/**
 * SSL/TLS configuration for secure HTTP communications.
 * Supports proper certificate validation with configurable trust store for development environments.
 *
 * <p>This class provides secure SSL context creation that:</p>
 * <ul>
 *   <li>Enforces proper certificate validation in production environments</li>
 *   <li>Allows custom trust stores for development environments with self-signed certificates</li>
 *   <li>Logs certificate validation events for security monitoring</li>
 *   <li>Provides clear security warnings for development mode usage</li>
 * </ul>
 *
 * <p><b>Security Configuration:</b></p>
 * <ul>
 *   <li><b>Production Mode:</b> Uses Java's default trust store with proper certificate validation</li>
 *   <li><b>Development Mode:</b> Optionally uses custom trust store for self-signed certificates</li>
 * </ul>
 *
 * <p><b>Global Properties:</b></p>
 * <ul>
 *   <li><code>ugandaemrsync.ssl.developmentMode</code> - Enable development mode (default: false)</li>
 *   <li><code>ugandaemrsync.ssl.trustStore.path</code> - Path to custom trust store (development only)</li>
 *   <li><code>ugandaemrsync.ssl.trustStore.password</code> - Trust store password (default: changeit)</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * SSLContext sslContext = SSLConfiguration.createSSLContext();
 * // Use with HTTP client
 * CloseableHttpClient client = HttpClients.custom()
 *     .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
 *     .build();
 * }</pre>
 *
 * @see SSLContext
 * @see SSLContextBuilder
 */
public class SSLConfiguration {

    private static final StructuredLogger logger = StructuredLogger.getLogger(SSLConfiguration.class);

    // Global property keys
    private static final String DEV_MODE_PROPERTY = "ugandaemrsync.ssl.developmentMode";
    private static final String TRUST_STORE_PATH_PROPERTY = "ugandaemrsync.ssl.trustStore.path";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "ugandaemrsync.ssl.trustStore.password";

    // Default values
    private static final String DEFAULT_TRUST_STORE_PASSWORD = "changeit";
    private static final boolean DEFAULT_DEV_MODE = false;

    /**
     * Create SSL context with proper certificate validation.
     *
     * <p>In production mode: Uses Java's default trust store with proper certificate validation.
     * This ensures only valid certificates from trusted Certificate Authorities are accepted.</p>
     *
     * <p>In development mode: Can optionally use a custom trust store for self-signed certificates.
     * This should NEVER be enabled in production environments.</p>
     *
     * @return SSLContext configured with appropriate certificate validation
     * @throws UgandaEMRSyncException if SSL context creation fails
     */
    public static SSLContext createSSLContext() throws UgandaEMRSyncException {
        try {
            boolean isDevMode = isDevelopmentMode();

            if (isDevMode) {
                logger.warn("SSL development mode enabled - Certificate validation may be weakened");

                String trustStorePath = getTrustStorePath();
                if (trustStorePath != null && !trustStorePath.trim().isEmpty()) {
                    return createSSLContextWithCustomTrustStore(trustStorePath);
                } else {
                    logger.warn("Development mode enabled but no custom trust store configured - using default SSL context");
                }
            }

            // Production mode: Use default SSL context with proper certificate validation
            SSLContext sslContext = SSLContext.getDefault();

            logSSLContextCreation("DEFAULT", "Production mode - Using default SSL context with strict certificate validation");

            return sslContext;

        } catch (Exception e) {
            Map<String, Object> context = new HashMap<>();
            context.put("error", e.getMessage());
            logger.logValidationError("SSLContext", "createSSLContext", "Failed to create SSL context: " + e.getMessage());

            throw new UgandaEMRSyncException(
                UgandaEMRSyncException.ErrorCode.CONFIGURATION_ERROR,
                "Failed to create SSL context",
                e
            );
        }
    }

    /**
     * Create SSL context with custom trust store for development environments.
     * This allows self-signed certificates ONLY in development mode.
     *
     * @param trustStorePath Path to the custom trust store file
     * @return SSLContext with custom trust store
     * @throws Exception if trust store loading fails
     */
    private static SSLContext createSSLContextWithCustomTrustStore(String trustStorePath) throws Exception {
        String trustStorePassword = getTrustStorePassword();

        Map<String, Object> context = new HashMap<>();
        context.put("trustStorePath", trustStorePath);
        context.put("developmentMode", "true");

        logger.warn("Creating SSL context with custom trust store", context);

        KeyStore trustStore = KeyStore.getInstance("JKS");

        try (InputStream trustStoreStream = new FileInputStream(trustStorePath)) {
            trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
        }

        SSLContext sslContext = SSLContextBuilder.create()
            .loadTrustMaterial(trustStore, null)
            .build();

        logSSLContextCreation("CUSTOM_TRUSTSTORE", "Development mode - Using custom trust store: " + trustStorePath);

        return sslContext;
    }

    /**
     * Check if SSL development mode is enabled.
     * Development mode allows self-signed certificates and should NEVER be used in production.
     *
     * @return true if development mode is enabled, false otherwise
     */
    private static boolean isDevelopmentMode() {
        try {
            AdministrationService adminService = Context.getAdministrationService();
            String devModeValue = adminService.getGlobalProperty(DEV_MODE_PROPERTY);

            if (devModeValue == null || devModeValue.trim().isEmpty()) {
                return DEFAULT_DEV_MODE;
            }

            return Boolean.parseBoolean(devModeValue);

        } catch (Exception e) {
            logger.logValidationError("SSLConfiguration", "isDevelopmentMode",
                "Failed to check development mode property: " + e.getMessage());
            return DEFAULT_DEV_MODE;
        }
    }

    /**
     * Get the custom trust store path from global properties.
     *
     * @return Trust store path, or null if not configured
     */
    private static String getTrustStorePath() {
        try {
            AdministrationService adminService = Context.getAdministrationService();
            return adminService.getGlobalProperty(TRUST_STORE_PATH_PROPERTY);

        } catch (Exception e) {
            logger.logValidationError("SSLConfiguration", "getTrustStorePath",
                "Failed to get trust store path: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the custom trust store password from global properties.
     *
     * @return Trust store password, or default if not configured
     */
    private static String getTrustStorePassword() {
        try {
            AdministrationService adminService = Context.getAdministrationService();
            String password = adminService.getGlobalProperty(TRUST_STORE_PASSWORD_PROPERTY);

            if (password == null || password.trim().isEmpty()) {
                return DEFAULT_TRUST_STORE_PASSWORD;
            }

            return password;

        } catch (Exception e) {
            logger.logValidationError("SSLConfiguration", "getTrustStorePassword",
                "Failed to get trust store password: " + e.getMessage());
            return DEFAULT_TRUST_STORE_PASSWORD;
        }
    }

    /**
     * Log SSL context creation for security audit trail.
     *
     * @param contextType Type of SSL context created
     * @param description Description of the context configuration
     */
    private static void logSSLContextCreation(String contextType, String description) {
        Map<String, Object> context = new HashMap<>();
        context.put("contextType", contextType);
        context.put("description", description);
        context.put("developmentMode", isDevelopmentMode());

        if (isDevelopmentMode()) {
            logger.warn("SSL context created: " + contextType, context);
        } else {
            logger.info("SSL context created: " + contextType, context);
        }
    }
}