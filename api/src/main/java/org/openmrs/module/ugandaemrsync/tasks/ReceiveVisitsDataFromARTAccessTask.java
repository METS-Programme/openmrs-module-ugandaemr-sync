package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.EncounterType;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.Obs;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.Patient;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.api.UserService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.FormService;
import org.openmrs.api.VisitService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.server.SyncGlobalProperties;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.*;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.*;

public class ReceiveVisitsDataFromARTAccessTask extends AbstractTask {
    protected final Log log = LogFactory.getLog(ReceiveVisitsDataFromARTAccessTask.class);

    SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
    UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
    ConceptService conceptService = Context.getConceptService();
    VisitService visitService = Context.getVisitService();
    EncounterService encounterService =Context.getEncounterService();
    LocationService locationService = Context.getLocationService();
    Location pharmacyLocation = locationService.getLocationByUuid("3ec8ff90-3ec1-408e-bf8c-22e4553d6e17");
    EncounterType artCardEncounterType = encounterService.getEncounterTypeByUuid("8d5b2be0-c2cc-11de-8d13-0010c6dffd0f");
    SyncTaskType syncTaskType = ugandaEMRSyncService.getSyncTaskTypeByUUID(ART_ACCESS_PULL_TYPE_UUID);

    public Concept medicineQuantityCodedConcept= conceptService.getConcept(160856);
    public Concept medicineStrengthCodedConcept= conceptService.getConcept(1444);
    public Concept medicineDurationCodedConcept= conceptService.getConcept(159368);
    public Concept dosingUnitsCodedConcept= conceptService.getConcept(165791);
    public Concept medicationFrequencyCodedConcept= conceptService.getConcept(160855);
    public Concept durationUnitsCodedConcept= conceptService.getConcept(1732);
    public Concept ncdMedicineConcept= conceptService.getConcept(166707);

    @Override
    public void execute() {
        UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();

        if (!isGpDhis2OrganizationUuidSet()) {
            return;
        }

        if (!ugandaEMRHttpURLConnection.isConnectionAvailable()) {
            return;
        }

        String artAccessServerUrlEndPoint="";
        String results="";
        if(syncTaskType.getUrl()!=null){
            artAccessServerUrlEndPoint = syncTaskType.getUrl();
            artAccessServerUrlEndPoint = addParametersToUrl(artAccessServerUrlEndPoint);

            if (!ugandaEMRHttpURLConnection.isServerAvailable(artAccessServerUrlEndPoint)) {
                log.error("server not available ");
                return;
            }

        }
        Map resultMap = new HashMap<>();
        try {
            String username = syncTaskType.getUrlUserName();
            String password = syncTaskType.getUrlPassword();
                        
            resultMap = ugandaEMRHttpURLConnection.getByWithBasicAuth(artAccessServerUrlEndPoint, username, password, "String");

            results = (String)resultMap.get("result");

        } catch (Exception e) {
            log.error("Failed to fetch results",e);
        }

         if (results != null && !results.isEmpty() ) {
             JSONObject object = new JSONObject(results);
             processData(object, resultMap);

         }else{
             log.error("Results are empty");
         }
    }

