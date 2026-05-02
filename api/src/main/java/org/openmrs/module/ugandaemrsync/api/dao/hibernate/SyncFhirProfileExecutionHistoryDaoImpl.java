package org.openmrs.module.ugandaemrsync.api.dao.hibernate;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ugandaemrsync.api.dao.SyncFhirProfileExecutionHistoryDao;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfileExecutionHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Hibernate implementation of SyncFhirProfileExecutionHistoryDao
 */
@Repository("ugandaemrsync.SyncFhirProfileExecutionHistoryDao")
public class SyncFhirProfileExecutionHistoryDaoImpl implements SyncFhirProfileExecutionHistoryDao {

    @Autowired
    private DbSessionFactory sessionFactory;

    public void setSessionFactory(DbSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected DbSession getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public SyncFhirProfileExecutionHistory saveOrUpdate(SyncFhirProfileExecutionHistory object) {
        getCurrentSession().saveOrUpdate(object);
        return object;
    }

    @Override
    public SyncFhirProfileExecutionHistory getById(Integer id) {
        return (SyncFhirProfileExecutionHistory) getCurrentSession().get(SyncFhirProfileExecutionHistory.class, id);
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getAll() {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.addOrder(Order.desc("executionStartTime"));
        return criteria.list();
    }

    @Override
    public void delete(SyncFhirProfileExecutionHistory object) {
        getCurrentSession().delete(object);
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getExecutionHistoryByProfile(SyncFhirProfile profile) {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.add(Restrictions.eq("profile", profile));
        criteria.addOrder(Order.desc("executionStartTime"));
        return criteria.list();
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getExecutionHistoryByProfile(SyncFhirProfile profile, int limit, int offset) {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.add(Restrictions.eq("profile", profile));
        criteria.addOrder(Order.desc("executionStartTime"));
        criteria.setMaxResults(limit);
        criteria.setFirstResult(offset);
        return criteria.list();
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getExecutionHistoryByDateRange(Date startDate, Date endDate) {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.add(Restrictions.between("executionStartTime", startDate, endDate));
        criteria.addOrder(Order.desc("executionStartTime"));
        return criteria.list();
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getRecentExecutions(int limit) {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.addOrder(Order.desc("executionStartTime"));
        criteria.setMaxResults(limit);
        return criteria.list();
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getFailedExecutions(int limit) {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.add(Restrictions.eq("executionStatus", "FAILED"));
        criteria.addOrder(Order.desc("executionStartTime"));
        criteria.setMaxResults(limit);
        return criteria.list();
    }

    @Override
    public List<SyncFhirProfileExecutionHistory> getExecutionsByStatus(String status, int limit) {
        Criteria criteria = getCurrentSession().createCriteria(SyncFhirProfileExecutionHistory.class);
        criteria.add(Restrictions.eq("executionStatus", status));
        criteria.addOrder(Order.desc("executionStartTime"));
        criteria.setMaxResults(limit);
        return criteria.list();
    }

    @Override
    public ExecutionStatistics getExecutionStatistics(SyncFhirProfile profile) {
        String sql = "SELECT " +
                "COUNT(*) as totalExecutions, " +
                "SUM(CASE WHEN executionStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulExecutions, " +
                "SUM(CASE WHEN executionStatus = 'FAILED' THEN 1 ELSE 0 END) as failedExecutions, " +
                "AVG(executionDurationMs) as averageDurationMs " +
                "FROM SyncFhirProfileExecutionHistory " +
                "WHERE profile = :profile";

        org.hibernate.SQLQuery query = getCurrentSession().createSQLQuery(sql);
        query.setParameter("profile", profile.getSyncFhirProfileId());

        Object[] result = (Object[]) query.uniqueResult();
        if (result != null) {
            return new ExecutionStatistics(
                    result[0] != null ? ((Number) result[0]).longValue() : 0L,
                    result[1] != null ? ((Number) result[1]).longValue() : 0L,
                    result[2] != null ? ((Number) result[2]).longValue() : 0L,
                    result[3] != null ? ((Number) result[3]).longValue() : 0L
            );
        }
        return new ExecutionStatistics(0L, 0L, 0L, 0L);
    }

    @Override
    public int purgeOldExecutionHistory(Date olderThan) {
        String hql = "DELETE FROM SyncFhirProfileExecutionHistory WHERE executionStartTime < :olderThan";
        org.hibernate.Query query = getCurrentSession().createQuery(hql);
        query.setParameter("olderThan", olderThan);
        return query.executeUpdate();
    }
}
