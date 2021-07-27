/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ugandaemrsync.api.dao;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openmrs.Patient;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ugandaemrsync.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository("ugandaemrsync.UgandaEMRSyncDao")
public class UgandaEMRSyncDao {

    @Autowired
    DbSessionFactory sessionFactory;

    /**
     * @return
     */
    private DbSession getSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncTaskType()
     */
    public List<SyncTaskType> getAllSyncTaskType() {
        return (List<SyncTaskType>) getSession().createCriteria(SyncTaskType.class).list();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTaskType(org.openmrs.module.ugandaemrsync.model.SyncTaskType)
     */
    public SyncTaskType getSyncTaskTypeByUUID(String uuid) {
        return (SyncTaskType) getSession().createCriteria(SyncTaskType.class).add(Restrictions.eq("uuid", uuid))
                .uniqueResult();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTaskType(org.openmrs.module.ugandaemrsync.model.SyncTaskType)
     */
    public SyncTaskType saveSyncTaskType(SyncTaskType syncTaskType) {
        getSession().saveOrUpdate(syncTaskType);
        return syncTaskType;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncTaskBySyncTaskId(java.lang.String)
     */
    public SyncTask getSyncTask(String syncTask) {
        return (SyncTask) getSession().createCriteria(SyncTask.class).add(Restrictions.eq("syncTask", syncTask))
                .uniqueResult();
    }

    /**
     * /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncTask()
     */
    public List<SyncTask> getAllSyncTask() {
        return (List<SyncTask>) getSession().createCriteria(SyncTask.class).list();
    }


    /**
     * /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTask(org.openmrs.module.ugandaemrsync.model.SyncTask)
     */
    public SyncTask saveSyncTask(SyncTask syncTask) {
        getSession().saveOrUpdate(syncTask);
        return syncTask;
    }

    /**
     * @param query
     * @return
     */
    public List getDatabaseRecord(String query) {
        SQLQuery sqlQuery = getSession().createSQLQuery(query);
        return sqlQuery.list();
    }

    /**
     * @param columns
     * @param finalQuery
     * @return
     */
    public List getFinalResults(List<String> columns, String finalQuery) {
        SQLQuery sqlQuery = getSession().createSQLQuery(finalQuery);
        for (String column : columns) {
            sqlQuery.addScalar(column, StringType.INSTANCE);
        }
        return sqlQuery.list();
    }

    /**
     * /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getIncompleteActionSyncTask(java.lang.String)
     */
    public List<SyncTask> getIncompleteActionSyncTask(String syncTaskTypeIdentifier) {
        SQLQuery sqlQuery = getSession()
                .createSQLQuery(
                        "select sync_task.* from sync_task inner join sync_task_type on (sync_task_type.sync_task_type_id=sync_task.sync_task_type) where sync_task_type.data_type_id='"
                                + syncTaskTypeIdentifier
                                + "' and  require_action=true and action_completed=false;");
        sqlQuery.addEntity(SyncTask.class);
        return sqlQuery.list();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileById(java.lang.Integer)
     */
    public SyncFhirProfile getSyncFhirProfileById(Integer id) {
        return (SyncFhirProfile) getSession().createCriteria(SyncFhirProfile.class).add(Restrictions.eq("syncFhirProfileId", id))
                .uniqueResult();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileByUUID(java.lang.String)
     */
    public SyncFhirProfile getSyncFhirProfileByUUID(String uuid) {
        return (SyncFhirProfile) getSession().createCriteria(SyncFhirProfile.class).add(Restrictions.eq("uuid", uuid))
                .uniqueResult();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFhirProfile(SyncFhirProfile)
     */
    public SyncFhirProfile saveSyncFhirProfile(SyncFhirProfile syncFhirProfile) {
        getSession().saveOrUpdate(syncFhirProfile);
        return syncFhirProfile;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveFHIRResource(SyncFhirResource)
     */
    public SyncFhirResource saveSyncFHIRResource(SyncFhirResource syncFHIRResource) {
        getSession().saveOrUpdate(syncFHIRResource);

        return syncFHIRResource;

    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveFHIRResource(SyncFhirResource)
     */
    public List<SyncFhirResource> getSyncResourceBySyncFhirProfile(SyncFhirProfile syncFhirProfile, boolean includeSynced) {

        Criteria criteria = getSession().createCriteria(SyncFhirResource.class);

        if (syncFhirProfile != null) {
            criteria.add(Restrictions.eq("generatorProfile", syncFhirProfile));
        }

        if (!includeSynced) {
            criteria.add(Restrictions.eq("synced", false));
        }

        criteria.addOrder(Order.desc("dateCreated"));

        return criteria.list();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFHIRResourceById(java.lang.Integer)
     */
    public SyncFhirResource getSyncFHIRResourceById(Integer id) {

        return (SyncFhirResource) getSession().createCriteria(SyncFhirResource.class).add(Restrictions.eq("resourceId", id))
                .uniqueResult();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#purgeExpiredFHIRResource(java.util.Date)
     */
    public void purgeExpiredFHIRResource(SyncFhirResource syncFHIRResource) {
        getSession().delete(syncFHIRResource);
    }

    public List<SyncFhirResource> getExpiredSyncFHIRResources(Date date) {

        Criteria criteria = getSession().createCriteria(SyncFhirResource.class).add(Restrictions.le("expiryDate", date));

        criteria.addOrder(Order.desc("expiryDate"));

        return criteria.list();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFhirProfileLog(SyncFhirProfileLog)
     */
    public SyncFhirProfileLog saveSyncFhirProfileLog(SyncFhirProfileLog syncFhirProfileLog) {
        getSession().saveOrUpdate(syncFhirProfileLog);
        return syncFhirProfileLog;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile,java.lang.String)
     */
    public List<SyncFhirProfileLog> getSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile syncFhirProfile, String resourceType) {
        Criteria criteria = getSession().createCriteria(SyncFhirProfileLog.class);

        if (syncFhirProfile != null && resourceType != null) {
            criteria.add(Restrictions.eq("resourceType", resourceType));
            criteria.add(Restrictions.eq("profile", syncFhirProfile));
            criteria.addOrder(Order.desc("dateCreated"));
        }

        return criteria.list();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFHIRCaseBySyncFhirProfileAndPatient(SyncFhirProfile,org.openmrs.Patient,java.lang.String)
     */
    public SyncFhirCase getSyncFHIRCaseBySyncFhirProfileAndPatient(SyncFhirProfile syncFhirProfile, Patient patient, String caseIdentifier) {
        Criteria criteria = getSession().createCriteria(SyncFhirCase.class);
        criteria.add(Restrictions.eq("profile", syncFhirProfile));
        criteria.add(Restrictions.eq("patient", patient));
        criteria.add(Restrictions.eq("caseIdentifier", caseIdentifier));

        return (SyncFhirCase) criteria.uniqueResult();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFHIRCase(SyncFhirCase)
     */
    public SyncFhirCase saveSyncFHIRCase(SyncFhirCase syncFHIRCase) {

        getSession().saveOrUpdate(syncFHIRCase);

        return syncFHIRCase;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncFhirProfile()
     */
    public List<SyncFhirProfile> getAllSyncFhirProfile() {
        return (List<SyncFhirProfile>) getSession().createCriteria(SyncFhirProfile.class).list();
    }
}