    public boolean isGpDhis2OrganizationUuidSet() {
        if (isBlank(syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID))) {
            log.info("DHIS2 Organization UUID is not set");
            return false;
        }
        return true;
    }

    public String addParametersToUrl(String url) {
        String uuid  = syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID);
        String lastSyncDate = syncGlobalProperties.getGlobalProperty(GP_ART_ACCESS_LAST_SYNC_DATE);
        log.info(lastSyncDate+"last sync date");
        String uuidParameter  = "&managingOrganisation="+uuid;
        String startDateParameter;
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);

        String newEndDate = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDateParameter = "&%20periodEnd="+ newEndDate+"%20"+"23:59:59";
        if (lastSyncDate == null || lastSyncDate.equals("")) {
           startDateParameter=  "?periodStart="+"2021-06-01"+"%20"+"00:00:00";
        } else {
            startDateParameter = "?periodStart="+lastSyncDate+"%20"+"00:00:00";
        }
        return url+startDateParameter+endDateParameter+uuidParameter;
    }

    private void processData(JSONObject jsonObject, Map resultsMap) {
      JSONArray patientRecords =  jsonObject.getJSONArray("entry");

      if(patientRecords.length()>0) {
          for (Object o : patientRecords) {
             JSONObject patientRecord = (JSONObject)o;
           JSONObject patientAttributes = patientRecord.getJSONArray("entry").getJSONObject(0);
           int no_of_days =(int) patientRecord.getJSONArray("entry").getJSONObject(1).get("number_of_days");
           int no_of_pills =(int) patientRecord.getJSONArray("entry").getJSONObject(1).get("number_of_pills");
           JSONObject patientEncounterDetails = patientRecord.getJSONArray("entry").getJSONObject(3);

           String patientARTNo = getIdentifier(patientAttributes);
           Patient patient = ugandaEMRSyncService.getPatientByPatientIdentifier(patientARTNo);
           if(patient!=null){
               log.info(patientARTNo);
                processPatientBundle(patientEncounterDetails,patient,no_of_days,no_of_pills);
           }

          }
          savedSyncTask(resultsMap);
      }

    }

    private void processPatientBundle(JSONObject jsonObject, Patient patient,Integer no_of_days,Integer no_of_pills){
        HashMap conceptsCaptured = getARTAccessRecordsConcepts();
        UserService userService = Context.getUserService();
        User user = userService.getUserByUuid("9bd6584f-33e0-11e7-9528-1866da16840d");

        Set<Obs> obsList =new HashSet<>();

          try {
            log.error("processing visit dates");
            String visit_date = getJSONObjectValue(jsonObject.getJSONObject("0"), "visit_date");
            String dateFormat = ugandaEMRSyncService.getDateFormat(visit_date);
            Date startVisitDate = ugandaEMRSyncService.convertStringToDate(visit_date, "00:00:00", dateFormat);
            Date stopVisitDate = ugandaEMRSyncService.convertStringToDate(visit_date, "23:59:59", dateFormat);

            String next_visit_date = getJSONObjectValue(jsonObject.getJSONObject("1"), "next_visit_date");
           try{ Date return_date = ugandaEMRSyncService.convertStringToDate(next_visit_date, "00:00:00", ugandaEMRSyncService.getDateFormat(next_visit_date));
            if(!Objects.equals(next_visit_date, "") && next_visit_date!=null) {
                log.info("adding next_visit_date to obs list");
                log.info(next_visit_date);
                addObs(obsList, next_visit_date, conceptService.getConcept((int) conceptsCaptured.get("next_visit_date")), 
                    null, return_date, null, patient, user, startVisitDate);
                log.info("exited add obs");
            }
            else {
                log.info("Next visit date is empty or null");
            }
            }catch (Exception e){
                log.error("An Exception was thrown during addObs");
                e.printStackTrace();
            }

            log.info("adherence starts here");
            String adherence = getJSONObjectValue(jsonObject.getJSONObject("4"),"adherence");
            Concept adherence_concept = conceptService.getConcept((int)conceptsCaptured.get("adherence"));
            Concept adherence_answer = convertAdherence(adherence);
            addObs(obsList,adherence,adherence_concept, adherence_answer, null, null, patient, user, startVisitDate);

            String regimen= convertObjectToStringIfNotNull(jsonObject.getJSONObject("regimen").getJSONObject("coding").get("code"));
            
            Concept regimenConcept = conceptService.getConcept((int) conceptsCaptured.get("regimen"));
            Concept regimenAnswer = convertRegimen(regimen);

            if(regimenAnswer!=null) {
               
                Obs regimenObs =  processObs(regimenConcept, regimenAnswer, null, null, patient, user, startVisitDate);
                Obs pills =processObs(conceptService.getConcept(99038),no_of_pills,patient,user,startVisitDate); // no. of pills
                Obs days =processObs(conceptService.getConcept(99036),no_of_days,patient,user,startVisitDate); // no. of days
                Obs groupObs = processObs(conceptService.getConcept(165430),null,null,null,patient,user,startVisitDate);
                groupObs.addGroupMember(regimenObs);
                groupObs.addGroupMember(pills);
                groupObs.addGroupMember(days);
                obsList.add(groupObs);
                
            }
            else{
                log.info("Missing regiment in emr");
            }

            String other_drugs = getJSONObjectValue(jsonObject.getJSONObject("13"),"other_drugs");
            Concept other_medicationsConcept = conceptService.getConcept((int)conceptsCaptured.get("other_drugs"));
            addObs(obsList,other_drugs,other_medicationsConcept,null,null,other_drugs,patient,user,startVisitDate);

            String complaint = getJSONObjectValue(jsonObject.getJSONObject("6"),"complaints");
            Concept complaintQuestion  = conceptService.getConcept((int) conceptsCaptured.get("complaints"));
              assert complaint != null;
              if(!complaint.contains("null")){
                if(complaint.contains(",")){
                    List<String> complaints = Arrays.asList(complaint.split(","));
                    for (String s:complaints) {
                        Concept answer = convertComplaints(s);
                        if(answer!=null) {
                            addObs(obsList, complaint, complaintQuestion, answer, null, null, patient, user, startVisitDate);
                        }
                    }

                }else{
                    Concept answer = convertComplaints(complaint);
                    if(answer!=null) {
                        addObs(obsList, complaint, complaintQuestion, answer, null, null, patient, user, startVisitDate);
                    }
                }

            }

            //processing family planning commodities

            Concept familyPlanningMethodQuestion  = conceptService.getConcept(374);

            JSONArray familyPlanningMethods = jsonObject.getJSONObject("18").getJSONObject("family_planning").getJSONObject("coding").getJSONArray("methods");

            for (int i = 0 ; i < familyPlanningMethods.length(); i++) {
                JSONObject obj = familyPlanningMethods.getJSONObject(i);
                String fpMethodName = obj.getString("display");

                if(Objects.equals(fpMethodName, "Condoms")) {
                    Concept answerCondoms = convertFamilyPlanningMethods("condoms");
                    addObs(obsList, "condoms", familyPlanningMethodQuestion, answerCondoms, null, null, patient, user, startVisitDate);
                }
                else if(Objects.equals(fpMethodName, "Sayana Press")) {
                    Concept sayana_press = convertFamilyPlanningMethods("sayanaPress");
                    addObs(obsList, "sayanaPress", familyPlanningMethodQuestion, sayana_press, null, null, patient, user, startVisitDate);
                }
            }

            //processing hypertension medicine
            Concept hypertensionMedicineConcept = conceptService.getConcept(1282);

            JSONObject hypertensionMedication = jsonObject.getJSONObject("20").getJSONObject("hypertension_medicine").getJSONObject("coding");

            String hmQuantity = hypertensionMedication.getString("quantity");
            String hmStrength = (String) hypertensionMedication.get("strength");
            String hmDuration = hypertensionMedication.getString("duration");
            String hmDurationUnit = (String) hypertensionMedication.get("durationUnit");
            String hmDosingUnit = (String) hypertensionMedication.get("dosingUnit");
            String hmFrequency = (String) hypertensionMedication.get("frequencyOfmedication");
            String hmConceptId = hypertensionMedication.getString("conceptId");

            if(!hmQuantity.equals("null") && !hmStrength.equals("null") && !hmDuration.equals("null") && !hmDosingUnit.equals("null") && !hmFrequency.equals("null") && !hmConceptId.equals("null")) {
 

                int hypertensionMedicineQuantity = Integer.parseInt(hypertensionMedication.getString("quantity"));
                String hypertensionMedicineStrength = (String) hypertensionMedication.get("strength");
                int hypertensionMedicineDuration = Integer.parseInt(hypertensionMedication.getString("duration"));
                String hypertensionMedicineDurationUnit = (String) hypertensionMedication.get("durationUnit");
                String hypertensionMedicineDosingUnit = (String) hypertensionMedication.get("dosingUnit");
                String hypertensionMedicineFrequency = (String) hypertensionMedication.get("frequencyOfmedication");
                String hypertensionMedicineConceptId = hypertensionMedication.getString("conceptId");
                Concept hypertensionMedicineAnswer = conceptService.getConcept(hypertensionMedicineConceptId);
                Concept hypertensionMedicineFrequencyAnswer = convertMedicationFrequency(hypertensionMedicineFrequency);
                Concept hypertensionMedicineDurationUnitsAnswer = convertDurationUnits(hypertensionMedicineDurationUnit);
                Concept hypertensionMedicineDosingUnitsAnswer = convertDosingUnits(hypertensionMedicineDosingUnit);

                Obs hypertensionMedicineObs =  processObs(hypertensionMedicineConcept, hypertensionMedicineAnswer, null, null, patient, user, startVisitDate);
                Obs hypertensionMedicineQuantityObs = processObs(medicineQuantityCodedConcept,hypertensionMedicineQuantity,patient,user,startVisitDate); 
                Obs hypertensionMedicinestrengthObs = processObs(medicineStrengthCodedConcept,hypertensionMedicineStrength,patient,user,startVisitDate); //
                Obs hypertensionMedicineDurationObs = processObs(medicineDurationCodedConcept,hypertensionMedicineDuration,patient,user,startVisitDate); // numeric duration e.g 10
                Obs hypertensionMedicinedosingUnitObs = processObs(dosingUnitsCodedConcept,hypertensionMedicineDosingUnitsAnswer, null, null, patient,user,startVisitDate); // dosing unit e.g tablets/capsules
                Obs hypertensionMedicineFrequencyObs = processObs(medicationFrequencyCodedConcept,hypertensionMedicineFrequencyAnswer, null, null, patient,user,startVisitDate); // frequency of medication
                Obs hypertensionMedicineDurationUnitObs =processObs(durationUnitsCodedConcept,hypertensionMedicineDurationUnitsAnswer, null, null, patient,user,startVisitDate); // duration unit eg days

                Obs groupObs = processObs(ncdMedicineConcept,null,null,null,patient,user,startVisitDate);

                groupObs.addGroupMember(hypertensionMedicineObs);
                groupObs.addGroupMember(hypertensionMedicineQuantityObs);
                groupObs.addGroupMember(hypertensionMedicinestrengthObs);
                groupObs.addGroupMember(hypertensionMedicinedosingUnitObs);
                groupObs.addGroupMember(hypertensionMedicineDurationObs);
                groupObs.addGroupMember(hypertensionMedicineDurationUnitObs);
                groupObs.addGroupMember(hypertensionMedicineFrequencyObs);
                obsList.add(groupObs);

                }
                else {
                    log.info("Missing Hypertension medicine information");
                }

            //processing diabetes medicine
            Concept diabetesMedicineConcept = conceptService.getConcept(1282);

            JSONObject diabetesMedication = jsonObject.getJSONObject("19").getJSONObject("diabetes_medicine").getJSONObject("coding");

            String  dmQuantity = diabetesMedication.getString("quantity");
            String dmStrength = (String) diabetesMedication.get("strength");
            String  dmDuration = diabetesMedication.getString("duration");
            String dmDurationUnit = (String) diabetesMedication.get("durationUnit");
            String dmDosingUnit = (String) diabetesMedication.get("dosingUnit");
            String dmFrequency = (String) diabetesMedication.get("frequencyOfmedication");
            String dmConceptId = diabetesMedication.getString("conceptId");

            if(!dmQuantity.equals("null") && !dmStrength.equals("null") && !dmDuration.equals("null") && !dmDosingUnit.equals("null") && !dmFrequency.equals("null") && !dmConceptId.equals("null")) {

                int diabetesMedicineQuantity = Integer.parseInt(diabetesMedication.getString("quantity"));
                String diabetesMedicineStrength =  (String) diabetesMedication.get("strength");
                int diabetesMedicineDuration = Integer.parseInt(diabetesMedication.getString("duration"));
                String diabetesMedicineDurationUnit = (String) diabetesMedication.get("durationUnit");
                String diabetesMedicineDosingUnit = (String) diabetesMedication.get("dosingUnit");
                String diabetesMedicineFrequency = (String) diabetesMedication.get("frequencyOfmedication");
                String diabetesMedicineConceptId = diabetesMedication.getString("conceptId");
                Concept diabetesMedicineAnswer = conceptService.getConcept(diabetesMedicineConceptId);
                Concept diabetesMedicineFrequencyAnswer = convertMedicationFrequency(diabetesMedicineFrequency);
                Concept diabetesMedicineDurationUnitsAnswer = convertDurationUnits(diabetesMedicineDurationUnit);
                Concept diabetesMedicineDosingUnitsAnswer = convertDosingUnits(diabetesMedicineDosingUnit);

                Obs diabetesMedicineObs =  processObs(diabetesMedicineConcept, diabetesMedicineAnswer, null, null, patient, user, startVisitDate);
                Obs diabetesMedicineQuantityObs = processObs(medicineQuantityCodedConcept,diabetesMedicineQuantity,patient,user,startVisitDate); 
                Obs diabetesMedicinestrengthObs = processObs(medicineStrengthCodedConcept,diabetesMedicineStrength,patient,user,startVisitDate); 
                Obs diabetesMedicineDurationObs = processObs(medicineDurationCodedConcept,diabetesMedicineDuration,patient,user,startVisitDate);
                Obs diabetesMedicinedosingUnitObs = processObs(dosingUnitsCodedConcept,diabetesMedicineDosingUnitsAnswer, null, null, patient,user,startVisitDate); // dosing unit e.g tablets/capsules
                Obs diabetesMedicineFrequencyObs = processObs(medicationFrequencyCodedConcept,diabetesMedicineFrequencyAnswer, null, null, patient,user,startVisitDate); // frequency of medication
                Obs diabetesMedicineDurationUnitObs =processObs(durationUnitsCodedConcept,diabetesMedicineDurationUnitsAnswer, null, null, patient,user,startVisitDate);// duration unit eg days
              
                Obs diabetesGroupObs = processObs(ncdMedicineConcept,null,null,null,patient,user,startVisitDate);
                diabetesGroupObs.addGroupMember(diabetesMedicineObs);
                diabetesGroupObs.addGroupMember(diabetesMedicineQuantityObs);
                diabetesGroupObs.addGroupMember(diabetesMedicinestrengthObs);
                diabetesGroupObs.addGroupMember(diabetesMedicinedosingUnitObs);
                diabetesGroupObs.addGroupMember(diabetesMedicineDurationObs);
                diabetesGroupObs.addGroupMember(diabetesMedicineDurationUnitObs);
                diabetesGroupObs.addGroupMember(diabetesMedicineFrequencyObs);
                obsList.add(diabetesGroupObs);

            }
            else {
                log.info("Missing Diabetes medicine information");
            } 

            addObsToEncounter(patient,startVisitDate,stopVisitDate,obsList,user);

        }catch (Exception e){
            log.info(e.getMessage());
        }
    }

    private String getIdentifier(JSONObject jsonObject){
        String identifier="";
        if(jsonObject!=null) {
            identifier = (String) jsonObject.getJSONObject("resource").getJSONArray("identifier").
                    getJSONObject(0).getJSONObject("type").getJSONObject("coding").getJSONObject("0").get("code");
        }
        return identifier;
    }

    private HashMap getARTAccessRecordsConcepts() {
        HashMap<String,Integer> map = new HashMap<>();
        map.put("visit_date",null);
        map.put("next_visit_date",5096);
        map.put("medicine_picked",null);
        map.put("clinical_status",null);
        map.put("adherence",90221);
        map.put("viral_load",856);
        map.put("complaints",90227); // side effects
        map.put("other_complaints",99113); // other side effects
        map.put("reference_reason",null);
        map.put("client_representative",null);
        map.put("discontinue_reason",164975); // other reason for next appointment
        map.put("admission_since_last_visit",null);
        map.put("pharmacist_name",null);
        map.put("other_drugs",99035);
        map.put("next_facility_visit",null);
        map.put("regimen",90315);
        
        return map;
    }

    private String getJSONObjectValue(JSONObject jsonObject,String objectName){
        Object value = "";
        if(jsonObject!=null) {
          value = jsonObject.getJSONObject(objectName).getJSONObject("coding").get("code");
        }
        try {
            return (String)value;
        }catch (ClassCastException e){
            return null;
        }
    }

    private Visit createVisit(Patient patient, Date startDate,Date stopDate,User creator,Location location){
        Visit visit = new Visit();
        visit.setLocation(location);
        visit.setPatient(patient);
        visit.setStartDatetime(startDate);
        visit.setStopDatetime(stopDate);
        visit.setVisitType(visitService.getVisitTypeByUuid("2ce24f40-8f4c-4bfa-8fde-09d475783468"));
        visit.setCreator(creator);
        visit.setDateCreated(new Date());
        return  visit;

    }

    private Encounter createEncounter(Patient patient,Date visitDate,User creator,Location location){
        FormService formService =Context.getFormService();
        Encounter encounter = new Encounter();
        encounter.setEncounterDatetime(visitDate);
        encounter.setEncounterType(encounterService.getEncounterTypeByUuid("8d5b2be0-c2cc-11de-8d13-0010c6dffd0f"));
        encounter.setForm(formService.getFormByUuid("12de5bc5-352e-4faf-9961-a2125085a75c"));
        encounter.setPatient(patient);
        encounter.setCreator(creator);
        encounter.setLocation(location);
        encounter.setDateCreated(new Date());
        return encounter;

    }

    private Obs processObs(Concept question, Concept valueCoded, Date valueDateTime,String valueText,Patient patient,User creator,Date visitDate){
       
        Obs newObs = new Obs();
        newObs.setConcept(question);
        newObs.setValueCoded(valueCoded);
        newObs.setValueText(valueText);
        newObs.setValueDatetime(valueDateTime);
        newObs.setCreator(creator);
        newObs.setObsDatetime(visitDate);
        newObs.setPerson(patient);
        newObs.setLocation(pharmacyLocation);
        return newObs;
    }
    private Obs processObs(Concept question,double valueNumeric,Patient patient,User creator,Date visitDate){
        
        Obs newObs = new Obs();
        newObs.setConcept(question);
        newObs.setValueNumeric(valueNumeric);
        newObs.setCreator(creator);
        newObs.setObsDatetime(visitDate);
        newObs.setPerson(patient);
        newObs.setLocation(pharmacyLocation);
        
        return newObs;
    }

    private Obs processObs(Concept question,String valueText,Patient patient,User creator,Date visitDate){
        Obs newObs = new Obs();
        newObs.setConcept(question);
        newObs.setValueText(valueText);
        newObs.setCreator(creator);
        newObs.setObsDatetime(visitDate);
        newObs.setPerson(patient);
        newObs.setLocation(pharmacyLocation);
        return newObs;
    }

    private Set<Obs> addObs(Set<Obs> obsList,String artAccessValue,Concept question, Concept valueCoded, Date valueDateTime,String valueText,Patient patient,User creator,Date visitDate){

        if (artAccessValue!=null) {
            
            Obs singleProcessedOb = processObs(question,valueCoded, valueDateTime,valueText,patient,creator,visitDate);

            obsList.add(singleProcessedOb);
            
        }
        
        return obsList;
    }

    private Concept convertAdherence(String adherence) {
        int conceptValue =0 ;
        if(adherence!=null) {
            if (adherence.contains("poor")) {
                conceptValue = 90158;
            } else if (adherence.contains("good")) {
                conceptValue = 90156;
            } else if (adherence.contains("fair")) {
                conceptValue =90157;
            } else {
                conceptValue = 90001; // unknown
            }
        }
        if(conceptValue!=0){
            return conceptService.getConcept(conceptValue);
        }else{
            return  null;
        }
    }

    private Concept convertComplaints(String complaint) {
        int conceptValue =0 ;
        if(complaint!=null) {
            if (complaint.contains("cough")) {
                conceptValue = 90132;
            } else if (complaint.contains("fever")) {
                conceptValue = 90116;
            } else if (complaint.contains("weight loss")) {
                conceptValue =90135;
            } else if (complaint.contains("Diarrhoea")) {
                conceptValue =16;
            } else if (complaint.contains("Headache")) {
                conceptValue =90094;
            }
        }
        if(conceptValue!=0){
            return conceptService.getConcept(conceptValue);
        }else{
            return  null;
        }
    }

    private Concept convertFamilyPlanningMethods(String fpMethod){
        int conceptValue =0 ;
        if(fpMethod!=null) {
            if (fpMethod.contains("Condoms")) {
                conceptValue = 190;
            } else if (fpMethod.contains("Sayana Press")) {
                conceptValue = 175402; 
            }
        }
        if(conceptValue!=0){
            return conceptService.getConcept(conceptValue);
        }else{
            return  null;
        }
    }

    private Concept convertDurationUnits(String durationUnits) {
        int conceptValue;
        HashMap<String, Integer> map =new HashMap<>();
        map.put("seconds", 162583);
        map.put("minutes", 1733);
        map.put("hours", 1822);
        map.put("days", 1072);
        map.put("weeks", 1073);
        map.put("months" , 1074);
        map.put("years" , 1734);

        if(durationUnits!=null) {
            conceptValue = map.get(durationUnits);
            return conceptService.getConcept(conceptValue);
        }
        else {
            return null;
        }
    }

    private Concept convertMedicationFrequency(String medicationFrequency) {
        int conceptValue;
        HashMap<String, Integer> map =new HashMap<>();
        map.put("OD", 160862);
        map.put("BD", 160858);
        map.put("TDS", 160866);
        map.put("QID", 160870);

        if(medicationFrequency!=null) {
            conceptValue = map.get(medicationFrequency);
            return conceptService.getConcept(conceptValue);
        }
        else {
            return null;
        }
    }

    private Concept convertDosingUnits(String dosingUnits) {
        int conceptValue;
        HashMap<String, Integer> map =new HashMap<>();
        map.put("tablets", 1513);
        map.put("capsules", 1608);

        if(dosingUnits!=null) {
            conceptValue = map.get(dosingUnits);
            return conceptService.getConcept(conceptValue);
        }
        else {
            return null;
        }
    }

    private Concept convertRegimen(String regimenName) {
        int conceptValue;
        HashMap<String, Integer> map =new HashMap<>();
        map.put("TDF/3TC/EFV",99040);
        map.put("ZDV/3TC/NVP" ,0);
        map.put("ZDV/3TC/EFV",0);
        map.put("TDF/FTC/EFV",99042);
        map.put("TDF/ZDV/3TC",0);
        map.put("ZDV/3TC/ABC",0);
        map.put("NVP/TDF/3TC",99039);
        map.put("AZT/3TC/DTG",164979);
        map.put("TDF/3TC/NVT",0);
        map.put("AZT/3TC/EFV",99006);
        map.put("AZT/3TC/NVP",99005);
        map.put("TDF/3TC/NVP",99039);
        map.put("TDF/3TC/DTG",164977);
        map.put("ABC/3TC/DTG",164978);
        map.put("AZT/3TC/ATV/r",99286);
        map.put("TDF/3TC/ATV/r",99887);
        map.put("ABC/3TC/EFV",99885);
        map.put("ABC/3TC/LPV/r",163017);
        map.put("TDF/3TC/LPV/r",99044);
        map.put("AZT/3TC/LPV/r",99046);
        map.put("ABC/3TC/ATV/r",99888);
        map.put("Other Specify",0);

        if(regimenName!=null){
            conceptValue = map.get(regimenName);
            return conceptService.getConcept(conceptValue);
        }else{
            return null;
        }
    }

    private String convertObjectToStringIfNotNull(Object object){
        if(object!=null){
            return (String)object;
        }else{
            return null;
        }
    }

    private void addObsToEncounter(Patient patient,Date startVisitDate,Date stopVisitDate,Set<Obs> obsList,User creator){

        EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(patient, pharmacyLocation, startVisitDate, stopVisitDate, null, null, Arrays.asList(artCardEncounterType), null, Arrays.asList(visitService.getVisitTypeByUuid("2ce24f40-8f4c-4bfa-8fde-09d475783468")), null, false);
        List<Encounter> savedEncounters = encounterService.getEncounters(encounterSearchCriteria);

        log.info("print-obs-encounters");
        log.info(obsList);
        log.info(savedEncounters);

        if(!savedEncounters.isEmpty()) {
            Encounter encounter = savedEncounters.get(0);
            log.info("print-encounters");
            log.info(encounter);
            log.info(obsList);
            if(!obsList.isEmpty()) {
                log.info("print-obs");
                log.info(obsList);
                voidObsFound( encounter, obsList);
            }
            encounter.setObs(obsList);
            encounterService.saveEncounter(encounter);
            log.info("encounter saved");

        }else{
            log.info("print visit");
            Visit visit =createVisit(patient,startVisitDate,stopVisitDate,creator, pharmacyLocation);
            Encounter encounter = createEncounter(patient,startVisitDate,creator, pharmacyLocation);
            encounter.setVisit(visit);
            encounterService.saveEncounter(encounter);
            addObsToEncounter(patient,startVisitDate,stopVisitDate,obsList,creator);
        }
    }
    private void assignEncounterToGroupMember(Obs obs, Encounter encounter){
        if(obs.hasGroupMembers()){
            for (Obs o:obs.getGroupMembers()) {
                o.setEncounter(encounter);
            }
            obs.setEncounter(encounter);
        }else{
            obs.setEncounter(encounter);
        }
    }

    private void voidObsFound(Encounter encounter, Set<Obs> obsList){
        for (Obs o:obsList) {
            ObsService obsService = Context.getObsService();
            Concept concept = o.getConcept();
            List<Obs> obsListToIgnore = obsService.getObservationsByPersonAndConcept(encounter.getPatient(), concept);
            for (Obs obsToVoid : obsListToIgnore) {
                if (obsToVoid.getEncounter() == encounter) {
                    obsService.voidObs(obsToVoid, "Observation has been replaced or updated.");
                }
            }
            assignEncounterToGroupMember(o,encounter);
        }
    }

    private void savedSyncTask(Map resultMap) {
        SyncTask syncTask = new SyncTask();
        syncTask.setActionCompleted(true);
        syncTask.setDateSent(new Date());
        syncTask.setSyncTaskType(syncTaskType);
        syncTask.setCreator(Context.getUserService().getUser(1));
        syncTask.setSentToUrl(syncTaskType.getUrl());

        if (!resultMap.isEmpty() && resultMap.get("responseCode") != null) {
            syncTask.setStatusCode(Integer.parseInt(resultMap.get("responseCode").toString()));
            syncTask.setStatus(resultMap.get("responseMessage").toString());
        }

        syncTask.setRequireAction(false);
        syncTask.setSyncTask("ART Access receive date as of " + new Date());
        ugandaEMRSyncService.saveSyncTask(syncTask);

        syncGlobalProperties.setGlobalProperty(GP_ART_ACCESS_LAST_SYNC_DATE,LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
