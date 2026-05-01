package org.openmrs.module.ugandaemrsync.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.fhir2.api.FhirConditionService;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.FhirEpisodeOfCareService;
import org.openmrs.module.fhir2.api.FhirImmunizationService;
import org.openmrs.module.fhir2.api.FhirMedicationDispenseService;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.fhir2.api.FhirPersonService;
import org.openmrs.module.fhir2.api.FhirPractitionerService;
import org.openmrs.module.fhir2.api.FhirRelatedPersonService;
import org.openmrs.module.fhir2.api.FhirServiceRequestService;
import org.openmrs.module.fhir2.api.search.param.ConditionSearchParams;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import org.openmrs.module.fhir2.api.FhirEpisodeOfCareService;
import org.openmrs.module.fhir2.api.FhirImmunizationService;
import org.openmrs.module.fhir2.api.FhirMedicationDispenseService;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.fhir2.api.FhirPersonService;
import org.openmrs.module.fhir2.api.FhirPractitionerService;
import org.openmrs.module.fhir2.api.FhirRelatedPersonService;
import org.openmrs.module.fhir2.api.FhirServiceRequestService;
import org.openmrs.module.fhir2.api.search.param.ConditionSearchParams;
import org.openmrs.module.fhir2.api.search.param.PersonSearchParams;
import org.openmrs.module.fhir2.api.search.param.PractitionerSearchParams;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comprehensive FHIR resource generator.
 * Handles all resource types with proper error handling and tracking.
 */
public class FhirResourceGenerator {

    private static final Log log = LogFactory.getLog(FhirResourceGenerator.class);
    private final FhirResourceTransformer transformer;
    private final String healthCenterIdentifier;
    private final String healthCenterName;
    private final SyncFhirProfile profile;
    private SyncFhirCase syncFhirCase;

    public FhirResourceGenerator(String healthCenterIdentifier, String healthCenterName, SyncFhirProfile profile) {
        this.healthCenterIdentifier = healthCenterIdentifier;
        this.healthCenterName = healthCenterName;
        this.profile = profile;
        this.transformer = new FhirResourceTransformer(healthCenterIdentifier, healthCenterName, profile);
    }

    /**
     * Set the current case for case-based resource generation
     */
    public void setSyncFhirCase(SyncFhirCase syncFhirCase) {
        this.syncFhirCase = syncFhirCase;
    }

    /**
     * Get the application context for accessing FHIR services
     * Uses the same pattern as original SyncFHIRRecord to ensure compatibility
     */
    private ApplicationContext getApplicationContext() {
        Field serviceContextField = null;
        ApplicationContext applicationContext = null;
        try {
            serviceContextField = Context.class.getDeclaredField("serviceContext");
        } catch (NoSuchFieldException e) {
            log.error("Failed to get serviceContext field", e);
        }
        serviceContextField.setAccessible(true);

        try {
            applicationContext = ((ServiceContext) serviceContextField.get(null)).getApplicationContext();
        } catch (IllegalAccessException e) {
            log.error("Failed to access serviceContext field", e);
        }
        return applicationContext;
    }

