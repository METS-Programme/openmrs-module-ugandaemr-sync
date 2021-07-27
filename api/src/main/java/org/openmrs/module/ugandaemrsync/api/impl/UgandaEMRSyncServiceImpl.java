/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ugandaemrsync.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.ObsService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.api.dao.UgandaEMRSyncDao;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfileLog;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.server.SyncGlobalProperties;
import org.openmrs.module.ugandaemrsync.util.UgandaEMRSyncUtil;
import org.openmrs.scheduler.TaskDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.GP_DHIS2_ORGANIZATION_UUID;

public class UgandaEMRSyncServiceImpl extends BaseOpenmrsService implements UgandaEMRSyncService {

    UgandaEMRSyncDao dao;
    Log log = LogFactory.getLog(UgandaEMRSyncServiceImpl.class);

    @Autowired
    UserService userService;

    /**
     * Injected in moduleApplicationContext.xml
     */
    public void setDao(UgandaEMRSyncDao dao) {
        this.dao = dao;
    }

    /**
     * Injected in moduleApplicationContext.xml
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncTaskType()
     */
    @Override
    public List<SyncTaskType> getAllSyncTaskType() throws APIException {
        return dao.getAllSyncTaskType();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncTaskTypeByUUID(java.lang.String)
     */
    @Override
    public SyncTaskType getSyncTaskTypeByUUID(String uuid) throws APIException {
        return dao.getSyncTaskTypeByUUID(uuid);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTaskType(org.openmrs.module.ugandaemrsync.model.SyncTaskType)
     */
    @Override
    public SyncTaskType saveSyncTaskType(SyncTaskType syncTaskType) throws APIException {
        if (syncTaskType.getCreator() == null) {
            syncTaskType.setCreator(userService.getUser(1));
        }
        return dao.saveSyncTaskType(syncTaskType);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncTaskBySyncTaskId(java.lang.String)
     */
    @Override
    public SyncTask getSyncTaskBySyncTaskId(String syncTaskId) throws APIException {
        return dao.getSyncTask(syncTaskId);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncTask()
     */
    @Override
    public List<SyncTask> getAllSyncTask() {
        return dao.getAllSyncTask();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTask(org.openmrs.module.ugandaemrsync.model.SyncTask)
     */
    @Override
    public SyncTask saveSyncTask(SyncTask syncTask) throws APIException {
        if (syncTask.getCreator() == null) {
            syncTask.setCreator(userService.getUser(1));
        }
        return dao.saveSyncTask(syncTask);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getIncompleteActionSyncTask(java.lang.String)
     */
    @Override
    public List<SyncTask> getIncompleteActionSyncTask(String syncTaskTypeIdentifier) throws APIException {
        return dao.getIncompleteActionSyncTask(syncTaskTypeIdentifier);
    }

    /**
     * @param query
     * @return
     */
    @Override
    public List getDatabaseRecord(String query) {
        return dao.getDatabaseRecord(query);
    }

    /**
     * @param columns
     * @param finalQuery
     * @return
     */
    @Override
    public List getFinalList(List<String> columns, String finalQuery) {
        return dao.getFinalResults(columns, finalQuery);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#addVLToEncounter(java.lang.String, java.lang.String, java.lang.String, org.openmrs.Encounter, org.openmrs.Order)
     */
    public Encounter addVLToEncounter(String vlQualitative, String vlQuantitative, String vlDate, Encounter encounter, Order order) {
        if (!encounterHasVLDataAlreadySaved(encounter)) {
            Concept dateSampleTaken = Context.getConceptService().getConcept("163023");
            Concept viralLoadQualitative = Context.getConceptService().getConcept("1305");
            Concept viralLoadQuantitative = Context.getConceptService().getConcept("856");
            Concept valueCoded = null;

            String dateFormat = getDateFormat(vlDate);

            String vlQualitativeString = vlQualitative.replaceAll("\"", "");

            if (vlQualitativeString.contains("Target Not Detected") || vlQualitativeString.contains("Not detected")) {
                valueCoded = Context.getConceptService().getConcept("1306");
            } else if (vlQualitativeString.contains("FAILED")) {
                valueCoded = Context.getConceptService().getConcept("1304");
            } else {
                valueCoded = Context.getConceptService().getConcept("1301");
            }
            Concept viralLOadTestGroupConcept = null;
            if (order != null) {
                viralLOadTestGroupConcept = order.getConcept();
            } else {
                viralLOadTestGroupConcept = Context.getConceptService().getConcept(165412);
            }

            Obs dateSampleTakenObs = createObs(encounter, order, dateSampleTaken, null, convertStringToDate(vlDate, "00:00:00", dateFormat), null);
            Obs viralLoadQualitativeObs = createObs(encounter, order, viralLoadQualitative, valueCoded, null, null);
            Obs viralLoadQuantitativeObs = createObs(encounter, order, viralLoadQuantitative, null, null, Double.valueOf(vlQuantitative));

            Obs viralLoadTestGroupObs = createObs(encounter, order, viralLOadTestGroupConcept, null, null, null);
            viralLoadTestGroupObs.addGroupMember(dateSampleTakenObs);
            viralLoadTestGroupObs.addGroupMember(viralLoadQualitativeObs);
            viralLoadTestGroupObs.addGroupMember(viralLoadQuantitativeObs);

            //Void Similar observation
            voidObsFound(encounter, dateSampleTaken);
            voidObsFound(encounter, viralLoadQualitative);
            voidObsFound(encounter, viralLoadQuantitative);

            encounter.addObs(dateSampleTakenObs);
            encounter.addObs(viralLoadQualitativeObs);
            encounter.addObs(viralLoadQuantitativeObs);
            encounter.addObs(viralLoadTestGroupObs);

            try {
                if (order != null) {
                    Context.getOrderService().discontinueOrder(order, "Completed", new Date(), order.getOrderer(), order.getEncounter());
                }
            } catch (Exception e) {
                log.error("Failed to discontinue order", e);
            }
            Context.getObsService().saveObs(viralLoadTestGroupObs, "Adding Viral Load Data");
            return encounter;
        } else {
            return encounter;
        }
    }

    public String getDateFormat(String date) {
        String dateFormat = "";
        if (date.contains("-")) {
            dateFormat = "yyyy-MM-dd";
        } else if (date.contains("/")) {
            dateFormat = "dd/MM/yyyy";
        }
        return dateFormat;
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getPatientByPatientIdentifier(java.lang.String)
     */
    public Patient getPatientByPatientIdentifier(String patientIdentifier) {
        try {
            return Context.getPatientService().getPatientIdentifiers(patientIdentifier, null, null, null, null).get(0).getPatient();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#validateFacility(java.lang.String)
     */
    public boolean validateFacility(String facilityDHIS2UUID) {
        try {
            String globalProperty = Context.getAdministrationService().getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID);
            return facilityDHIS2UUID.contentEquals(globalProperty);
        } catch (Exception e) {
            log.error("Failed to validate facility uuid", e);
            return false;
        }

    }

    public Collection<EncounterType> getEcounterTypes(String encounterTypesUUID) {
        Collection<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(Context.getEncounterService().getEncounterTypeByUuid(encounterTypesUUID));
        return encounterTypes;
    }

    /**
     * Appends a time to a date
     *
     * @param dateString the date in string which will be
     * @param time
     * @param dateFormat
     * @return
     */
    public Date convertStringToDate(String dateString, String time, String dateFormat) {

        DateFormat format = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        Date date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        SimpleDateFormat formatterExt = new SimpleDateFormat("dd/MM/yyyy");

        try {
            date = format.parse(dateString);
            if (date != null && time != "") {
                date = formatter.parse(formatterExt.format(date) + " " + time);

            }
        } catch (ParseException e) {
            log.error("failed to convert date to string", e);
        }

        return date;
    }


    /**
     * This Method is used to generate an observation.
     *
     * @param encounter     the encounter which is has to be assigned to the observation
     * @param order         the order which has to be assigned to the observation
     * @param concept       the concept which is the question to the observation
     * @param valueCoded    concept which may be the answer to the question
     * @param valueDatetime the value date which may be the answer to the question.
     * @param valueNumeric  a numeric value which may be assigned to
     * @return
     */
    private Obs createObs(Encounter encounter, Order order, Concept concept, Concept valueCoded, Date valueDatetime, Double valueNumeric) {
        Obs newObs = new Obs();
        newObs.setConcept(concept);
        newObs.setValueCoded(valueCoded);
        newObs.setValueNumeric(valueNumeric);
        newObs.setValueDatetime(valueDatetime);
        newObs.setCreator(encounter.getCreator());
        newObs.setDateCreated(encounter.getDateCreated());
        newObs.setEncounter(encounter);
        newObs.setOrder(order);
        newObs.setPerson(encounter.getPatient());
        return newObs;
    }

    /**
     * This method is used to void any observation that is similar to what is being added
     *
     * @param encounter the encounter for the observation that will be voided
     * @param concept   the concept for the encounter that will be voided
     */
    private void voidObsFound(Encounter encounter, Concept concept) {
        ObsService obsService = Context.getObsService();
        List<Obs> obsListToVoid = obsService.getObservationsByPersonAndConcept(encounter.getPatient(), concept);
        for (Obs obsToVoid : obsListToVoid) {
            if (obsToVoid.getEncounter() == encounter) {
                obsService.voidObs(obsToVoid, "Observation has been replaced or updated.");
            }
        }
    }

    /**
     * /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getHealthCenterCode()
     */
    public String getHealthCenterCode() {
        SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
        return syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getHealthCenterName()
     */
    public String getHealthCenterName() {
        SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
        return syncGlobalProperties.getGlobalProperty("aijar.healthCenterName");
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getPatientIdentifier(org.openmrs.Patient, java.lang.String)
     */
    public String getPatientIdentifier(Patient patient, String patientIdentifierTypeUUID) {
        String query = "select patient_identifier.identifier from patient_identifier inner join patient_identifier_type on(patient_identifier.identifier_type=patient_identifier_type.patient_identifier_type_id) where patient_identifier_type.uuid in ('" + patientIdentifierTypeUUID + "') AND patient_id=" + patient.getPatientId() + "";
        List list = Context.getAdministrationService().executeSQL(query, true);
        String patientARTNO = "";
        if (!list.isEmpty()) {
            patientARTNO = list.get(0).toString().replace("[", "").replace("]", "");
        }
        return patientARTNO;
    }


    public boolean encounterHasVLDataAlreadySaved(Encounter encounter) {
        Set<Obs> obs = encounter.getAllObs(false);
        return obs.stream().map(Obs::getConcept).collect(Collectors.toSet()).contains(Context.getConceptService().getConcept(165412));
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFhirProfile(SyncFhirProfile)
     */
    @Override
    public SyncFhirProfile saveSyncFhirProfile(SyncFhirProfile syncFhirProfile) {
        return dao.saveSyncFhirProfile(syncFhirProfile);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileById(java.lang.Integer)
     */
    @Override
    public SyncFhirProfile getSyncFhirProfileById(Integer id) {
        return dao.getSyncFhirProfileById(id);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileByUUID(java.lang.String)
     */
    @Override
    public SyncFhirProfile getSyncFhirProfileByUUID(String uuid) {
        return dao.getSyncFhirProfileByUUID(uuid);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileByScheduledTaskName(java.lang.String)
     */
    @Override
    public SyncFhirProfile getSyncFhirProfileByScheduledTaskName(String scheduledTaskName) {

        TaskDefinition taskDefinition = Context.getSchedulerService().getTaskByName(scheduledTaskName);

        if (taskDefinition != null) {
            String syncFhirProfileUUID = taskDefinition.getProperty("syncFhirProfileUUID");
            SyncFhirProfile syncFhirProfile = getSyncFhirProfileByUUID(syncFhirProfileUUID);
            if (syncFhirProfile != null) {
                return syncFhirProfile;
            }
        }
        return null;
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveFHIRResource(SyncFhirResource)
     */
    @Override
    public SyncFhirResource saveFHIRResource(SyncFhirResource syncFHIRResource) {
        return dao.saveSyncFHIRResource(syncFHIRResource);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveFHIRResource(SyncFhirResource)
     */
    @Override
    public List<SyncFhirResource> getSyncFHIRResourceBySyncFhirProfile(SyncFhirProfile syncFhirProfile, boolean includeSynced) {
        return dao.getSyncResourceBySyncFhirProfile(syncFhirProfile, includeSynced);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFHIRResourceById(java.lang.Integer)
     */
    @Override
    public SyncFhirResource getSyncFHIRResourceById(Integer id) {
        return dao.getSyncFHIRResourceById(id);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#markSyncFHIRResourceSynced(SyncFhirResource)
     */
    @Override
    public SyncFhirResource markSyncFHIRResourceSynced(SyncFhirResource syncFhirResources) {
        Date today = new Date();
        syncFhirResources.setSynced(true);
        syncFhirResources.setDateSynced(today);
        syncFhirResources.setExpiryDate(UgandaEMRSyncUtil.addDaysToDate(today, syncFhirResources.getGeneratorProfile().getDurationToKeepSyncedResources()));
        return dao.saveSyncFHIRResource(syncFhirResources);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getExpiredSyncFHIRResources(java.util.Date)
     */
    @Override
    public List<SyncFhirResource> getExpiredSyncFHIRResources(Date date) {
        return dao.getExpiredSyncFHIRResources(date);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#purgeExpiredFHIRResource(java.util.Date)
     */
    @Override
    public void purgeExpiredFHIRResource(Date date) {
        for (SyncFhirResource syncFHIRResource : getExpiredSyncFHIRResources(date)) {
            dao.purgeExpiredFHIRResource(syncFHIRResource);
        }
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFhirProfileLog(SyncFhirProfileLog)
     */
    @Override
    public SyncFhirProfileLog saveSyncFhirProfileLog(SyncFhirProfileLog syncFhirProfileLog) {
        return dao.saveSyncFhirProfileLog(syncFhirProfileLog);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile,java.lang.String)
     */
    @Override
    public List<SyncFhirProfileLog> getSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile syncFhirProfile, String resourceType) {
        return dao.getSyncFhirProfileLogByProfileAndResourceName(syncFhirProfile,resourceType);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getLatestSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile,java.lang.String)
     */
    @Override
    public SyncFhirProfileLog getLatestSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile syncFhirProfile, String resourceType) {

        List<SyncFhirProfileLog> syncFhirProfileLogs = getSyncFhirProfileLogByProfileAndResourceName(syncFhirProfile, resourceType);

        if (syncFhirProfileLogs.size() > 0)
            return syncFhirProfileLogs.get(0);
        else
            return null;
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFHIRCaseBySyncFhirProfileAndPatient(SyncFhirProfile,org.openmrs.Patient,java.lang.String)
     */
    @Override
    public SyncFhirCase getSyncFHIRCaseBySyncFhirProfileAndPatient(SyncFhirProfile syncFhirProfile, Patient patient, String caseIdentifier) {
        return dao.getSyncFHIRCaseBySyncFhirProfileAndPatient(syncFhirProfile,patient,caseIdentifier);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFHIRCase(SyncFhirCase)
     */
    @Override
    public SyncFhirCase saveSyncFHIRCase(SyncFhirCase syncFHIRCase) {
        return dao.saveSyncFHIRCase(syncFHIRCase);
    }

    /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncFhirProfile()
     */
    @Override
    public List<SyncFhirProfile> getAllSyncFhirProfile() {
        return dao.getAllSyncFhirProfile();
    }


}

