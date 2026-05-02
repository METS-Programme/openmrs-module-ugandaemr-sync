package org.openmrs.module.ugandaemrsync.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Handles database query execution with SQL injection protection.
 * Responsible for safe parameterized queries and input validation.
 */
public class FhirQueryExecutor {

    private static final Log log = LogFactory.getLog(FhirQueryExecutor.class);

    /**
     * Safely executes database queries with parameterized values to prevent SQL injection.
     * Uses parameterized queries instead of string formatting.
     *
     * @param query SQL query with parameter placeholders
     * @param from Start date parameter
     * @param to End date parameter
     * @param datesToBeReplaced Number of date parameters to bind
     * @param columns Column names to retrieve
     * @return List of results
     * @throws IllegalArgumentException if date parameters are invalid
     */
    public List getDatabaseRecordWithOutFacility(String query, String from, String to, int datesToBeReplaced, List<String> columns) {
        // Validate date parameters to prevent SQL injection
        validateDateParameters(from, to);

        try {
            Session session = Context.getRegisteredComponent("sessionFactory", SessionFactory.class).getCurrentSession();
            SQLQuery sqlQuery = session.createSQLQuery(query);

            // Use parameterized queries to prevent SQL injection
            if (datesToBeReplaced == 1) {
                sqlQuery.setParameter("lastSyncDate", getLastSyncDate());
                sqlQuery.setParameter("from", from);
                sqlQuery.setParameter("to", to);
            } else if (datesToBeReplaced == 2) {
                sqlQuery.setParameter("lastSyncDate1", getLastSyncDate());
                sqlQuery.setParameter("lastSyncDate2", getLastSyncDate());
                sqlQuery.setParameter("from", from);
                sqlQuery.setParameter("to", to);
            } else if (datesToBeReplaced == 3) {
                sqlQuery.setParameter("lastSyncDate1", getLastSyncDate());
                sqlQuery.setParameter("lastSyncDate2", getLastSyncDate());
                sqlQuery.setParameter("lastSyncDate3", getLastSyncDate());
                sqlQuery.setParameter("from", from);
                sqlQuery.setParameter("to", to);
            } else {
                sqlQuery.setParameter("from", from);
                sqlQuery.setParameter("to", to);
            }

            return sqlQuery.list();
        } catch (Exception e) {
            log.error("Error executing parameterized query: " + query, e);
            throw new RuntimeException("Failed to execute database query safely", e);
        }
    }

    /**
     * Executes a safe database query with proper error handling.
     * Note: This method expects queries with no user input parameters.
     * For queries with parameters, use getDatabaseRecordWithOutFacility instead.
     *
     * @param query Static SQL query with no user input
     * @return List of results
     * @throws IllegalArgumentException if query contains suspicious patterns
     */
    public List getDatabaseRecord(String query) {
        // Validate query doesn't contain string formatting patterns that suggest unsafe usage
        if (query.contains("%s") || query.contains("%d")) {
            throw new IllegalArgumentException("Query contains format specifiers. Use parameterized queries instead.");
        }

        try {
            Session session = Context.getRegisteredComponent("sessionFactory", SessionFactory.class).getCurrentSession();
            SQLQuery sqlQuery = session.createSQLQuery(query);
            return sqlQuery.list();
        } catch (Exception e) {
            log.error("Error executing database query: " + query, e);
            throw new RuntimeException("Failed to execute database query", e);
        }
    }

    /**
     * Validates date string to prevent SQL injection.
     * Only allows properly formatted date strings.
     *
     * @param dateString Date string to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidDateString(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return false;
        }

        // Check for SQL injection patterns before parsing
        String lowerDate = dateString.toLowerCase();
        if (lowerDate.contains("--") || lowerDate.contains("/*") || lowerDate.contains("*/") ||
            lowerDate.contains(" or ") || lowerDate.contains(" and ") || lowerDate.contains(";") ||
            lowerDate.contains("'") || lowerDate.contains("\"") || lowerDate.contains("=") ||
            lowerDate.contains("drop") || lowerDate.contains("delete") || lowerDate.contains("insert") ||
            lowerDate.contains("update") || lowerDate.contains("select") || lowerDate.contains("union")) {
            log.warn("Potential SQL injection detected in date parameter: " + dateString);
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            log.warn("Invalid date format detected: " + dateString);
            return false;
        }
    }

    /**
     * Sanitizes and validates date parameters for database queries.
     *
     * @param fromDate Start date string
     * @param toDate End date string
     * @throws IllegalArgumentException if dates are invalid
     */
    public void validateDateParameters(String fromDate, String toDate) {
        if (fromDate != null && !isValidDateString(fromDate)) {
            throw new IllegalArgumentException("Invalid from date format: " + fromDate);
        }
        if (toDate != null && !isValidDateString(toDate)) {
            throw new IllegalArgumentException("Invalid to date format: " + toDate);
        }
    }

    /**
     * Gets the last sync date from global properties.
     *
     * @return Last sync date string
     */
    private String getLastSyncDate() {
        SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
        return syncGlobalProperties.getGlobalProperty(SyncConstant.LAST_SYNC_DATE);
    }
}