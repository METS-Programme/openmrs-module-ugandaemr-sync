package org.openmrs.module.ugandaemrsync.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

/**
 * SLF4J-based logging utility with structured logging support for UgandaEMR Sync operations.
 * Provides correlation tracking, performance monitoring, and consistent logging format.
 *
 * <p>This logger enhances standard SLF4J logging by:</p>
 * <ul>
 *   <li>Adding correlation IDs for request tracing across components</li>
 *   <li>Providing structured context information with each log entry via MDC</li>
 *   <li>Tracking sync operations from start to completion</li>
 *   <li>Recording performance metrics automatically</li>
 *   <li>Monitoring external service calls with detailed timing</li>
 *   <li>Generating audit trails for compliance and debugging</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * private static final SyncLogger log = SyncLogger.getLogger(MyClass.class);
 *
 * // Structured logging with context
 * Map<String, Object> context = Map.of(
 *     "correlationId", "abc-123",
 *     "patientId", 12345
 * );
 * log.info("Processing patient sync", context);
 *
 * // Sync operation tracking
 * String correlationId = log.logSyncStart("patient_sync", details);
 * try {
 *     syncPatients();
 *     log.logSyncComplete(correlationId, "patient_sync", results);
 * } catch (Exception e) {
 *     log.logSyncFailure(correlationId, "patient_sync", e, context);
 * }
 * }</pre>
 *
 * <p><b>Performance Monitoring:</b></p>
 * <pre>{@code
 * long startTime = System.currentTimeMillis();
 * // ... perform operation ...
 * long duration = System.currentTimeMillis() - startTime;
 * log.logPerformanceMetrics("patient_export", duration, patientCount, metrics);
 * }</pre>
 *
 * @author UgandaEMR Development Team
 * @version 2.0.6
 * @since 2.0.6
 */
public class SyncLogger {

    private final Logger logger;
    private final String componentName;

