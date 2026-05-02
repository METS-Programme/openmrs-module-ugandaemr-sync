package org.openmrs.module.ugandaemrsync.api.dao;

import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfileExecutionHistory;

import java.util.Date;
import java.util.List;

/**
 * DAO interface for SyncFhirProfileExecutionHistory operations.
 */
public interface SyncFhirProfileExecutionHistoryDao {

    /**
     * Save or update execution history record
     */
    SyncFhirProfileExecutionHistory saveOrUpdate(SyncFhirProfileExecutionHistory object);

    /**
     * Get execution history by ID
     */
    SyncFhirProfileExecutionHistory getById(Integer id);

    /**
     * Get all execution history records
     */
    List<SyncFhirProfileExecutionHistory> getAll();

    /**
     * Delete execution history record
     */
    void delete(SyncFhirProfileExecutionHistory object);

    /**
     * Get execution history for a specific profile
     *
     * @param profile the profile to get history for
     * @return list of execution history records, most recent first
     */
    List<SyncFhirProfileExecutionHistory> getExecutionHistoryByProfile(SyncFhirProfile profile);

    /**
     * Get execution history for a specific profile with pagination
     *
     * @param profile the profile to get history for
     * @param limit   maximum number of records to return
     * @param offset  starting offset
     * @return list of execution history records
     */
    List<SyncFhirProfileExecutionHistory> getExecutionHistoryByProfile(SyncFhirProfile profile, int limit, int offset);

    /**
     * Get execution history within a date range
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of execution history records
     */
    List<SyncFhirProfileExecutionHistory> getExecutionHistoryByDateRange(Date startDate, Date endDate);

    /**
     * Get recent executions for all profiles
     *
     * @param limit maximum number of records to return
     * @return list of recent execution history records
     */
    List<SyncFhirProfileExecutionHistory> getRecentExecutions(int limit);

    /**
     * Get failed executions
     *
     * @param limit maximum number of records to return
     * @return list of failed execution history records
     */
    List<SyncFhirProfileExecutionHistory> getFailedExecutions(int limit);

    /**
     * Get executions by status
     *
     * @param status the execution status (SUCCESS, FAILED, TIMEOUT)
     * @param limit  maximum number of records to return
     * @return list of execution history records
     */
    List<SyncFhirProfileExecutionHistory> getExecutionsByStatus(String status, int limit);

    /**
     * Get execution statistics for a profile
     *
     * @param profile the profile to get stats for
     * @return map containing statistics (total count, success count, failure count, avg duration)
     */
    ExecutionStatistics getExecutionStatistics(SyncFhirProfile profile);

    /**
     * Delete old execution history records
     *
     * @param olderThan delete records older than this date
     * @return number of records deleted
     */
    int purgeOldExecutionHistory(Date olderThan);

    /**
     * Inner class to hold execution statistics
     */
    class ExecutionStatistics {
        private Long totalExecutions;
        private Long successfulExecutions;
        private Long failedExecutions;
        private Long averageDurationMs;

        public ExecutionStatistics() {
        }

        public ExecutionStatistics(Long totalExecutions, Long successfulExecutions, Long failedExecutions, Long averageDurationMs) {
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.averageDurationMs = averageDurationMs;
        }

        public Long getTotalExecutions() {
            return totalExecutions;
        }

        public void setTotalExecutions(Long totalExecutions) {
            this.totalExecutions = totalExecutions;
        }

        public Long getSuccessfulExecutions() {
            return successfulExecutions;
        }

        public void setSuccessfulExecutions(Long successfulExecutions) {
            this.successfulExecutions = successfulExecutions;
        }

        public Long getFailedExecutions() {
            return failedExecutions;
        }

        public void setFailedExecutions(Long failedExecutions) {
            this.failedExecutions = failedExecutions;
        }

        public Long getAverageDurationMs() {
            return averageDurationMs;
        }

        public void setAverageDurationMs(Long averageDurationMs) {
            this.averageDurationMs = averageDurationMs;
        }
    }
}
