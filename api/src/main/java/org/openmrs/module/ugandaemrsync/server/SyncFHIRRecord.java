package org.openmrs.module.ugandaemrsync.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.module.fhir2.api.*;
import org.openmrs.module.fhir2.api.FhirEpisodeOfCareService;
import org.openmrs.module.fhir2.api.search.param.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfileLog;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.model.SyncErrorType;
import org.openmrs.module.ugandaemrsync.service.SyncCaseGenerationTrackingService;
import org.openmrs.module.ugandaemrsync.service.SyncErrorTrackingService;
import org.openmrs.module.ugandaemrsync.util.UgandaEMRSyncUtil;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hl7.fhir.r4.model.Patient.SP_IDENTIFIER;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.FSHR_SYNC_FHIR_PROFILE_UUID;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.CROSS_BORDER_CR_SYNC_FHIR_PROFILE_UUID;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.PATIENT_ID_TYPE_CROSS_BORDER_UUID;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.PATIENT_ID_TYPE_CROSS_BORDER_NAME;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.LAST_SYNC_DATE;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.GP_ENABLE_SYNC_CBS_FHIR_DATA;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.GP_DEFAULT_LOCATION_UUID;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.DEFAULT_LOCATION_UUID_PLACE_HOLDER;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.PERSON_UUID_QUERY;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.PRACTITIONER_UUID_QUERY;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.PATIENT_UUID_QUERY;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.ENCOUNTER_UUID_QUERY;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.OBSERVATION_UUID_QUERY;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIRSERVER_SYNC_TASK_TYPE_UUID;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.GP_DHIS2;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIR_BUNDLE_RESOURCE_TRANSACTION;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIR_BUNDLE_CASE_RESOURCE_TRANSACTION;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIR_BUNDLE_RESOURCE_METHOD_POST;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIR_BUNDLE_RESOURCE_METHOD_PUT;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.ENCOUNTER_ROLE;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIR_CODING_DATATYPE;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.PASSPORT_IDENTIFIER_SYSTEM_URL_GP;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.NATIONAL_ID_IDENTIFIER_SYSTEM_URL_GP;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.NHPI_IDENTIFIER_SYSTEM_URL_GP;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.OPENMRS_IDENTIFIER_SYSTEM_URL_GP;

/**
 * Created by lubwamasamuel on 07/11/2016.
 */
public class SyncFHIRRecord {

    UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();

    Log log = LogFactory.getLog(SyncFHIRRecord.class);
    private static final Logger logger = LoggerFactory.getLogger(SyncFHIRRecord.class);

    private SyncFhirProfile syncFhirProfile;

    String healthCenterIdentifier;
    String healthCenterName;
    String lastSyncDate;

    SyncFhirCase syncFhirCase = null;

    private SyncFhirProfile profile = null;
    private List<PatientProgram> patientPrograms;

    Map<String, Object> anyOtherObject = new HashMap<>();

    // Extracted components for better separation of concerns
    private final FhirQueryExecutor queryExecutor = new FhirQueryExecutor();
    private FhirResourceGenerator resourceGenerator;
    private FhirResourceTransformer resourceTransformer;
    private final SyncErrorTrackingService errorTrackingService = new SyncErrorTrackingService();
    private final SyncCaseGenerationTrackingService caseTrackingService = new SyncCaseGenerationTrackingService();