    private SyncLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.componentName = clazz.getSimpleName();
    }

    private SyncLogger(String componentName) {
        this.logger = LoggerFactory.getLogger(componentName);
        this.componentName = componentName;
    }

    /**
     * Creates a structured logger for the given class.
     *
     * @param clazz the class to create a logger for
     * @return a new SyncLogger instance
     */
    public static SyncLogger getLogger(Class<?> clazz) {
        return new SyncLogger(clazz);
    }

    /**
     * Creates a structured logger with the specified component name.
     *
     * @param componentName the name of the logging component
     * @return a new SyncLogger instance
     */
    public static SyncLogger getLogger(String componentName) {
        return new SyncLogger(componentName);
    }

    // ==================== Standard Logging Methods ====================

    /**
     * Log informational message with structured context.
     * Context is added to MDC (Mapped Diagnostic Context) for this log entry.
     *
     * @param message the log message
     * @param context structured context information (will be cleared after logging)
     */
    public void info(String message, Map<String, Object> context) {
        setMDC(context);
        try {
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * Log informational message.
     *
     * @param message the log message
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * Log warning message with structured context.
     *
     * @param message the log message
     * @param context structured context information
     */
    public void warn(String message, Map<String, Object> context) {
        setMDC(context);
        try {
            if (logger.isWarnEnabled()) {
                logger.warn(message);
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * Log warning message.
     *
     * @param message the log message
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * Log warning message with throwable.
     *
     * @param message the log message
     * @param throwable the exception to log
     */
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    /**
     * Log error message with structured context.
     *
     * @param message the log message
     * @param context structured context information
     */
    public void error(String message, Map<String, Object> context) {
        setMDC(context);
        try {
            if (logger.isErrorEnabled()) {
                logger.error(message);
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * Log error message.
     *
     * @param message the log message
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * Log error message with throwable.
     *
     * @param message the log message
     * @param throwable the exception to log
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Log error message with throwable and structured context.
     *
     * @param message the log message
     * @param throwable the exception to log
     * @param context structured context information
     */
    public void error(String message, Throwable throwable, Map<String, Object> context) {
        setMDC(context);
        try {
            if (logger.isErrorEnabled()) {
                logger.error(message, throwable);
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * Log debug message with structured context.
     *
     * @param message the log message
     * @param context structured context information
     */
    public void debug(String message, Map<String, Object> context) {
        setMDC(context);
        try {
            if (logger.isDebugEnabled()) {
                logger.debug(message);
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * Log debug message.
     *
     * @param message the log message
     */
    public void debug(String message) {
        logger.debug(message);
    }

    // ==================== Specialized Sync Operation Methods ====================

    /**
     * Log the start of a sync operation with correlation tracking.
     * Generates a correlation ID for tracing this operation through the system.
     *
     * @param operationType the type of sync operation (e.g., "patient_sync", "lab_results")
     * @param details additional details about the operation
     * @return correlation ID for tracking this operation
     */
    public String logSyncStart(String operationType, Map<String, Object> details) {
        String correlationId = generateCorrelationId();

        Map<String, Object> context = new java.util.HashMap<>();
        context.put("correlationId", correlationId);
        context.put("operationType", operationType);
        context.put("eventType", "SYNC_START");
        context.put("component", componentName);

        if (details != null) {
            context.putAll(details);
        }

        info("SYNC_START: " + operationType, context);
        return correlationId;
    }

    /**
     * Log successful completion of a sync operation.
     *
     * @param correlationId the correlation ID from logSyncStart
     * @param operationType the type of sync operation
     * @param results operation results and metrics
     */
    public void logSyncComplete(String correlationId, String operationType, Map<String, Object> results) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("correlationId", correlationId);
        context.put("operationType", operationType);
        context.put("eventType", "SYNC_COMPLETE");
        context.put("component", componentName);

        if (results != null) {
            context.putAll(results);
        }

        info("SYNC_COMPLETE: " + operationType, context);
    }

    /**
     * Log sync operation failure.
     *
     * @param correlationId the correlation ID from logSyncStart
     * @param operationType the type of sync operation
     * @param exception the exception that caused the failure
     * @param context additional context information
     */
    public void logSyncFailure(String correlationId, String operationType, Exception exception, Map<String, Object> context) {
        Map<String, Object> errorContext = new java.util.HashMap<>();
        errorContext.put("correlationId", correlationId);
        errorContext.put("operationType", operationType);
        errorContext.put("eventType", "SYNC_FAILED");
        errorContext.put("component", componentName);
        errorContext.put("exceptionType", exception.getClass().getSimpleName());
        errorContext.put("exceptionMessage", exception.getMessage());

        if (context != null) {
            errorContext.putAll(context);
        }

        error("SYNC_FAILED: " + operationType, exception, errorContext);
    }

    /**
     * Log performance metrics for an operation.
     * Automatically calculates records per second.
     *
     * @param operation the operation name
     * @param durationMs operation duration in milliseconds
     * @param recordCount number of records processed
     * @param additionalMetrics additional metrics to include
     */
    public void logPerformanceMetrics(String operation, long durationMs, int recordCount, Map<String, Object> additionalMetrics) {
        Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("operation", operation);
        metrics.put("durationMs", durationMs);
        metrics.put("recordCount", recordCount);
        metrics.put("recordsPerSecond", recordCount > 0 ? (recordCount * 1000.0 / durationMs) : 0);
        metrics.put("eventType", "PERFORMANCE_METRICS");
        metrics.put("component", componentName);

        if (additionalMetrics != null) {
            metrics.putAll(additionalMetrics);
        }

        info("PERFORMANCE_METRICS: " + operation, metrics);
    }

    /**
     * Log data validation failure.
     *
     * @param field the field that failed validation
     * @param value the invalid value
     * @param validationRule the validation rule that failed
     */
    public void logValidationError(String field, Object value, String validationRule) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("field", field);
        context.put("value", String.valueOf(value));
        context.put("validationRule", validationRule);
        context.put("eventType", "VALIDATION_ERROR");
        context.put("component", componentName);

        warn("VALIDATION_ERROR: Field '" + field + "' failed validation", context);
    }

    /**
     * Log external service call start.
     *
     * @param serviceType the type of service (e.g., "CPHL", "DHIS2")
     * @param endpoint the service endpoint
     * @param details additional call details
     * @return correlation ID for tracking this call
     */
    public String logExternalServiceCall(String serviceType, String endpoint, Map<String, Object> details) {
        String correlationId = generateCorrelationId();

        Map<String, Object> context = new java.util.HashMap<>();
        context.put("correlationId", correlationId);
        context.put("serviceType", serviceType);
        context.put("endpoint", endpoint);
        context.put("eventType", "EXTERNAL_CALL_START");
        context.put("component", componentName);

        if (details != null) {
            context.putAll(details);
        }

        info("EXTERNAL_CALL_START: " + serviceType + " -> " + endpoint, context);
        return correlationId;
    }

    /**
     * Log external service response.
     *
     * @param correlationId the correlation ID from logExternalServiceCall
     * @param serviceType the type of service
     * @param statusCode HTTP status code or response code
     * @param durationMs call duration in milliseconds
     */
    public void logExternalServiceResponse(String correlationId, String serviceType, int statusCode, long durationMs) {
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("correlationId", correlationId);
        context.put("serviceType", serviceType);
        context.put("statusCode", statusCode);
        context.put("durationMs", durationMs);
        context.put("eventType", "EXTERNAL_CALL_COMPLETE");
        context.put("component", componentName);

        info("EXTERNAL_CALL_COMPLETE: " + serviceType, context);
    }

    // ==================== Helper Methods ====================

    /**
     * Set MDC (Mapped Diagnostic Context) from context map.
     * MDC provides thread-local context for logging patterns.
     *
     * @param context the context information to add to MDC
     */
    private void setMDC(Map<String, Object> context) {
        if (context != null && !context.isEmpty()) {
            context.forEach((key, value) -> {
                if (value != null) {
                    MDC.put(key, String.valueOf(value));
                }
            });
        }
    }

    /**
     * Clear MDC to prevent context leakage between threads/operations.
     */
    private void clearMDC() {
        MDC.clear();
    }

    /**
     * Generate a short correlation ID for tracking operations.
     *
     * @return 8-character correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}