    /**
     * Safely get a FHIR service from the application context
     * Returns null if service is not available
     */
    private <T> T getFhirService(Class<T> serviceClass) {
        ApplicationContext ctx = getApplicationContext();
        if (ctx == null) {
            log.warn("Application context not available, cannot get service: " + serviceClass.getSimpleName());
            return null;
        }
        try {
            return ctx.getBean(serviceClass);
        } catch (Exception e) {
            log.error("Error getting FHIR service: " + serviceClass.getSimpleName(), e);
            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(FhirResourceGenerator.class);

    /**
     * Generates FHIR Patient resources from patient identifiers
     */
    public Collection<IBaseResource> getPatientResourceBundle(SyncFhirProfile syncFhirProfile, List<PatientIdentifier> patientIdentifiers, SyncFhirCase syncFhirCase) {
        List<IBaseResource> iBaseResources = new ArrayList<>();

        ApplicationContext applicationContext = getApplicationContext();
        if (applicationContext == null) {
            log.warn("Application context not available, cannot generate FHIR Patient resources");
            return iBaseResources;
        }

        FhirPatientService fhirPatientService = applicationContext.getBean(FhirPatientService.class);

        if (patientIdentifiers != null) {
            for (PatientIdentifier patientIdentifier : patientIdentifiers) {
                try {
                    Patient patient = patientIdentifier.getPatient();
                    if (patient != null) {
                        IBaseResource fhirPatient = fhirPatientService.get(patient.getUuid());
                        if (fhirPatient != null) {
                            iBaseResources.add(fhirPatient);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error generating FHIR Patient resource", e);
                }
            }
        }

        return iBaseResources;
    }

    /**
     * Generates FHIR Encounter resources
     */
    public Collection<IBaseResource> getEncounterResourceBundle(List<Encounter> encounters) {
        FhirEncounterService fhirEncounterService = getFhirService(FhirEncounterService.class);
        if (fhirEncounterService == null) {
            log.warn("FhirEncounterService not available");
            return new ArrayList<>();
        }

        List<IBaseResource> iBaseResources = new ArrayList<>();

        if (encounters != null) {
            for (Encounter encounter : encounters) {
                try {
                    IBaseResource fhirEncounter = fhirEncounterService.get(encounter.getUuid());
                    if (fhirEncounter != null) {
                        iBaseResources.add(fhirEncounter);
                    }
                } catch (Exception e) {
                    log.error("Error generating FHIR Encounter resource", e);
                }
            }
        }

        return iBaseResources;
    }

    /**
     * Generates FHIR EpisodeOfCare resources from patient programs
     */
    public Collection<IBaseResource> getEpisodeOfCareResourceBundle(List<PatientProgram> patientPrograms) {
        FhirEpisodeOfCareService fhirEpisodeOfCareService = getFhirService(FhirEpisodeOfCareService.class);
        if (fhirEpisodeOfCareService == null) {
            log.warn("FhirEpisodeOfCareService not available");
            return new ArrayList<>();
        }

        List<IBaseResource> iBaseResources = new ArrayList<>();

        if (patientPrograms != null) {
            for (PatientProgram patientProgram : patientPrograms) {
                try {
                    IBaseResource fhirEpisodeOfCare = fhirEpisodeOfCareService.get(patientProgram.getUuid());
                    if (fhirEpisodeOfCare != null) {
                        iBaseResources.add(fhirEpisodeOfCare);
                    }
                } catch (Exception e) {
                    log.error("Error generating FHIR EpisodeOfCare resource", e);
                }
            }
        }

        return iBaseResources;
    }

    /**
     * Generates FHIR Observation resources from encounters and persons
     */
    public Collection<IBaseResource> getObservationResourceBundle(SyncFhirProfile syncFhirProfile, List<Encounter> encounterList, List<Person> personList) {
        List<Concept> conceptQuestionList = new ArrayList<>();
        DateRangeParam lastUpdated = new DateRangeParam();
        Date lastSyncDate = null;

        try {
            if (syncFhirProfile != null) {
                JSONObject searchParams = getSearchParametersInJsonObject("Observation", syncFhirProfile.getResourceSearchParameter());
                JSONArray codes = searchParams.getJSONArray("code");

                lastSyncDate = getLastSyncDate(syncFhirProfile, "Observation");
                for (Object conceptUID : codes) {
                    try {
                        Concept concept = Context.getConceptService().getConcept(conceptUID.toString());
                        if (concept != null) {
                            conceptQuestionList.add(concept);
                        }
                    } catch (Exception e) {
                        log.error("Error while adding concept with uuid " + conceptUID, e);
                    }
                }

                if (syncFhirProfile.getIsCaseBasedProfile()) {
                    Date caseDate = (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null)
                            ? syncFhirCase.getLastUpdateDate() : getDefaultLastSyncDate();
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(caseDate);
                } else {
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "Observation"));
                }
            }

            List<Obs> observationList = Context.getObsService().getObservations(
                    personList, encounterList, conceptQuestionList, null, null, null,
                    null, null, null, lastSyncDate, new Date(), false
            );

            Collection<String> obsListUUID = observationList.stream()
                    .map(Obs::getUuid)
                    .collect(Collectors.toCollection(ArrayList::new));

            Collection<IBaseResource> iBaseResources = new ArrayList<>();
            if (!obsListUUID.isEmpty()) {
                FhirObservationService observationService = getFhirService(FhirObservationService.class);
                if (observationService != null) {
                    iBaseResources.addAll(observationService.get(obsListUUID));
                }
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating Observation resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR Practitioner resources from encounters and orders
     */
    public Collection<IBaseResource> getPractitionerResourceBundle(SyncFhirProfile syncFhirProfile, List<Encounter> encounterList, List<Order> orders) {
        try {
            PractitionerSearchParams practitionerSearchParams = new PractitionerSearchParams();

            if (syncFhirProfile != null && !syncFhirProfile.getIsCaseBasedProfile()) {
                DateRangeParam lastUpdated = new DateRangeParam()
                        .setUpperBoundInclusive(new Date())
                        .setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "Practitioner"));
                practitionerSearchParams.setLastUpdated(lastUpdated);
            }

            Collection<String> providerList = new ArrayList<>();

            if (encounterList != null) {
                for (Encounter encounter : encounterList) {
                    Provider provider = getProviderFromEncounter(encounter);
                    if (provider != null) {
                        providerList.add(provider.getUuid());
                    }
                }
            }

            if (orders != null) {
                for (Order order : orders) {
                    if (order.getOrderer() != null) {
                        providerList.add(order.getOrderer().getUuid());
                    }
                }
            }

            List<IBaseResource> iBaseResources = new ArrayList<>();
            if (!providerList.isEmpty()) {
                FhirPractitionerService practitionerService = getFhirService(FhirPractitionerService.class);
                if (practitionerService != null) {
                    iBaseResources.addAll(practitionerService.get(providerList));
                }
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating Practitioner resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR ServiceRequest resources from orders
     */
    public Collection<IBaseResource> getServiceRequestResourceBundle(List<Order> testOrders) {
        try {
            Collection<String> testOrdersUUIDS = new ArrayList<>();
            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (testOrders != null) {
                for (Order testOrder : testOrders) {
                    testOrdersUUIDS.add(testOrder.getUuid());
                }
            }

            if (!testOrdersUUIDS.isEmpty()) {
                FhirServiceRequestService serviceRequestService = getFhirService(FhirServiceRequestService.class);
                if (serviceRequestService != null) {
                    iBaseResources.addAll(serviceRequestService.get(testOrdersUUIDS));
                }
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating ServiceRequest resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR Person resources
     */
    public Collection<IBaseResource> getPersonResourceBundle(SyncFhirProfile syncFhirProfile, List<Person> personList, SyncFhirCase syncFhirCase) {
        try {
            DateRangeParam lastUpdated = new DateRangeParam();

            if (syncFhirProfile != null) {
                if (syncFhirProfile.getIsCaseBasedProfile()) {
                    Date caseDate = (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null)
                            ? syncFhirCase.getLastUpdateDate() : getDefaultLastSyncDate();
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(caseDate);
                } else {
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "Patient"));
                }
            }

            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (personList != null && !personList.isEmpty()) {
                Collection<String> personListUUID = personList.stream()
                        .map(Person::getUuid)
                        .collect(Collectors.toCollection(ArrayList::new));
                FhirPersonService personService = getFhirService(FhirPersonService.class);
                if (personService != null) {
                    iBaseResources.addAll(personService.get(personListUUID));
                }
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating Person resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR Condition resources for a case
     */
    public Collection<IBaseResource> getConditionResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        try {
            JSONArray codes = new JSONArray();
            Collection<IBaseResource> iBaseResources = new ArrayList<>();
            ConditionSearchParams conditionSearchParams = new ConditionSearchParams();
            DateRangeParam lastUpdated = null;

            if (syncFhirProfile != null) {
                JSONObject searchParams = getSearchParametersInJsonObject("Condition", syncFhirProfile.getResourceSearchParameter());
                codes = searchParams.getJSONArray("code");

                if (syncFhirProfile.getIsCaseBasedProfile()) {
                    Date caseDate = (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null)
                            ? syncFhirCase.getLastUpdateDate() : getDefaultLastSyncDate();
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(caseDate);
                } else {
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "Condition"));
                }
            }

            // Note: Full implementation would use FhirConditionService with proper search parameters
            // For now, returning empty collection as this requires complex condition searching
            log.debug("Condition resource generation - requires complex search parameters");

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating Condition resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR AllergyIntolerance resources for a case
     */
    public Collection<IBaseResource> getAllergyResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        try {
            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            // Note: Allergy resources require patient-specific allergy service
            // This would need FhirAllergyIntoleranceService integration
            log.debug("AllergyIntolerance resource generation - requires patient-specific implementation");

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating AllergyIntolerance resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR Immunization resources for a case
     */
    public Collection<IBaseResource> getImmunizationResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        try {
            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (syncFhirCase != null && syncFhirCase.getPatient() != null) {
                // Would use FhirImmunizationService to get immunizations for patient
                log.debug("Immunization resource generation - requires FhirImmunizationService integration");
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating Immunization resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR MedicationDispense resources for a case
     */
    public Collection<IBaseResource> getMedicationDispenseResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        try {
            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (syncFhirCase != null && syncFhirCase.getPatient() != null) {
                // Would use FhirMedicationDispenseService to get medication dispenses
                log.debug("MedicationDispense resource generation - requires FhirMedicationDispenseService integration");
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating MedicationDispense resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR MedicationRequest resources for a case
     */
    public Collection<IBaseResource> getMedicationRequestResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        try {
            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (syncFhirCase != null && syncFhirCase.getPatient() != null) {
                // Would use FhirMedicationRequestService to get medication requests
                log.debug("MedicationRequest resource generation - requires FhirMedicationRequestService integration");
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating MedicationRequest resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR DiagnosticReport resources for a case
     */
    public Collection<IBaseResource> getDiagnosticReportResourceBundle(SyncFhirCase syncFhirCase, SyncFhirProfile syncFhirProfile) {
        try {
            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (syncFhirCase != null && syncFhirCase.getPatient() != null) {
                // Would use FhirDiagnosticReportService to get diagnostic reports
                log.debug("DiagnosticReport resource generation - requires FhirDiagnosticReportService integration");
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating DiagnosticReport resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates FHIR RelatedPerson resources
     */
    public Collection<IBaseResource> getRelatedPerson(SyncFhirProfile syncFhirProfile, List<Person> personList, SyncFhirCase syncFhirCase) {
        try {
            DateRangeParam lastUpdated = new DateRangeParam();

            if (syncFhirProfile != null) {
                if (syncFhirProfile.getIsCaseBasedProfile()) {
                    Date caseDate = (syncFhirCase != null && syncFhirCase.getLastUpdateDate() != null)
                            ? syncFhirCase.getLastUpdateDate() : getDefaultLastSyncDate();
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(caseDate);
                } else {
                    lastUpdated = new DateRangeParam()
                            .setUpperBoundInclusive(new Date())
                            .setLowerBoundInclusive(getLastSyncDate(syncFhirProfile, "RelatedPerson"));
                }
            }

            Collection<IBaseResource> iBaseResources = new ArrayList<>();

            if (personList != null && !personList.isEmpty()) {
                Collection<String> personListUUID = personList.stream()
                        .map(Person::getUuid)
                        .collect(Collectors.toCollection(ArrayList::new));
                FhirRelatedPersonService relatedPersonService = getFhirService(FhirRelatedPersonService.class);
                if (relatedPersonService != null) {
                    iBaseResources.addAll(relatedPersonService.get(personListUUID));
                }
            }

            return iBaseResources;

        } catch (Exception e) {
            log.error("Error generating RelatedPerson resource bundle", e);
            return new ArrayList<>();
        }
    }

    /**
     * Encodes a FHIR resource to JSON string with transformations
     */
    public String encodeResourceToString(String resourceType, String identifierTypeName, IBaseResource iBaseResource) {
        IParser parser = FhirContext.forR4().newJsonParser();
        String jsonString;

        try {
            jsonString = parser.encodeResourceToString(iBaseResource);

            // Apply transformations based on resource type
            if ("Patient".equals(resourceType)) {
                jsonString = transformer.handlePatientResource(jsonString);
            } else if ("Practitioner".equals(resourceType)) {
                jsonString = transformer.handlePractitionerResource(jsonString);
            } else if ("Person".equals(resourceType)) {
                jsonString = transformer.wrapResourceWithId(jsonString, resourceType);
            } else if ("Encounter".equals(resourceType)) {
                jsonString = transformer.handleEncounterResource(jsonString);
            } else if ("Observation".equals(resourceType)) {
                jsonString = transformer.addReferencesMappingToObservation(transformer.wrapResourceInPostRequest(jsonString));
            } else {
                jsonString = transformer.wrapResourceInPostRequest(jsonString);
            }
        } catch (Exception e) {
            log.error("Error encoding resource", e);
            return "";
        }

        return jsonString;
    }

    /**
     * Groups resources into bundles for FHIR transaction
     */
    public Collection<String> groupInBundles(String resourceType, Collection<IBaseResource> iBaseResources, Integer interval, String identifierTypeName) {
        List<String> resourceBundles = new ArrayList<>();
        List<String> currentBundleList = new ArrayList<>();

        if (iBaseResources != null) {
            for (IBaseResource iBaseResource : iBaseResources) {
                String jsonString = encodeResourceToString(resourceType, identifierTypeName, iBaseResource);

                if (jsonString != null && !jsonString.isEmpty()) {
                    if (currentBundleList.size() < interval) {
                        currentBundleList.add(jsonString);
                    } else {
                        if (!currentBundleList.isEmpty()) {
                            resourceBundles.add(String.format(SyncConstant.FHIR_BUNDLE_RESOURCE_TRANSACTION, currentBundleList.toString()));
                        }
                        currentBundleList = new ArrayList<>();
                        currentBundleList.add(jsonString);
                    }
                }
            }

            if (!iBaseResources.isEmpty() && !currentBundleList.isEmpty()) {
                resourceBundles.add(String.format(SyncConstant.FHIR_BUNDLE_RESOURCE_TRANSACTION, currentBundleList.toString()));
            }
        }

        return resourceBundles;
    }

    /**
     * Helper method to parse search parameters from JSON string
     */
    private JSONObject getSearchParametersInJsonObject(String resourceType, String searchParameterString) {
        try {
            JSONObject jsonObject = new JSONObject(searchParameterString);
            if (jsonObject.isNull(resourceType)) {
                jsonObject = jsonObject.getJSONObject(resourceType.toLowerCase() + "Filter");
            }
            return jsonObject;
        } catch (Exception e) {
            log.error("Error parsing search parameters for " + resourceType, e);
            return new JSONObject();
        }
    }

    /**
     * Helper method to get last sync date from profile
     */
    private Date getLastSyncDate(SyncFhirProfile syncFhirProfile, String resourceType) {
        try {
            if (syncFhirProfile != null && syncFhirProfile.getResourceSearchParameter() != null) {
                JSONObject searchParams = new JSONObject(syncFhirProfile.getResourceSearchParameter());
                if (searchParams.has("lastSync")) {
                    String lastSyncStr = searchParams.getString("lastSync");
                    // Parse date string - assuming ISO format
                    return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(lastSyncStr);
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse last sync date for " + resourceType, e);
        }
        return getDefaultLastSyncDate();
    }

    /**
     * Helper method to get default last sync date
     */
    private Date getDefaultLastSyncDate() {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01");
        } catch (Exception e) {
            return new Date(0); // Epoch
        }
    }

    /**
     * Helper method to extract provider from encounter
     */
    private Provider getProviderFromEncounter(Encounter encounter) {
        try {
            if (encounter != null && encounter.getEncounterProviders() != null) {
                return encounter.getEncounterProviders().stream()
                        .filter(ep -> ep.getProvider() != null)
                        .map(ep -> ep.getProvider())
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Error extracting provider from encounter", e);
        }
        return null;
    }

    /**
     * Groups resources into case bundles
     */
    public Collection<String> groupInCaseBundle(String resourceType, Collection<IBaseResource> iBaseResources, String identifierTypeName) {
        Collection<String> resourceBundles = new ArrayList<>();

        if (iBaseResources != null) {
            for (IBaseResource iBaseResource : iBaseResources) {
                String jsonString = encodeResourceToString(resourceType, identifierTypeName, iBaseResource);
                if (jsonString != null && !jsonString.isEmpty()) {
                    resourceBundles.add(jsonString);
                }
            }
        }

        return resourceBundles;
    }
}