    public SyncFHIRRecord() {
        healthCenterIdentifier = Context.getAdministrationService().getGlobalProperty(GP_DHIS2);

        // Get default location UUID from global properties
        String defaultLocationUuid = Context.getAdministrationService().getGlobalProperty(GP_DEFAULT_LOCATION_UUID);
        if (defaultLocationUuid == null || defaultLocationUuid.isEmpty()) {
            defaultLocationUuid = DEFAULT_LOCATION_UUID_PLACE_HOLDER;
        }

        Location location = Context.getLocationService().getLocationByUuid(defaultLocationUuid);
        if (location != null) {
            healthCenterName = location.getName();
        } else {
            log.warn("Default location not found for UUID: " + defaultLocationUuid + ", using default");
            healthCenterName = "Unknown Location";
        }

        lastSyncDate = Context.getAdministrationService().getGlobalProperty(LAST_SYNC_DATE);

        // Initialize resource transformer with health center info (will be set profile later)
        profile = new SyncFhirProfile(); // placeholder
        resourceTransformer = new FhirResourceTransformer(healthCenterIdentifier, healthCenterName, profile);
    }

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
    private List getDatabaseRecordWithOutFacility(String query, String from, String to, int datesToBeReplaced, List<String> columns) {
        // Delegate to FhirQueryExecutor for safe database operations
        return queryExecutor.getDatabaseRecordWithOutFacility(query, from, to, datesToBeReplaced, columns);
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
    private List getDatabaseRecord(String query) {
        // Delegate to FhirQueryExecutor for safe database operations
        return queryExecutor.getDatabaseRecord(query);
    }


    public List<Map> processFHIRData(List<String> dataToProcess, String dataType, boolean addOrganizationToRecord) {
        List<Map> maps = new ArrayList<>();
        SyncTaskType syncTaskType = Context.getService(UgandaEMRSyncService.class).getSyncTaskTypeByUUID(FHIRSERVER_SYNC_TASK_TYPE_UUID);

        FhirPersonService fhirPersonService;
        FhirPatientService fhirPatientService;
        FhirPractitionerService fhirPractitionerService;
        FhirEncounterService fhirEncounterService;
        FhirObservationService fhirObservationService;


        try {
            Field serviceContextField = Context.class.getDeclaredField("serviceContext");
            serviceContextField.setAccessible(true);
            try {
                ApplicationContext applicationContext = ((ServiceContext) serviceContextField.get(null))
                        .getApplicationContext();

                fhirPersonService = applicationContext.getBean(FhirPersonService.class);
                fhirPatientService = applicationContext.getBean(FhirPatientService.class);
                fhirEncounterService = applicationContext.getBean(FhirEncounterService.class);
                fhirObservationService = applicationContext.getBean(FhirObservationService.class);
                fhirPractitionerService = applicationContext.getBean(FhirPractitionerService.class);


            } finally {
                serviceContextField.setAccessible(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (String data : dataToProcess) {
            try {

                IParser parser = FhirContext.forR4().newJsonParser();
                String jsonData = "";

                if (dataType == "Patient") {
                    jsonData = parser.encodeResourceToString(fhirPatientService.get(data));
                } else if (dataType.equals("Person")) {
                    jsonData = parser.encodeResourceToString(fhirPersonService.get(data));
                } else if (dataType.equals("Encounter")) {
                    jsonData = parser.encodeResourceToString(fhirEncounterService.get(data));
                } else if (dataType.equals("Observation")) {
                    jsonData = parser.encodeResourceToString(fhirObservationService.get(data));
                } else if (dataType.equals("Practitioner")) {
                    jsonData = parser.encodeResourceToString(fhirPractitionerService.get(data));
                }

                if (!jsonData.equals("")) {
                    if (addOrganizationToRecord) {
                        jsonData = addOrganizationToRecord(jsonData, "managingOrganization");
                    }
                    Map map = ugandaEMRHttpURLConnection.sendPostBy(syncTaskType.getUrl() + dataType, syncTaskType.getUrlUserName(), syncTaskType.getUrlPassword(), "", jsonData, false);
                    map.put("DataType", dataType);
                    map.put("uuid", data);
                    maps.add(map);
                }

            } catch (Exception e) {
                log.error(e);
            }


        }
        return maps;
    }

    public String addOrganizationToRecord(String payload, String attributeName) {
        // Delegate to FhirResourceTransformer
        return resourceTransformer.addOrganizationToRecord(payload, attributeName);
    }

    public String addServiceType(String payload, String attributeName) {
        if (payload.isEmpty()) {
            return "";
        }
        JSONObject finalPayLoadJson = new JSONObject(payload);
        finalPayLoadJson.put(attributeName, new JSONObject("{\"coding\" : [{\"code\": \"dcd87b79-30ab-102d-86b0-7a5022ba4115\", \"display\": \"MEDICAL OUTPATIENT\"}],\"text\" : \"Out-Patient\"}"));
        return finalPayLoadJson.toString();
    }

    /**
     * Adds location to encounter Resource
     *
     * @param payload
     * @return
     */
    public String addLocationToEncounterResource(String payload) {
        if (payload.isEmpty()) {
            return "";
        }
        JSONObject finalPayLoadJson = new JSONObject(payload);


        return finalPayLoadJson.toString();
    }

    public List<Map> syncFHIRData() {

        List<Map> mapList = new ArrayList<>();

        SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();

        if (syncGlobalProperties.getGlobalProperty(GP_ENABLE_SYNC_CBS_FHIR_DATA).equals("true")) {

            try {
                mapList.addAll(processFHIRData(getDatabaseRecordWithOutFacility(PERSON_UUID_QUERY, "", "", 3, Arrays.asList("uuid")), "Person", false));

                mapList.addAll(processFHIRData(getDatabaseRecordWithOutFacility(PRACTITIONER_UUID_QUERY, "", "", 3, Arrays.asList("uuid")), "Practitioner", true));

                mapList.addAll(processFHIRData(getDatabaseRecordWithOutFacility(PATIENT_UUID_QUERY, "", "", 3, Arrays.asList("uuid")), "Patient", true));

                mapList.addAll(processFHIRData(getDatabaseRecordWithOutFacility(ENCOUNTER_UUID_QUERY, "", "", 3, Arrays.asList("uuid")), "Encounter", false));

                mapList.addAll(processFHIRData(getDatabaseRecordWithOutFacility(OBSERVATION_UUID_QUERY, "", "", 2, Arrays.asList("uuid")), "Observation", false));

                Date now = new Date();
                if (!mapList.isEmpty()) {
                    String newSyncDate = SyncConstant.DEFAULT_DATE_FORMAT.format(now);

                    syncGlobalProperties.setGlobalProperty(LAST_SYNC_DATE, newSyncDate);
                }
            } catch (Exception e) {
                log.error("Failed to process sync records central server", e);
            }
        } else {
            Map map = new HashMap();
            map.put("error", "Syncing of CBS Data is not enabled. Please enable it and proceed");
            mapList.add(map);
        }

        return mapList;
    }


    public Collection<SyncFhirResource> generateCaseBasedFHIRResourceBundles(SyncFhirProfile syncFhirProfile) {
        this.profile = syncFhirProfile;
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        if (syncFhirProfile != null && (!syncFhirProfile.getIsCaseBasedProfile() || syncFhirProfile.getCaseBasedPrimaryResourceType() == null)) {
            return null;
        }

        this.syncFhirProfile = syncFhirProfile;

        Collection<SyncFhirResource> syncFhirResources = new ArrayList<>();


        List<Integer> savedResourcesIds = new ArrayList<>();

        Date currentDate = new Date();

        identifyNewCases(syncFhirProfile, currentDate);


        List<SyncFhirCase> syncFhirCases = ugandaEMRSyncService.getSyncFhirCasesByProfile(syncFhirProfile);

        for (SyncFhirCase syncFhirCase : syncFhirCases) {
            SyncFhirResource syncFhirResource = saveCaseResources(syncFhirProfile, syncFhirCase);
            if (syncFhirResource != null) {
                savedResourcesIds.add(syncFhirResource.getId());
            }
        }

        if (savedResourcesIds.size() > 0) {
            SyncFhirProfileLog syncFhirProfileLog = new SyncFhirProfileLog();
            syncFhirProfileLog.setNumberOfResources(savedResourcesIds.size());
            syncFhirProfileLog.setProfile(syncFhirProfile);
            assert syncFhirProfile != null;
            syncFhirProfileLog.setResourceType(syncFhirProfile.getCaseBasedPrimaryResourceType());
            syncFhirProfileLog.setLastGenerationDate(currentDate);
            ugandaEMRSyncService.saveSyncFhirProfileLog(syncFhirProfileLog);
        }

        return syncFhirResources;
    }


    /**
     * Create and Identify new cases based on a given Sync Fhir Profile
     *
     * @param syncFhirProfile the profile for which the cases belong to
     * @param currentDate     Date when this task is being executed,
     */
    public void identifyNewCases(SyncFhirProfile syncFhirProfile, Date currentDate) {

        List<PatientProgram> patientProgramList;

        if (syncFhirProfile.getCaseBasedPrimaryResourceType().equals("EpisodeOfCare")) {
            Collection<Program> programs = new ArrayList<>();
            Program program = Context.getProgramWorkflowService().getProgramByUuid(syncFhirProfile.getCaseBasedPrimaryResourceTypeId());
            patientProgramList = Context.getProgramWorkflowService().getPatientPrograms(null, program, null, null, null, null, false);


            for (PatientProgram patientProgram : patientProgramList) {
                Patient patient = patientProgram.getPatient();
                if (!patient.getVoided()) {
                    String caseIdentifier = patientProgram.getUuid();
                    saveSyncFHIRCase(syncFhirProfile, currentDate, patient, caseIdentifier);
                }
            }
        } else if (syncFhirProfile.getCaseBasedPrimaryResourceType().equals("Encounter")) {
            List<EncounterType> encounterTypes = new ArrayList<>();

            encounterTypes.add(Context.getEncounterService().getEncounterTypeByUuid(syncFhirProfile.getCaseBasedPrimaryResourceTypeId()));

            EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, null, null, null, null, encounterTypes, null, null, null, false);


            for (Encounter encounter : Context.getEncounterService().getEncounters(encounterSearchCriteria)) {

                PatientIdentifier patientIdentifier = getPatientIdentifierByType(encounter.getPatient(), syncFhirProfile.getPatientIdentifierType());

                if (patientIdentifier != null) {
                    saveSyncFHIRCase(syncFhirProfile, currentDate, encounter.getPatient(), patientIdentifier.getIdentifier());
                }

            }
        } else if (syncFhirProfile.getCaseBasedPrimaryResourceType().equals("ProgramWorkFlowState")) {
            ProgramWorkflowService programWorkflowService = Context.getProgramWorkflowService();

            ProgramWorkflowState programWorkflowState = programWorkflowService.getStateByUuid(syncFhirProfile.getCaseBasedPrimaryResourceTypeId());

            List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(null, programWorkflowState.getProgramWorkflow().getProgram(), null, null, null, null, false);

            for (PatientProgram patientProgram : patientPrograms) {
                PatientState patientState = patientProgram.getCurrentState(programWorkflowState.getProgramWorkflow());
                if (patientState != null && patientState.getState().equals(programWorkflowState)) {
                    Patient patient = patientProgram.getPatient();
                    String caseIdentifier = patientProgram.getUuid();
                    saveSyncFHIRCase(syncFhirProfile, currentDate, patient, caseIdentifier);
                }
            }

        } else if (syncFhirProfile.getCaseBasedPrimaryResourceType().equals("Order")) {
            OrderService orderService = Context.getOrderService();
            OrderType orderType = orderService.getOrderTypeByUuid(syncFhirProfile.getCaseBasedPrimaryResourceTypeId());

            if (orderType != null) {

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                Date today = new Date();

                UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
                List<Integer> patientIds = ugandaEMRSyncService.getPatientsByOrderTypeAndDate(orderType.getId(), today);
                List<Patient> patientList = new ArrayList<>();

                for (Integer patientId : patientIds) {
                    Patient patient = Context.getPatientService().getPatient(patientId);
                    if (patient != null) {
                        patientList.add(patient);
                    }
                }
                for (Patient patient : patientList) {
                    String patientIdentifier = patient.getPatientId().toString();
                    saveSyncFHIRCase(syncFhirProfile, currentDate, patient, patientIdentifier);
                }
            }
        } else if (syncFhirProfile.getCaseBasedPrimaryResourceType().equals("PatientIdentifierType")) {
            PatientService patientService = Context.getPatientService();
            PatientIdentifierType patientIdentifierType = patientService.getPatientIdentifierTypeByUuid(syncFhirProfile.getCaseBasedPrimaryResourceTypeId());

            if (patientIdentifierType != null) {

                UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
                List<Integer> patientIds = ugandaEMRSyncService.getPatientsByIdentifierTypeExcludingProfile(patientIdentifierType.getId(), syncFhirProfile.getId());

                List<Patient> patientList = new ArrayList<>();

                for (Integer patientId : patientIds) {
                    Patient patient = patientService.getPatient(patientId);
                    if (patient != null) {
                        patientList.add(patient);
                    }
                }
                for (Patient patient : patientList.stream().filter(patient -> !patient.getVoided()).collect(Collectors.toList())) {
                    String patientIdentifier = patient.getPatientId().toString();
                    saveSyncFHIRCase(syncFhirProfile, currentDate, patient, patientIdentifier);
                }
            }
        } else if (syncFhirProfile.getCaseBasedPrimaryResourceType().equals("CohortType")) {
            String uuid = syncFhirProfile.getCaseBasedPrimaryResourceTypeId();

            List<Patient> patientList = getPatientByCohortType(uuid);
            if (patientList.size() > 0) {
                for (Patient patient : patientList) {
                    String patientIdentifier = patient.getPatientId().toString();
                    saveSyncFHIRCase(syncFhirProfile, currentDate, patient, patientIdentifier);
                }
            }

        }
    }

    public SyncFhirCase saveSyncFHIRCase(SyncFhirProfile syncFhirProfile, Date currentDate, Patient patient, String caseIdentifier) {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        SyncFhirCase syncFhirCase = ugandaEMRSyncService.getSyncFHIRCaseBySyncFhirProfileAndPatient(syncFhirProfile, patient, caseIdentifier);
        if (syncFhirCase == null) {
            syncFhirCase = new SyncFhirCase();
            syncFhirCase.setCaseIdentifier(caseIdentifier);
            syncFhirCase.setPatient(patient);
            syncFhirCase.setProfile(syncFhirProfile);
            syncFhirCase.setDateCreated(currentDate);
            return ugandaEMRSyncService.saveSyncFHIRCase(syncFhirCase);
        }

        return null;

    }

    public SyncFhirResource saveCaseResources(SyncFhirProfile syncFhirProfile, SyncFhirCase syncFhirCase) {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        SyncCaseGenerationTrackingService trackingService = new SyncCaseGenerationTrackingService();

        try {
            String resource = generateFHIRCaseResource(syncFhirProfile, syncFhirCase);

            if (resource != null && !resource.isEmpty()) {
                SyncFhirResource syncFHIRResource = new SyncFhirResource();
                syncFHIRResource.setGeneratorProfile(syncFhirProfile);
                syncFHIRResource.setResource(resource);
                syncFHIRResource.setSynced(false);
                syncFHIRResource.setPatient(syncFhirCase.getPatient());
                ugandaEMRSyncService.saveFHIRResource(syncFHIRResource);

                // Track successful generation
                trackingService.recordSuccessfulGeneration(syncFhirCase, syncFHIRResource, ugandaEMRSyncService);

                return syncFHIRResource;
            } else {
                // Track null/empty resource failure
                trackingService.recordNullOrEmptyResourceFailure(syncFhirCase, ugandaEMRSyncService,
                        syncFhirProfile.getCaseBasedPrimaryResourceType(),
                        "Case ID: " + syncFhirCase.getCaseIdentifier());
                return null;
            }
        } catch (Exception e) {
            // Track generation failure with exception details
            trackingService.recordFhirGenerationFailure(syncFhirCase, ugandaEMRSyncService,
                    e, "Case ID: " + syncFhirCase.getCaseIdentifier() +
                       ", Patient ID: " + (syncFhirCase.getPatient() != null ? syncFhirCase.getPatient().getId() : "Unknown"));
            return null;
        }
    }


    private String generateFHIRCaseResource(SyncFhirProfile syncFhirProfile, SyncFhirCase syncFHIRCase) {

        Collection<String> resources = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();
        Date currentDate = new Date();
        Date lastUpdateDate;
        List<Order> orderList = new ArrayList<>();

        if (syncFHIRCase.getLastUpdateDate() == null) {
            if (!syncFhirProfile.getSyncDataEverSince() && syncFhirProfile.getDataToSyncStartDate() == null) {
                lastUpdateDate = OpenmrsUtil.firstSecondOfDay(new Date());
            } else {
                lastUpdateDate = getDefaultLastSyncDate();
            }
        } else {
            lastUpdateDate = syncFHIRCase.getLastUpdateDate();
        }


        String[] resourceTypes = syncFhirProfile.getResourceTypes().split(",");
        OrderService orderService = Context.getOrderService();

        for (String resource : resourceTypes) {
            switch (resource) {
                case "Patient":
                    List<PatientIdentifier> patientIdentifiers = new ArrayList<>();
                    patientIdentifiers.add(getPatientIdentifierByType(syncFHIRCase.getPatient(), syncFhirProfile.getPatientIdentifierType()));
                    resources.addAll(groupInCaseBundle("Patient", getPatientResourceBundle(syncFhirProfile, patientIdentifiers, syncFHIRCase), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "Person":
                    List<Person> personList = new ArrayList<>();
                    Person person = syncFHIRCase.getPatient().getPerson();

                    if (syncFHIRCase.getLastUpdateDate() == null) {
                        personList.add(syncFHIRCase.getPatient().getPerson());
                    } else if ((person.getDateChanged() != null && person.getDateChanged().after(syncFHIRCase.getLastUpdateDate())) || (person.getDateCreated() != null && person.getDateCreated().after(syncFHIRCase.getLastUpdateDate()))) {
                        personList.add(syncFHIRCase.getPatient().getPerson());
                    }

                    if (!personList.isEmpty()) {
                        resources.addAll(groupInCaseBundle("Person", getPersonResourceBundle(syncFhirProfile, personList, syncFHIRCase), syncFhirProfile.getPatientIdentifierType().getName()));
                    }
                    break;
                case "EpisodeOfCare":
                    JSONArray jsonArray = getSearchParametersInJsonObject("EpisodeOfCare", syncFhirProfile.getResourceSearchParameter()).getJSONArray("type");

                    List<PatientProgram> patientProgramList = new ArrayList<>();

                    for (Object jsonObject : jsonArray) {
                        patientProgramList = Context.getProgramWorkflowService().getPatientPrograms(syncFHIRCase.getPatient(), Context.getProgramWorkflowService().getProgramByUuid(jsonObject.toString()), lastUpdateDate, currentDate, null, null, false);
                    }

                    if (patientProgramList.size() > 0) {
                        resources.addAll(groupInCaseBundle("EpisodeOfCare", getEpisodeOfCareResourceBundle(patientProgramList), syncFhirProfile.getPatientIdentifierType().getName()));
                    }
                    anyOtherObject.put("episodeOfCare", patientProgramList);
                    break;
                case "Encounter":
                    List<EncounterType> encounterTypes = new ArrayList<>();
                    DateRangeParam encounterLastUpdated = new DateRangeParam().setUpperBoundInclusive(currentDate).setLowerBoundInclusive(lastUpdateDate);
                    JSONArray encounterUUIDS = getSearchParametersInJsonObject("Encounter", syncFhirProfile.getResourceSearchParameter()).getJSONArray("type");

                    for (Object jsonObject : encounterUUIDS) {
                        encounterTypes.add(Context.getEncounterService().getEncounterTypeByUuid(jsonObject.toString()));
                    }
                    EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(syncFHIRCase.getPatient(), null, encounterLastUpdated.getLowerBoundAsInstant(), encounterLastUpdated.getUpperBoundAsInstant(), null, null, encounterTypes, null, null, null, false);
                    encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
                    resources.addAll(groupInCaseBundle("Encounter", getEncounterResourceBundle(encounters), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "Observation":
                    if (encounters.size() > 0) {
                        resources.addAll(groupInCaseBundle("Observation", getObservationResourceBundle(syncFhirProfile, encounters, getPersonsFromEncounterList(encounters)), syncFhirProfile.getPatientIdentifierType().getName()));
                    }
                    break;
                case "ServiceRequest":

                    List<Order> testOrders = orderService.getActiveOrders(syncFHIRCase.getPatient(), orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID), null, null).stream().filter(testOrder -> testOrder.getDateActivated().compareTo(lastUpdateDate) >= 0).collect(Collectors.toList());
                    orderList = testOrders;
                    resources.addAll(groupInCaseBundle("ServiceRequest", getServiceRequestResourceBundle(testOrders), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "Condition":
                    resources.addAll(groupInCaseBundle("Condition", getConditionResourceBundle(syncFHIRCase, syncFhirProfile), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "AllergyIntolerance":
                    resources.addAll(groupInCaseBundle("AllergyIntolerance", getAllergyResourceBundle(syncFHIRCase, syncFhirProfile), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "Immunization":
                    resources.addAll(groupInCaseBundle("Immunization", getImmunizationResourceBundle(syncFHIRCase, syncFhirProfile), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "MedicationDispense":
                    resources.addAll(groupInCaseBundle("MedicationDispense", getMedicationDispenseResourceBundle(syncFHIRCase, syncFhirProfile), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "MedicationRequest":
                    resources.addAll(groupInCaseBundle("MedicationRequest", getMedicationRequestResourceBundle(syncFHIRCase, syncFhirProfile), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;

                case "DiagnosticReport":
                    resources.addAll(groupInCaseBundle("DiagnosticReport", getDiagnosticReportResourceBundle(syncFHIRCase, syncFhirProfile), syncFhirProfile.getPatientIdentifierType().getName()));
                    break;
                case "Practitioner":
                    List<Provider> providerList = new ArrayList<>();
                    for (Order order : orderList) {
                        providerList.add(order.getOrderer());
                    }

                    for (Encounter encounter : encounters) {
                        Provider provider = getProviderFromEncounter(encounter);
                        if (provider != null) {
                            providerList.add(provider);
                        }
                    }

                    if (providerList.size() > 0) {
                        resources.addAll(groupInCaseBundle("Practitioner", getPractitionerResourceBundle(syncFhirProfile, encounters, orderList), syncFhirProfile.getPatientIdentifierType().getName()));
                    }
                    break;
            }
        }

        String finalCaseBundle = null;

        if (resources.size() > 0) {
            finalCaseBundle = String.format(FHIR_BUNDLE_CASE_RESOURCE_TRANSACTION, resources.toString());
        }


        return finalCaseBundle;
    }


    public Collection<String> generateFHIRResourceBundles(SyncFhirProfile syncFhirProfile) {
        Collection<String> stringCollection = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();

        this.syncFhirProfile = syncFhirProfile;

        Date currentDate = new Date();

        String[] resourceTypes = syncFhirProfile.getResourceTypes().split(",");
        for (String resource : resourceTypes) {
            switch (resource) {
                case "Encounter":
                    List<EncounterType> encounterTypes = new ArrayList<>();
                    DateRangeParam encounterLastUpdated = new DateRangeParam().setUpperBoundInclusive(currentDate).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "Encounter"));
                    JSONArray jsonArray = getSearchParametersInJsonObject("Encounter", syncFhirProfile.getResourceSearchParameter()).getJSONArray("type");

                    for (Object jsonObject : jsonArray) {
                        encounterTypes.add(Context.getEncounterService().getEncounterTypeByUuid(jsonObject.toString()));
                    }

                    EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, encounterLastUpdated.getLowerBoundAsInstant(), encounterLastUpdated.getUpperBoundAsInstant(), null, null, encounterTypes, null, null, null, false);
                    encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);

                    saveSyncFHIRResources(groupInBundles("Encounter", getEncounterResourceBundle(encounters), syncFhirProfile.getNumberOfResourcesInBundle(), null), resource, syncFhirProfile, currentDate);

                    break;
                case "Observation":
                    if (encounters.size() > 0) {
                        saveSyncFHIRResources(groupInBundles("Observation", getObservationResourceBundle(syncFhirProfile, encounters, getPersonsFromEncounterList(encounters)), syncFhirProfile.getNumberOfResourcesInBundle(), null), "Observation", syncFhirProfile, currentDate);
                    } else {
                        saveSyncFHIRResources(groupInBundles("Observation", getObservationResourceBundle(syncFhirProfile, null, null), syncFhirProfile.getNumberOfResourcesInBundle(), null), "Observation", syncFhirProfile, currentDate);
                    }
                    break;
                case "Patient":
                    if (encounters.size() > 0) {
                        saveSyncFHIRResources(groupInBundles("Patient", getPatientResourceBundle(syncFhirProfile, getPatientIdentifierFromEncounter(encounters, syncFhirProfile.getPatientIdentifierType()), null), syncFhirProfile.getNumberOfResourcesInBundle(), syncFhirProfile.getPatientIdentifierType().getName()), "Patient", syncFhirProfile, currentDate);
                    } else {
                        saveSyncFHIRResources(groupInBundles("Patient", getPatientResourceBundle(syncFhirProfile, null, null), syncFhirProfile.getNumberOfResourcesInBundle(), syncFhirProfile.getPatientIdentifierType().getName()), "Patient", syncFhirProfile, currentDate);
                    }
                    break;
                case "Practitioner":
                    if (encounters.size() > 0) {
                        saveSyncFHIRResources(groupInBundles("Practitioner", getPractitionerResourceBundle(syncFhirProfile, encounters, null), syncFhirProfile.getNumberOfResourcesInBundle(), null), "Practitioner", syncFhirProfile, currentDate);
                    } else {
                        saveSyncFHIRResources(groupInBundles("Practitioner", getPractitionerResourceBundle(syncFhirProfile, null, null), syncFhirProfile.getNumberOfResourcesInBundle(), null), "Practitioner", syncFhirProfile, currentDate);
                    }
                    break;
                case "Person":
                    if (encounters.size() > 0) {
                        saveSyncFHIRResources(groupInBundles("Person", getPersonResourceBundle(syncFhirProfile, getPersonsFromEncounterList(encounters), null), syncFhirProfile.getNumberOfResourcesInBundle(), null), "Person", syncFhirProfile, currentDate);
                    } else {
                        saveSyncFHIRResources(groupInBundles("Person", getPersonResourceBundle(syncFhirProfile, null, null), syncFhirProfile.getNumberOfResourcesInBundle(), null), "Person", syncFhirProfile, currentDate);
                    }
                    break;
            }
        }

        return stringCollection;
    }

    public List<SyncFhirResource> saveSyncFHIRResources(@NotNull Collection<String> resources, @NotNull String resourceType, @NotNull SyncFhirProfile syncFhirProfile, Date currentDate) {
        List<SyncFhirResource> syncFhirResources = new ArrayList<>();
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);

        for (String resource : resources) {
            SyncFhirResource syncFHIRResource = new SyncFhirResource();
            syncFHIRResource.setGeneratorProfile(syncFhirProfile);
            syncFHIRResource.setResource(resource);
            syncFHIRResource.setSynced(false);
            syncFhirResources.add(ugandaEMRSyncService.saveFHIRResource(syncFHIRResource));
        }


        if (syncFhirResources.size() > 0) {
            SyncFhirProfileLog syncFhirProfileLog = new SyncFhirProfileLog();
            syncFhirProfileLog.setNumberOfResources(syncFhirResources.size());
            syncFhirProfileLog.setProfile(syncFhirProfile);
            syncFhirProfileLog.setResourceType(resourceType);
            syncFhirProfileLog.setLastGenerationDate(currentDate);
            ugandaEMRSyncService.saveSyncFhirProfileLog(syncFhirProfileLog);
        }


        return syncFhirResources;
    }


    private List<PatientIdentifier> getPatientIdentifierFromEncounter(List<Encounter> encounters, PatientIdentifierType patientIdentifierType) {
        List<PatientIdentifier> patientIdentifiers = new ArrayList<>();
        for (Encounter encounter : encounters) {
            PatientIdentifier patientIdentifier = encounter.getPatient().getPatientIdentifier(patientIdentifierType);
            if (patientIdentifier != null) {
                patientIdentifiers.add(patientIdentifier);
            }
        }
        return patientIdentifiers;
    }

    private List<Person> getPersonFromEncounter(List<Encounter> encounters) {
        List<Person> personList = new ArrayList<>();
        for (Encounter encounter : encounters) {
            personList.add(encounter.getPatient().getPerson());
        }
        return personList;
    }

    private Collection<String> groupInBundles(String resourceType, Collection<IBaseResource> iBaseResources, Integer interval, String identifierTypeName) {
        List<String> resourceBundles = new ArrayList<>();
        List<String> currentBundleList = new ArrayList<>();

        for (IBaseResource iBaseResource : iBaseResources) {
            String jsonString = encodeResourceToString(resourceType, identifierTypeName, iBaseResource);

            if (currentBundleList.size() < interval) {
                currentBundleList.add(jsonString);
            } else {
                if (!currentBundleList.toString().equals("")) {
                    resourceBundles.add(String.format(FHIR_BUNDLE_RESOURCE_TRANSACTION, currentBundleList.toString()));
                }
                currentBundleList = new ArrayList<>();
                currentBundleList.add(jsonString);
            }
        }

        if (iBaseResources.size() > 0 && currentBundleList.size() < interval) {
            resourceBundles.add(String.format(FHIR_BUNDLE_RESOURCE_TRANSACTION, currentBundleList.toString()));
        }

        return resourceBundles;
    }

    public Collection<String> groupInCaseBundle(String resourceType, Collection<IBaseResource> iBaseResources, String identifierTypeName) {
        // Delegate to FhirResourceGenerator
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
        }
        return resourceGenerator.groupInCaseBundle(resourceType, iBaseResources, identifierTypeName);
    }

    private String encodeResourceToString(String resourceType, String identifierTypeName, IBaseResource iBaseResource) {
        // Delegate to FhirResourceTransformer for consistent JSON transformations
        return resourceTransformer.encodeResourceToString(resourceType, identifierTypeName, iBaseResource);
    }

    private String handlePatientResource(String jsonString) {
        jsonString = correctEstimatedDOB(jsonString);
        if (profile != null && profile.getKeepProfileIdentifierOnly()!=null && profile.getKeepProfileIdentifierOnly()) {
            try {
                jsonString = removeIdentifierExceptProfileId(jsonString, "identifier");
                jsonString = addCodingToIdentifier(jsonString, "identifier");
            } catch (Exception e) {
                log.error("Error processing patient identifiers: ", e);
            }
        }

        jsonString = commonPersonPractitionerTransformations(jsonString);
        return wrapResourceWithId(jsonString, "Patient");
    }

    private String handlePractitionerResource(String jsonString) {
        jsonString = commonPersonPractitionerTransformations(jsonString);
        return wrapResourceWithId(jsonString, "Practitioner");
    }

    private String commonPersonPractitionerTransformations(String jsonString) {
        jsonString = addAttributeToObject(jsonString, "telecom", "system", "phone");
        jsonString = addOrganizationToRecord(jsonString, "managingOrganization");
        jsonString = addUseOfficialToName(jsonString, "name");
        jsonString = removeAttribute(jsonString, "contained");
        jsonString = jsonString.replace("address5", "village")
                .replace("address4", "parish")
                .replace("address3", "subcounty")
                .replace("state", "city");
        return jsonString;
    }

    private String handleEncounterResource(String jsonString) {
        jsonString = addOrganizationToRecord(jsonString, "serviceProvider");
        jsonString = addServiceType(jsonString, "serviceType");

        if (anyOtherObject.get("episodeOfCare") != null) {
            jsonString = addEpisodeOfCareToEncounter(jsonString, anyOtherObject.get("episodeOfCare"));
        }

        return wrapResourceInPostRequest(jsonString);
    }

    private String wrapResourceWithId(String jsonString, String resourceType) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String id = jsonObject.getString("id");
            return wrapResourceInPUTRequest(jsonString, resourceType, id);
        } catch (Exception e) {
            log.error("Error wrapping resource with ID: ", e);
            return wrapResourceInPostRequest(jsonString);
        }
    }

    private String addUseOfficialToName(String payload, String attributeName) {
        JSONObject jsonObject = new JSONObject(payload);
        int objectCount = 0;
        for (Object jsonObject1 : jsonObject.getJSONArray(attributeName)) {
            jsonObject.getJSONArray(attributeName).getJSONObject(objectCount).put("use", "official");
            objectCount++;
        }
        return jsonObject.toString();
    }

    private String removeIdentifierExceptProfileId(String payload, String attributeName) {
        if (profile != null) {
            JSONObject jsonObject = new JSONObject(payload);
            int objectCount = 0;
            for (int i = 0; i < jsonObject.getJSONArray(attributeName).length(); i++) {
                if (!jsonObject.getJSONArray("identifier").getJSONObject(i).getJSONObject("type").getJSONArray("coding").getJSONObject(0).get("code").toString().equals(profile.getPatientIdentifierType().getUuid())) {
                    jsonObject.getJSONArray("identifier").remove(i);
                }
            }

            return jsonObject.toString();
        } else {
            return payload;
        }
    }

    private String removeAttribute(String payload, String attributeName) {
        JSONObject jsonObject = new JSONObject(payload);
        if (jsonObject.has(attributeName)) {
            jsonObject.remove(attributeName);
        }
        return jsonObject.toString();
    }

    public String addCodingToIdentifier(String payload, String attributeName) {
        JSONObject jsonObject = new JSONObject(payload);
        int identifierCount = 0;
        if (jsonObject.has(attributeName)) {
            for (Object jsonObject1 : jsonObject.getJSONArray(attributeName)) {
                JSONObject jsonObject2 = new JSONObject(jsonObject1.toString());
                PatientIdentifier patientIdentifier = Context.getPatientService().getPatientIdentifierByUuid(jsonObject2.get("id").toString());
                jsonObject.getJSONArray(attributeName).getJSONObject(identifierCount).getJSONObject("type").put("coding", new JSONArray().put(new JSONObject().put("system", "UgandaEMR").put("code", patientIdentifier.getIdentifierType().getUuid())));
                identifierCount++;
            }
        }
        return jsonObject.toString();
    }

    public String correctEstimatedDOB(String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);

            if (!jsonObject.has("id") || jsonObject.isNull("id")) {
                return payload;
            }

            String patientUuid = jsonObject.getString("id");
            Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);

            if (patient != null && Boolean.TRUE.equals(patient.getBirthdateEstimated()) && patient.getBirthdate() != null) {
                String formattedBirthdate = new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthdate());
                jsonObject.put("birthDate", formattedBirthdate);
            }

            return jsonObject.toString();
        } catch (Exception e) {
            log.error("Failed to correct estimated DOB", e);
            return payload;
        }
    }

    private String addAttributeToObject(String payload, String targetObject, String attributeName, String attributeValue) {
        JSONObject jsonObject = new JSONObject(payload);
        if (jsonObject.has(targetObject) && jsonObject.getJSONArray(targetObject).length() > 0) {
            for (int i = 0; i < jsonObject.getJSONArray(targetObject).length(); i++) {
                jsonObject.getJSONArray(targetObject).getJSONObject(i).put(attributeName, attributeValue);
                i++;
            }
        }
        return jsonObject.toString();
    }

    private String getIdentifierSystemURL(String propertyName) {
        return Context.getAdministrationService().getGlobalProperty(propertyName);
    }

    public String wrapResourceInPostRequest(String payload) {
        // Delegate to FhirResourceTransformer
        return resourceTransformer.wrapResourceInPostRequest(payload);
    }

    public String wrapResourceInPUTRequest(String payload, String resourceType, String identifier) {
        // Delegate to FhirResourceTransformer
        return resourceTransformer.wrapResourceInPUTRequest(payload, resourceType, identifier);
    }


    private ApplicationContext getApplicationContext() {
        Field serviceContextField = null;
        ApplicationContext applicationContext = null;
        try {
            serviceContextField = Context.class.getDeclaredField("serviceContext");
        } catch (NoSuchFieldException e) {
            logger.error("Failed to get serviceContext field", e);
        }
        serviceContextField.setAccessible(true);

        try {
            applicationContext = ((ServiceContext) serviceContextField.get(null)).getApplicationContext();
        } catch (IllegalAccessException e) {
            logger.error("Failed to access serviceContext field", e);
        }
        return applicationContext;
    }

    private JSONObject getSearchParametersInJsonObject(String resourceType, String searchParameterString) {
        JSONObject jsonObject = new JSONObject(searchParameterString);
        if (jsonObject.isNull(resourceType)) {
            jsonObject = jsonObject.getJSONObject(resourceType.toLowerCase() + "Filter");
        }
        return jsonObject;
    }

    public String addSearchParameter(String resourceType, String searchParam, String searchParamString) {


        return searchParamString;
    }

    private Provider getProviderFromEncounter(Encounter encounter) {
        EncounterRole encounterRole = Context.getEncounterService().getEncounterRoleByUuid(ENCOUNTER_ROLE);

        Set<Provider> providers = encounter.getProvidersByRole(encounterRole);
        List<Provider> providerList = new ArrayList<>();
        for (Provider provider : providers) {
            providerList.add(provider);
        }

        if (!providerList.isEmpty()) {
            return providerList.get(0);
        } else {
            return null;
        }
    }

    private List<Person> getPersonsFromEncounterList(List<Encounter> encounters) {
        EncounterRole encounterClinicianRole = Context.getEncounterService().getEncounterRoleByUuid(ENCOUNTER_ROLE);
        List<Person> person = new ArrayList<>();

        for (Encounter encounter : encounters) {
            person.add(encounter.getPatient().getPerson());
            for (Provider provider : encounter.getProvidersByRole(encounterClinicianRole)) {
                person.add(provider.getPerson());
            }
        }
        return person;
    }


    public Collection<IBaseResource> getPatientResourceBundle(SyncFhirProfile syncFhirProfile, List<PatientIdentifier> patientIdentifiers, SyncFhirCase syncFhirCase) {
        // Delegate to FhirResourceGenerator for consistent resource generation
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
            resourceGenerator.setSyncFhirCase(syncFhirCase);
        } else {
            resourceGenerator.setSyncFhirCase(syncFhirCase);
        }

        Collection<IBaseResource> resources = resourceGenerator.getPatientResourceBundle(syncFhirProfile, patientIdentifiers, syncFhirCase);

        // Track generation success/failure (optional - only if tracking service is available)
        try {
            UgandaEMRSyncService syncService = Context.getService(UgandaEMRSyncService.class);
            if (resources.isEmpty()) {
                caseTrackingService.recordNullOrEmptyResourceFailure(syncFhirCase, syncService, "Patient", syncFhirCase != null ? syncFhirCase.getCaseIdentifier() : "unknown");
            } else {
                // Create a placeholder resource for tracking
                SyncFhirResource trackingResource = new SyncFhirResource();
                caseTrackingService.recordSuccessfulGeneration(syncFhirCase, trackingResource, syncService);
            }
        } catch (Exception e) {
            // Error tracking is optional - don't let it break the main functionality
            log.debug("Error tracking not available: " + e.getMessage());
        }

        return resources;
    }

    public Collection<IBaseResource> getPractitionerResourceBundle(SyncFhirProfile syncFhirProfile, List<Encounter> encounterList, List<Order> orders) {
        // Delegate to FhirResourceGenerator
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
        }
        return resourceGenerator.getPractitionerResourceBundle(syncFhirProfile, encounterList, orders);
    }

    public Collection<IBaseResource> getPersonResourceBundle(SyncFhirProfile syncFhirProfile, List<Person> personList, SyncFhirCase syncFhirCase) {
        // Delegate to FhirResourceGenerator
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
            resourceGenerator.setSyncFhirCase(syncFhirCase);
        } else {
            resourceGenerator.setSyncFhirCase(syncFhirCase);
        }
        return resourceGenerator.getPersonResourceBundle(syncFhirProfile, personList, syncFhirCase);
    }


    public Collection<IBaseResource> getEncounterResourceBundle(List<Encounter> encounters) {
        // Delegate to FhirResourceGenerator
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
        }
        return resourceGenerator.getEncounterResourceBundle(encounters);
    }

    public Collection<IBaseResource> getObservationResourceBundle(SyncFhirProfile syncFhirProfile, List<Encounter> encounterList, List<Person> personList) {
        // Delegate to FhirResourceGenerator
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
            resourceGenerator.setSyncFhirCase(syncFhirCase);
        } else {
            resourceGenerator.setSyncFhirCase(syncFhirCase);
        }
        return resourceGenerator.getObservationResourceBundle(syncFhirProfile, encounterList, personList);
    }


    public Collection<IBaseResource> getServiceRequestResourceBundle(List<Order> testOrders) {
        // Delegate to FhirResourceGenerator
        if (resourceGenerator == null) {
            resourceGenerator = new FhirResourceGenerator(healthCenterIdentifier, healthCenterName, syncFhirProfile);
        }
        return resourceGenerator.getServiceRequestResourceBundle(testOrders);
    }

    private Collection<IBaseResource> getConditionResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        JSONArray codes = new JSONArray();
        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        TokenAndListParam codeReference = new TokenAndListParam();
        ConditionSearchParams conditionSearchParams = new ConditionSearchParams();

        DateRangeParam lastUpdated = null;
        if (syncFhirProfile != null) {
            JSONObject searchParams = getSearchParametersInJsonObject("Condition", syncFhirProfile.getResourceSearchParameter());
            codes = searchParams.getJSONArray("code");

            if (syncFhirProfile != null && syncFhirProfile.getIsCaseBasedProfile()) {


                if (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null) {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(syncFhirCase.getLastUpdateDate());
                } else {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getDefaultLastSyncDate());
                }
            } else {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "Condition"));

            }

        }

        for (Object conceptUID : codes) {
            try {
                TokenParam paramConcept = new TokenParam(conceptUID.toString());
                codeReference.addAnd(paramConcept);
            } catch (Exception e) {
                log.error("Error while adding concept with uuid " + conceptUID, e);
            }

        }

        ReferenceAndListParam patientReference = new ReferenceAndListParam();
        if (syncFhirCase != null) {
            patientReference.addValue(new ReferenceOrListParam().add(new ReferenceParam(SP_IDENTIFIER, syncFhirCase.getPatient().getPatientIdentifier().getIdentifier())));
        }

        conditionSearchParams.setCode(codeReference);
        conditionSearchParams.setLastUpdated(lastUpdated);
        conditionSearchParams.setPatientParam(patientReference);

        iBaseResources.addAll(getApplicationContext().getBean(FhirConditionService.class).searchConditions(conditionSearchParams).getResources(0, Integer.MAX_VALUE));

        return iBaseResources;
    }


    private Collection<IBaseResource> getAllergyResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        FhirAllergyIntoleranceSearchParams fhirAllergyIntoleranceSearchParams = new FhirAllergyIntoleranceSearchParams();

        DateRangeParam lastUpdated;

        if (syncFhirProfile != null && syncFhirProfile.getIsCaseBasedProfile()) {
            if (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null) {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(syncFhirCase.getLastUpdateDate());
            } else {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getDefaultLastSyncDate());
            }
        } else {
            lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "AllergyIntolerance"));

        }

        ReferenceAndListParam patientReference = new ReferenceAndListParam();
        if (syncFhirCase != null) {
            patientReference.addValue(new ReferenceOrListParam().add(new ReferenceParam(SP_IDENTIFIER, syncFhirCase.getPatient().getPatientIdentifier().getIdentifier())));
        }

        fhirAllergyIntoleranceSearchParams.setLastUpdated(lastUpdated);
        fhirAllergyIntoleranceSearchParams.setPatientReference(patientReference);

        iBaseResources.addAll(getApplicationContext().getBean(FhirAllergyIntoleranceService.class).searchForAllergies(fhirAllergyIntoleranceSearchParams).getResources(0, Integer.MAX_VALUE));

        return iBaseResources;
    }

    private Collection<IBaseResource> getImmunizationResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        ReferenceAndListParam param = new ReferenceAndListParam();
        if (syncFhirCase != null) {
            param.addValue(new ReferenceOrListParam().add(new ReferenceParam(SP_IDENTIFIER, syncFhirCase.getPatient().getPatientIdentifier().getIdentifier())));
        }

        iBaseResources.addAll(getApplicationContext().getBean(FhirImmunizationService.class).searchImmunizations(param, null).getResources(0, Integer.MAX_VALUE));

        return iBaseResources;
    }


    private Collection<IBaseResource> getMedicationDispenseResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {

        JSONObject searchParams = getSearchParametersInJsonObject("medicationDispense", syncFhirProfile.getResourceSearchParameter());

        JSONArray codes = searchParams.getJSONArray("code");
        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        MedicationDispenseSearchParams medicationDispenseSearchParams = new MedicationDispenseSearchParams();
        DateRangeParam lastUpdated;

        if (syncFhirProfile != null && syncFhirProfile.getIsCaseBasedProfile()) {
            if (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null) {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(syncFhirCase.getLastUpdateDate());
            } else {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getDefaultLastSyncDate());
            }
        } else {
            lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "MedicationRequest"));

        }

        ReferenceAndListParam patientReference = new ReferenceAndListParam();
        if (syncFhirCase != null) {
            patientReference.addValue(new ReferenceOrListParam().add(new ReferenceParam(SP_IDENTIFIER, syncFhirCase.getPatient().getPatientIdentifier().getIdentifier())));
        }


        medicationDispenseSearchParams.setLastUpdated(lastUpdated);
        medicationDispenseSearchParams.setPatient(patientReference);

        iBaseResources.addAll(getApplicationContext().getBean(FhirMedicationDispenseService.class).searchMedicationDispenses(medicationDispenseSearchParams).getResources(0, Integer.MAX_VALUE));

        return iBaseResources;
    }

    private Collection<IBaseResource> getMedicationRequestResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {

        JSONObject searchParams = new JSONObject();


        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        MedicationRequestSearchParams medicationRequestSearchParams = new MedicationRequestSearchParams();
        TokenAndListParam codeReference = new TokenAndListParam();

        DateRangeParam lastUpdated = null;

        if (syncFhirProfile != null) {
            getSearchParametersInJsonObject("medicationRequest", syncFhirProfile.getResourceSearchParameter());
            if (syncFhirProfile.getIsCaseBasedProfile()) {
                if (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null) {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(syncFhirCase.getLastUpdateDate());
                } else {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getDefaultLastSyncDate());
                }
            } else {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "MedicationRequest"));

            }

        }
        if (!searchParams.isEmpty() && searchParams.has("code")) {
            JSONArray codes = searchParams.getJSONArray("code");
            for (Object conceptUID : codes) {
                try {
                    TokenParam paramConcept = new TokenParam(conceptUID.toString());
                    codeReference.addAnd(paramConcept);
                } catch (Exception e) {
                    log.error("Error while adding concept with uuid " + conceptUID, e);
                }

            }
        }

        ReferenceAndListParam patientReference = new ReferenceAndListParam();
        if (syncFhirCase != null) {
            patientReference.addValue(new ReferenceOrListParam().add(new ReferenceParam(SP_IDENTIFIER, syncFhirCase.getPatient().getPatientIdentifier().getIdentifier())));
        }


        medicationRequestSearchParams.setLastUpdated(lastUpdated);
        medicationRequestSearchParams.setCode(codeReference);
        medicationRequestSearchParams.setPatientReference(patientReference);

        iBaseResources.addAll(getApplicationContext().getBean(FhirMedicationRequestService.class).searchForMedicationRequests(medicationRequestSearchParams).getResources(0, Integer.MAX_VALUE));

        return iBaseResources;
    }

    private Collection<IBaseResource> getDiagnosticReportResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {

        JSONObject searchParams = new JSONObject();


        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        DiagnosticReportSearchParams diagnosticReportSearchParams = new DiagnosticReportSearchParams();
        TokenAndListParam codeReference = new TokenAndListParam();

        DateRangeParam lastUpdated = null;

        if (syncFhirProfile != null) {
            getSearchParametersInJsonObject("diagnosticReport", syncFhirProfile.getResourceSearchParameter());
            if (syncFhirProfile.getIsCaseBasedProfile()) {
                if (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null) {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(syncFhirCase.getLastUpdateDate());
                } else {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getDefaultLastSyncDate());
                }
            } else {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "MedicationRequest"));

            }
        }

        if (!searchParams.isEmpty() && searchParams.has("code")) {
            JSONArray codes = searchParams.getJSONArray("code");
            for (Object conceptUID : codes) {
                try {
                    TokenParam paramConcept = new TokenParam(conceptUID.toString());
                    codeReference.addAnd(paramConcept);
                } catch (Exception e) {
                    log.error("Error while adding concept with uuid " + conceptUID, e);
                }

            }
        }

        ReferenceAndListParam patientReference = new ReferenceAndListParam();
        if (syncFhirCase != null) {
            patientReference.addValue(new ReferenceOrListParam().add(new ReferenceParam(SP_IDENTIFIER, syncFhirCase.getPatient().getPatientIdentifier().getIdentifier())));
        }


        diagnosticReportSearchParams.setLastUpdated(lastUpdated);
        diagnosticReportSearchParams.setCode(codeReference);
        diagnosticReportSearchParams.setPatientReference(patientReference);

        iBaseResources.addAll(getApplicationContext().getBean(FhirDiagnosticReportService.class).searchForDiagnosticReports(diagnosticReportSearchParams).getResources(0, Integer.MAX_VALUE));

        return iBaseResources;
    }

    private Collection<IBaseResource> getEpisodeOfCareResourceBundle(List<PatientProgram> patientPrograms) {
        this.patientPrograms = patientPrograms;
        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        Collection<String> patientProgramUUIDs = patientPrograms.stream().map(PatientProgram::getUuid).collect(Collectors.toCollection(ArrayList::new));

        iBaseResources.addAll(getApplicationContext().getBean(FhirEpisodeOfCareService.class).get(patientProgramUUIDs));
        return iBaseResources;
    }

    public Collection<IBaseResource> getRelatedPerson(SyncFhirProfile syncFhirProfile, List<Person> personList, SyncFhirCase syncFhirCase) {

        DateRangeParam lastUpdated = new DateRangeParam();

        if (syncFhirProfile != null) {
            if (syncFhirProfile.getIsCaseBasedProfile()) {
                if (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null) {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(syncFhirCase.getLastUpdateDate());
                } else {
                    lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getDefaultLastSyncDate());
                }
            } else {
                lastUpdated = new DateRangeParam().setUpperBoundInclusive(new Date()).setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "RelatedPerson"));

            }
        }

        PersonSearchParams personSearchParams = new PersonSearchParams();

        Collection<IBaseResource> iBaseResources = new ArrayList<>();

        if (personList.size() > 0) {
            Collection<String> personListUUID = personList.stream().map(Person::getUuid).collect(Collectors.toCollection(ArrayList::new));
            iBaseResources.addAll(getApplicationContext().getBean(FhirRelatedPersonService.class).get(personListUUID));

        } else if (syncFhirProfile != null && !syncFhirProfile.getIsCaseBasedProfile()) {
            personSearchParams.setLastUpdated(lastUpdated);
        }
        return iBaseResources;
    }


    private Date getLastSyncDate(SyncFhirProfile syncFhirProfile, String resourceType) {
        Date date;

        SyncFhirProfileLog syncFhirProfileLog = Context.getService(UgandaEMRSyncService.class).getLatestSyncFhirProfileLogByProfileAndResourceName(syncFhirProfile, resourceType);

        if (syncFhirProfileLog != null) {
            date = syncFhirProfileLog.getLastGenerationDate();
        } else if (syncFhirProfile.getDataToSyncStartDate() != null) {
            date = syncFhirProfile.getDataToSyncStartDate();
        } else {
            date = getDefaultLastSyncDate();
        }
        return date;
    }

    private Date getDefaultLastSyncDate() {
        try {
            return new SimpleDateFormat("yyyy/MM/dd").parse("1989/01/01");
        } catch (ParseException e) {
            log.error(e);
        }
        return null;
    }


    private PatientIdentifier getPatientIdentifierByPatientAndType(Patient patient, PatientIdentifierType patientIdentifierType) {
        List<PatientIdentifierType> patientIdentifierTypes = new ArrayList<>();
        List<Patient> patients = new ArrayList<>();
        patientIdentifierTypes.add(patientIdentifierType);
        patients.add(patient);
        Context.getPatientService().getPatientIdentifiers(null, patientIdentifierTypes, null, patients, false);
        return null;
    }

    public List<Map> sendFhirResourcesTo(SyncFhirProfile syncFhirProfile) {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();
        SyncErrorTrackingService errorTrackingService = new SyncErrorTrackingService();
        List<Map> maps = new ArrayList<>();
        List<SyncFhirResource> syncFhirResources = ugandaEMRSyncService.getUnSyncedFHirResources(syncFhirProfile);

        for (SyncFhirResource syncFhirResource : syncFhirResources) {
            Date date = new Date();
            try {
                boolean connectionStatus = ugandaEMRHttpURLConnection.isConnectionAvailable();

                if (connectionStatus) {
                    Map map = ugandaEMRHttpURLConnection.sendPostBy(syncFhirProfile.getUrl(), syncFhirProfile.getUrlUserName(), syncFhirProfile.getUrlPassword(), syncFhirProfile.getUrlToken(), syncFhirResource.getResource(), false);
                    if (map.get("responseCode").equals(SyncConstant.CONNECTION_SUCCESS_200) || map.get("responseCode").equals(SyncConstant.CONNECTION_SUCCESS_201)) {
                        maps.add(map);
                        syncFhirResource.setDateSynced(date);
                        syncFhirResource.setSynced(true);
                        syncFhirResource.setResource(null);
                        syncFhirResource.setStatusCode(Integer.parseInt(map.get("responseCode").toString()));
                        syncFhirResource.setStatusCodeDetail(map.get("responseMessage").toString());
                        syncFhirResource.setExpiryDate(UgandaEMRSyncUtil.addDaysToDate(date, syncFhirProfile.getDurationToKeepSyncedResources()));

                        // Track successful sync
                        errorTrackingService.recordSuccessfulSync(syncFhirResource, ugandaEMRSyncService);

                        if (syncFhirProfile.getUuid().equals(FSHR_SYNC_FHIR_PROFILE_UUID) || syncFhirProfile.getUuid().equals(CROSS_BORDER_CR_SYNC_FHIR_PROFILE_UUID)) {
                            ugandaEMRSyncService.updatePatientsFromFHIR((String) map.get("result"), PATIENT_ID_TYPE_CROSS_BORDER_UUID, PATIENT_ID_TYPE_CROSS_BORDER_NAME);
                        }
                    } else {
                        // Track HTTP sync failure with detailed error information
                        int statusCode = Integer.parseInt(map.get("responseCode").toString());
                        String statusMessage = map.get("responseMessage").toString();
                        errorTrackingService.recordHttpSyncResult(syncFhirResource, ugandaEMRSyncService,
                                statusCode, statusMessage, false);
                    }
                } else {
                    // Track connection failure
                    errorTrackingService.recordFailedSync(syncFhirResource, ugandaEMRSyncService,
                            SyncErrorType.HTTP_CONNECTION_FAILED, "Internet connection unavailable. Code: " + connectionStatus, null);
                }

            } catch (Exception e) {
                // Track unexpected sync failure
                errorTrackingService.recordFailedSync(syncFhirResource, ugandaEMRSyncService,
                        SyncErrorType.UNKNOWN_ERROR, "Unexpected error during HTTP sync: " + e.getMessage(), e);
            }

        }

        return maps;
    }


    private String addReferencesMappingToObservation(String observation) {
        ConceptService conceptService = Context.getConceptService();
        JSONObject jsonObject = new JSONObject(observation);
        JSONObject observationResource = jsonObject.getJSONObject("resource");
        String conceptUUid = observationResource.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code");


        Concept concept = conceptService.getConceptByUuid(conceptUUid);

        JSONArray newQuestionJson = observationResource.getJSONObject("code").getJSONArray("coding");

        newQuestionJson.put(new JSONObject(String.format(FHIR_CODING_DATATYPE, "UgandaEMR", concept.getConceptId(), concept.getName().getName())));

        if (concept.getConceptMappings().size() > 0) {
            for (ConceptMap conceptQuestionMap : concept.getConceptMappings()) {
                newQuestionJson.put(new JSONObject(String.format(FHIR_CODING_DATATYPE, conceptQuestionMap.getConceptReferenceTerm().getConceptSource().getName(), conceptQuestionMap.getConceptReferenceTerm().getCode(), conceptQuestionMap.getConceptReferenceTerm().getName())));
            }
        }

        if (concept.getDatatype().equals(conceptService.getConceptDatatypeByUuid("8d4a48b6-c2cc-11de-8d13-0010c6dffd0f")) && !observationResource.isNull("valueCodeableConcept")) {
            JSONArray newValueCodeableJson = observationResource.getJSONObject("valueCodeableConcept").getJSONArray("coding");
            String valueCodedConceptUUid = observationResource.getJSONObject("valueCodeableConcept").getJSONArray("coding").getJSONObject(0).getString("code");
            Concept valueCodedConcept = conceptService.getConceptByUuid(valueCodedConceptUUid);
            newValueCodeableJson.put(new JSONObject(String.format(FHIR_CODING_DATATYPE, "UgandaEMR", valueCodedConcept.getConceptId(), valueCodedConcept.getName().getName())));
            for (ConceptMap conceptMap : valueCodedConcept.getConceptMappings()) {
                newValueCodeableJson.put(new JSONObject(String.format(FHIR_CODING_DATATYPE, conceptMap.getConceptReferenceTerm().getConceptSource().getName(), conceptMap.getConceptReferenceTerm().getCode(), conceptMap.getConceptReferenceTerm().getName())));
            }
        }

        return jsonObject.toString();
    }

    private String addEpisodeOfCareToEncounter(String encounter, Object episodeOfcare) {
        String episodeOfCareReference = "{\"reference\":\"EpisodeOfCare/%s\"}";

        JSONObject jsonObject = new JSONObject(encounter);

        Date encounterDate = null;
        try {
            encounterDate = new SimpleDateFormat("yyyy-MM-dd").parse(jsonObject.getJSONObject("period").getString("start"));
        } catch (ParseException e) {
            log.error(e);
        }

        List<PatientProgram> patientPrograms = (List<PatientProgram>) episodeOfcare;

        for (PatientProgram patientProgram : patientPrograms) {
            if ((encounterDate.equals(patientProgram.getDateEnrolled()) || encounterDate.after(patientProgram.getDateEnrolled())) && (patientProgram.getDateCompleted() == null || (patientProgram.getDateCompleted() != null && (encounterDate.equals(patientProgram.getDateCompleted()) || encounterDate.before(patientProgram.getDateCompleted()))))) {
                jsonObject.put("episodeOfCare", new JSONObject(String.format(episodeOfCareReference, patientProgram.getUuid())));
            }
        }


        return jsonObject.toString();
    }


    private PatientIdentifier getPatientIdentifierByType(Patient patient, PatientIdentifierType patientIdentifierType) {
        for (PatientIdentifier patientIdentifier : patient.getActiveIdentifiers()) {
            if (patientIdentifier.getIdentifierType().equals(patientIdentifierType)) {
                return patientIdentifier;
            }

        }
        return null;
    }

    public void CollectTestOrdersFromSyncFHIRResource(SyncFhirProfile syncFhirProfile) {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);

        List<SyncFhirResource> syncFhirResources = ugandaEMRSyncService.getSyncedFHirResources(syncFhirProfile);
        List<Order> orders = new ArrayList<>();
        SyncTaskType syncTaskType = ugandaEMRSyncService.getSyncTaskTypeByUUID("f947128e-93d7-46d5-aa32-645e38a125fe");
        for (SyncFhirResource syncFhirResource : syncFhirResources) {
            JSONObject jsonObject = new JSONObject(syncFhirResource.getResource());

            JSONArray jsonArray = jsonObject.getJSONArray("entry");

            for (Object o : jsonArray) {
                JSONObject jsonObject1 = new JSONObject(o.toString());

                if (jsonObject1.getJSONObject("resource").get("resourceType").equals("ServiceRequest")) {
                    Order order = Context.getOrderService().getOrderByUuid(jsonObject1.getJSONObject("resource").getString("id"));

                    if (!order.isActive() || !ugandaEMRSyncService.getSyncTaskBySyncTaskId(order.getOrderNumber()).equals(null)) {
                        continue;
                    }

                    SyncTask newSyncTask = new SyncTask();
                    newSyncTask.setDateSent(new Date());
                    newSyncTask.setCreator(Context.getUserService().getUser(1));
                    newSyncTask.setSentToUrl(syncTaskType.getUrl());
                    newSyncTask.setRequireAction(true);
                    newSyncTask.setActionCompleted(false);
                    newSyncTask.setSyncTask(order.getUuid());
                    newSyncTask.setStatusCode(200);
                    newSyncTask.setStatus("SUCCESS");
                    newSyncTask.setSyncTaskType(syncTaskType);
                    ugandaEMRSyncService.saveSyncTask(newSyncTask);
                }
            }


        }
    }

    private List<Patient> getPatientByCohortType(String cohortTypeUuid) {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        List<Integer> patientIds = ugandaEMRSyncService.getPatientsByCohortType(cohortTypeUuid);
        PatientService patientService = Context.getPatientService();
        List<Patient> patientList = new ArrayList<>();

        for (Integer patientId : patientIds) {
            Patient patient = patientService.getPatient(patientId);
            if (patient != null) {
                patientList.add(patient);
            }
        }
        return patientList;
    }


}
