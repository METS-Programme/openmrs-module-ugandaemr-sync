package org.openmrs.module.ugandaemrsync.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.FhirEpisodeOfCareService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Handles JSON transformation operations for FHIR resources.
 * Responsible for manipulating and transforming JSON payloads.
 */
public class FhirResourceTransformer {

    private static final Log log = LogFactory.getLog(FhirResourceTransformer.class);

    private String healthCenterIdentifier;
    private String healthCenterName;
    private SyncFhirProfile profile;

    public FhirResourceTransformer(String healthCenterIdentifier, String healthCenterName, SyncFhirProfile profile) {
        this.healthCenterIdentifier = healthCenterIdentifier;
        this.healthCenterName = healthCenterName;
        this.profile = profile;
    }

    /**
     * Adds organization information to a FHIR resource payload.
     *
     * @param payload JSON payload
     * @param attributeName Attribute name to add organization to
     * @return Modified JSON payload with organization
     */
    public String addOrganizationToRecord(String payload, String attributeName) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }

        String organizationString = String.format("{\"reference\":\"Organization/%s\",\"type\":\"Organization\",\"identifier\":{\"use\":\"official\",\"value\":\"%s\",\"system\":\"https://hmis.health.go.ug/\"},\"display\":\"%s\"}", healthCenterIdentifier, healthCenterIdentifier, healthCenterName);
        JSONObject finalPayLoadJson = new JSONObject(payload);
        JSONObject organization = new JSONObject(organizationString);

        finalPayLoadJson.put(attributeName, organization);
        return finalPayLoadJson.toString();
    }

    /**
     * Adds service type information to a FHIR resource payload.
     *
     * @param payload JSON payload
     * @param attributeName Attribute name to add service type to
     * @return Modified JSON payload with service type
     */
    public String addServiceType(String payload, String attributeName) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        JSONObject finalPayLoadJson = new JSONObject(payload);
        finalPayLoadJson.put(attributeName, new JSONObject("{\"coding\" : [{\"code\": \"dcd87b79-30ab-102d-86b0-7a5022ba4115\", \"display\": \"MEDICAL OUTPATIENT\"}],\"text\" : \"Out-Patient\"}"));
        return finalPayLoadJson.toString();
    }

    /**
     * Adds location information to encounter resource.
     *
     * @param payload JSON payload
     * @return Modified JSON payload with location
     */
    public String addLocationToEncounterResource(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        JSONObject finalPayLoadJson = new JSONObject(payload);
        return finalPayLoadJson.toString();
    }

    /**
     * Transforms patient resource by applying patient-specific transformations.
     *
     * @param jsonString Patient JSON
     * @return Transformed patient JSON
     */
    public String handlePatientResource(String jsonString) {
        jsonString = correctEstimatedDOB(jsonString);
        if (profile != null && profile.getKeepProfileIdentifierOnly() != null && profile.getKeepProfileIdentifierOnly()) {
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

    /**
     * Transforms practitioner resource by applying practitioner-specific transformations.
     *
     * @param jsonString Practitioner JSON
     * @return Transformed practitioner JSON
     */
    public String handlePractitionerResource(String jsonString) {
        jsonString = commonPersonPractitionerTransformations(jsonString);
        return wrapResourceWithId(jsonString, "Practitioner");
    }

    /**
     * Applies common transformations for person and practitioner resources.
     *
     * @param jsonString JSON to transform
     * @return Transformed JSON
     */
    public String commonPersonPractitionerTransformations(String jsonString) {
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

    /**
     * Transforms encounter resource by applying encounter-specific transformations.
     *
     * @param jsonString Encounter JSON
     * @return Transformed encounter JSON
     */
    public String handleEncounterResource(String jsonString) {
        jsonString = addOrganizationToRecord(jsonString, "serviceProvider");
        jsonString = addServiceType(jsonString, "serviceType");
        return wrapResourceInPostRequest(jsonString);
    }

    /**
     * Wraps a resource with its ID for PUT requests.
     *
     * @param jsonString Resource JSON
     * @param resourceType Type of resource
     * @return Wrapped resource JSON
     */
    public String wrapResourceWithId(String jsonString, String resourceType) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String id = jsonObject.getString("id");
            return wrapResourceInPUTRequest(jsonString, resourceType, id);
        } catch (Exception e) {
            log.error("Error wrapping resource with ID: ", e);
            return wrapResourceInPostRequest(jsonString);
        }
    }

    /**
     * Adds "use": "official" to name objects in JSON.
     *
     * @param payload JSON payload
     * @param attributeName Name of the attribute to modify
     * @return Modified JSON payload
     */
    public String addUseOfficialToName(String payload, String attributeName) {
        JSONObject jsonObject = new JSONObject(payload);
        if (!jsonObject.has(attributeName)) {
            return payload; // Return original if attribute doesn't exist
        }
        int objectCount = 0;
        for (Object jsonObject1 : jsonObject.getJSONArray(attributeName)) {
            jsonObject.getJSONArray(attributeName).getJSONObject(objectCount).put("use", "official");
            objectCount++;
        }
        return jsonObject.toString();
    }

    /**
     * Removes all identifiers except the profile identifier.
     *
     * @param payload JSON payload
     * @param attributeName Attribute name containing identifiers
     * @return Modified JSON payload
     */
    public String removeIdentifierExceptProfileId(String payload, String attributeName) {
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

    /**
     * Removes an attribute from JSON payload.
     *
     * @param payload JSON payload
     * @param attributeName Attribute to remove
     * @return Modified JSON payload
     */
    public String removeAttribute(String payload, String attributeName) {
        JSONObject jsonObject = new JSONObject(payload);
        if (jsonObject.has(attributeName)) {
            jsonObject.remove(attributeName);
        }
        return jsonObject.toString();
    }

    /**
     * Adds coding information to identifier in JSON.
     *
     * @param payload JSON payload
     * @param attributeName Attribute name containing identifiers
     * @return Modified JSON payload
     */
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

    /**
     * Corrects estimated date of birth in patient JSON.
     *
     * @param payload JSON payload
     * @return Modified JSON payload with corrected DOB
     */
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

    /**
     * Adds an attribute to an object within JSON.
     *
     * @param payload JSON payload
     * @param targetObject Name of the target object
     * @param attributeName Attribute name to add
     * @param attributeValue Attribute value to set
     * @return Modified JSON payload
     */
    public String addAttributeToObject(String payload, String targetObject, String attributeName, String attributeValue) {
        JSONObject jsonObject = new JSONObject(payload);
        if (jsonObject.has(targetObject) && jsonObject.getJSONArray(targetObject).length() > 0) {
            for (int i = 0; i < jsonObject.getJSONArray(targetObject).length(); i++) {
                jsonObject.getJSONArray(targetObject).getJSONObject(i).put(attributeName, attributeValue);
                i++;
            }
        }
        return jsonObject.toString();
    }

    /**
     * Gets identifier system URL from global properties.
     *
     * @param propertyName Global property name
     * @return Identifier system URL
     */
    public String getIdentifierSystemURL(String propertyName) {
        return Context.getAdministrationService().getGlobalProperty(propertyName);
    }

    /**
     * Wraps a resource in a POST request format.
     *
     * @param payload Resource JSON
     * @return Wrapped resource JSON for POST request
     */
    public String wrapResourceInPostRequest(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }

        String wrappedResourceInRequest = String.format(SyncConstant.FHIR_BUNDLE_RESOURCE_METHOD_POST, payload);
        return wrappedResourceInRequest;
    }

    /**
     * Wraps a resource in a PUT request format.
     *
     * @param payload Resource JSON
     * @param resourceType Type of resource
     * @param identifier Resource identifier
     * @return Wrapped resource JSON for PUT request
     */
    public String wrapResourceInPUTRequest(String payload, String resourceType, String identifier) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }

        String wrappedResourceInRequest = String.format(SyncConstant.FHIR_BUNDLE_RESOURCE_METHOD_PUT, payload, resourceType + "/" + identifier);
        return wrappedResourceInRequest;
    }

    /**
     * Encodes a FHIR resource to JSON string with transformations.
     *
     * @param resourceType Type of FHIR resource
     * @param identifierTypeName Identifier type name
     * @param iBaseResource FHIR resource to encode
     * @return Encoded and transformed JSON string
     */
    public String encodeResourceToString(String resourceType, String identifierTypeName, IBaseResource iBaseResource) {
        IParser parser = FhirContext.forR4().newJsonParser();
        String jsonString;

        try {
            jsonString = parser.encodeResourceToString(iBaseResource);

            switch (resourceType) {
                case "Patient":
                    jsonString = handlePatientResource(jsonString);
                    break;

                case "Practitioner":
                    jsonString = handlePractitionerResource(jsonString);
                    break;

                case "Person":
                    jsonString = wrapResourceWithId(jsonString, resourceType);
                    break;

                case "Encounter":
                    jsonString = handleEncounterResource(jsonString);
                    break;

                case "Observation":
                    jsonString = addReferencesMappingToObservation(wrapResourceInPostRequest(jsonString));
                    break;

                default:
                    jsonString = wrapResourceInPostRequest(jsonString);
                    break;
            }
        } catch (Exception e) {
            log.error("Error encoding resource: ", e);
            return "";
        }

        return jsonString;
    }

    /**
     * Adds references mapping to observation resource.
     *
     * @param observation Observation JSON
     * @return Modified observation JSON with references
     */
    public String addReferencesMappingToObservation(String observation) {
        org.openmrs.api.ConceptService conceptService = Context.getConceptService();
        JSONObject jsonObject = new JSONObject(observation);
        JSONObject observationResource = jsonObject.getJSONObject("resource");
        String conceptUUid = observationResource.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code");

        Concept concept = conceptService.getConceptByUuid(conceptUUid);

        JSONArray newQuestionJson = observationResource.getJSONObject("code").getJSONArray("coding");

        newQuestionJson.put(new JSONObject(String.format(SyncConstant.FHIR_CODING_DATATYPE, "UgandaEMR", concept.getConceptId(), concept.getName().getName())));

        if (concept.getConceptMappings().size() > 0) {
            for (ConceptMap conceptQuestionMap : concept.getConceptMappings()) {
                newQuestionJson.put(new JSONObject(String.format(SyncConstant.FHIR_CODING_DATATYPE, conceptQuestionMap.getConceptReferenceTerm().getConceptSource().getName(), conceptQuestionMap.getConceptReferenceTerm().getCode(), conceptQuestionMap.getConceptReferenceTerm().getName())));
            }
        }

        if (concept.getDatatype().equals(conceptService.getConceptDatatypeByUuid("8d4a48b6-c2cc-11de-8d13-0010c6dffd0f")) && !observationResource.isNull("valueCodeableConcept")) {
            JSONArray newValueCodeableJson = observationResource.getJSONObject("valueCodeableConcept").getJSONArray("coding");
            String valueCodedConceptUUid = observationResource.getJSONObject("valueCodeableConcept").getJSONArray("coding").getJSONObject(0).getString("code");
            Concept valueCodedConcept = conceptService.getConceptByUuid(valueCodedConceptUUid);
            newValueCodeableJson.put(new JSONObject(String.format(SyncConstant.FHIR_CODING_DATATYPE, "UgandaEMR", valueCodedConcept.getConceptId(), valueCodedConcept.getName().getName())));
            for (ConceptMap conceptMap : valueCodedConcept.getConceptMappings()) {
                newValueCodeableJson.put(new JSONObject(String.format(SyncConstant.FHIR_CODING_DATATYPE, conceptMap.getConceptReferenceTerm().getConceptSource().getName(), conceptMap.getConceptReferenceTerm().getCode(), conceptMap.getConceptReferenceTerm().getName())));
            }
        }

        return jsonObject.toString();
    }

    /**
     * Adds episode of care reference to encounter resource.
     *
     * @param encounter Encounter JSON
     * @param episodeOfcare Episode of care data
     * @return Modified encounter JSON with episode of care reference
     */
    public String addEpisodeOfCareToEncounter(String encounter, Object episodeOfcare) {
        String episodeOfCareReference = "{\"reference\":\"EpisodeOfCare/%s\"}";

        JSONObject jsonObject = new JSONObject(encounter);

        Date encounterDate = null;
        try {
            encounterDate = new SimpleDateFormat("yyyy-MM-dd").parse(jsonObject.getJSONObject("period").getString("start"));
        } catch (Exception e) {
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

    /**
     * Groups resources into bundles for FHIR transaction.
     *
     * @param resourceType Type of resources
     * @param iBaseResources Collection of FHIR resources
     * @param interval Maximum resources per bundle
     * @param identifierTypeName Identifier type name
     * @return Collection of bundle JSON strings
     */
    public Collection<String> groupInBundles(String resourceType, Collection<IBaseResource> iBaseResources, Integer interval, String identifierTypeName) {
        List<String> resourceBundles = new ArrayList<>();
        List<String> currentBundleList = new ArrayList<>();

        for (IBaseResource iBaseResource : iBaseResources) {
            String jsonString = encodeResourceToString(resourceType, identifierTypeName, iBaseResource);

            if (currentBundleList.size() < interval) {
                currentBundleList.add(jsonString);
            } else {
                if (!currentBundleList.toString().equals("")) {
                    resourceBundles.add(String.format(SyncConstant.FHIR_BUNDLE_RESOURCE_TRANSACTION, currentBundleList.toString()));
                }
                currentBundleList = new ArrayList<>();
                currentBundleList.add(jsonString);
            }
        }

        if (iBaseResources.size() > 0 && currentBundleList.size() < interval) {
            resourceBundles.add(String.format(SyncConstant.FHIR_BUNDLE_RESOURCE_TRANSACTION, currentBundleList.toString()));
        }

        return resourceBundles;
    }

    /**
     * Groups resources into case bundles.
     *
     * @param resourceType Type of resources
     * @param iBaseResources Collection of FHIR resources
     * @param identifierTypeName Identifier type name
     * @return Collection of resource JSON strings
     */
    public Collection<String> groupInCaseBundle(String resourceType, Collection<IBaseResource> iBaseResources, String identifierTypeName) {
        Collection<String> resourceBundles = new ArrayList<>();

        for (IBaseResource iBaseResource : iBaseResources) {
            String jsonString = encodeResourceToString(resourceType, identifierTypeName, iBaseResource);
            resourceBundles.add(jsonString);
        }

        return resourceBundles;
    }

    /**
     * Adds search parameter to resource type.
     *
     * @param resourceType Type of FHIR resource
     * @param searchParam Search parameter name
     * @param searchParamString Search parameter string
     * @return Search parameter string
     */
    public String addSearchParameter(String resourceType, String searchParam, String searchParamString) {
        return searchParamString;
    }
}