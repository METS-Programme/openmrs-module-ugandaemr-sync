package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.api.impl.UgandaEMRSyncServiceImpl;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.server.SyncConstant;
import org.openmrs.module.ugandaemrsync.util.UgandaEMRSyncUtil;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.openmrs.module.ugandaemrsync.server.SyncConstant.*;

/**
 * Posts Viral load PROGRAM data to the central server
 */

public class SendViralLoadProgramDataToCentralServerTask extends AbstractTask {

    protected Log log = LogFactory.getLog(SendViralLoadProgramDataToCentralServerTask.class);

    @Override
    public void execute() {
        UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        List<Order> orderList = new ArrayList<>();

        if (!ugandaEMRHttpURLConnection.isConnectionAvailable()) {
            return;
        }

        try {
            orderList = getOrders();
        } catch (IOException e) {
            log.error("Failed to get orders", e);
        } catch (ParseException e) {
            log.error("Failed to pass orders to list", e);
        }

        SyncTaskType syncTaskType = ugandaEMRSyncService.getSyncTaskTypeByUUID(VL_PROGRAM_DATA_SYNC_TYPE_UUID);

        for (Order order : orderList) {
            SyncTask syncTask = ugandaEMRSyncService.getSyncTaskBySyncTaskId(order.getAccessionNumber());
            if (syncTask == null) {
                Map<String, String> dataOutput = generateVLProgramDataFHIRBody((TestOrder) order, VL_SEND_PROGRAM_DATA_FHIR_JSON_STRING);
                String json = dataOutput.get("json");

                try {
                    Map map = ugandaEMRHttpURLConnection.sendPostBy(syncTaskType.getUrl(), syncTaskType.getUrlUserName(), syncTaskType.getUrlPassword(), "", json, false);
                    if ((map != null) && UgandaEMRSyncUtil.getSuccessCodeList().contains(map.get("responseCode"))) {
                        SyncTask newSyncTask = new SyncTask();
                        newSyncTask.setDateSent(new Date());
                        newSyncTask.setCreator(Context.getUserService().getUser(1));
                        newSyncTask.setSentToUrl(syncTaskType.getUrl());
                        newSyncTask.setRequireAction(true);
                        newSyncTask.setActionCompleted(true);
                        newSyncTask.setSyncTask(order.getAccessionNumber());
                        newSyncTask.setStatusCode((Integer) map.get("responseCode"));
                        newSyncTask.setStatus("SUCCESS");
                        newSyncTask.setSyncTaskType(ugandaEMRSyncService.getSyncTaskTypeByUUID(VL_PROGRAM_DATA_SYNC_TYPE_UUID));
                        ugandaEMRSyncService.saveSyncTask(newSyncTask);
                    }
                } catch (Exception e) {
                    log.error("Failed to create sync task",e);
                }
            }
        }
    }


    public Map<String, String> generateVLProgramDataFHIRBody(TestOrder testOrder, String jsonFHIRMap) {
        Map<String, String> jsonMap = new HashMap<>();
        UgandaEMRSyncService ugandaEMRSyncService = new UgandaEMRSyncServiceImpl();
        String filledJsonFile = "";
        if (testOrder != null) {
            AdministrationService administrationService = Context.getAdministrationService();

            String healthCenterCode = ugandaEMRSyncService.getHealthCenterCode();
            String otherID= "";
            Patient patient = testOrder.getPatient();
            Date date_activated = testOrder.getDateActivated();
            String patientARTNO = ugandaEMRSyncService.getPatientIdentifier(testOrder.getPatient(),PATIENT_IDENTIFIER_TYPE);
            String patientOpenMRSID = ugandaEMRSyncService.getPatientIdentifier(testOrder.getPatient(),OPENMRS_IDENTIFIER_TYPE_UUID);
            String patientANCID = ugandaEMRSyncService.getPatientIdentifier(testOrder.getPatient(),ANC_IDENTIFIER_TYPE_UUID);
            String patientNATIONALID = ugandaEMRSyncService.getPatientIdentifier(testOrder.getPatient(),NATIONAL_ID_IDENTIFIER_TYPE_UUID);
            String patientPNC_ID= ugandaEMRSyncService.getPatientIdentifier(testOrder.getPatient(),PNC_IDENTIFIER_TYPE_UUID);
            String sampleID = testOrder.getAccessionNumber();
            String gender = patient.getGender();
            List current_regimenList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),90315,date_activated),true);


            String current_regimen="";
            if(current_regimenList.size()>0) {
                ArrayList regimenList = (ArrayList) current_regimenList.get(0);
                int regimen_code = Integer.parseInt(regimenList.get(0).toString());
                current_regimen = Context.getConceptService().getConcept(regimen_code).getName().getName();
            }

            List obs_dsdmList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),165143,date_activated),true);

            String dsdm="";
            if(obs_dsdmList.size()>0){
                ArrayList myList = (ArrayList) obs_dsdmList.get(0);
                int dsdm_code = Integer.parseInt(myList.get(0).toString());
                dsdm = Context.getConceptService().getConcept(dsdm_code).getName().getName();
            }

            List obs_adherenceList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),90221,date_activated),true);
            String adherence="";
            int adherence_code;
            if(obs_adherenceList.size()>0){
                ArrayList myList = (ArrayList) obs_adherenceList.get(0);
                adherence_code =Integer.parseInt(myList.get(0).toString());
                if(adherence_code==90156){
                    adherence = "Good >=95%";
                } else if (adherence_code==90157) {
                    adherence = "Fair 85-94%";
                }else if(adherence_code==90158){
                    adherence = "Poor <85%";
                }
            }

            List obs_durationList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_numeric", patient.getPatientId(),99025,date_activated),true);


            String duration_string="";
            if(obs_durationList.size()>0){
                ArrayList myList = (ArrayList) obs_durationList.get(0);
                 Double duration = Double.parseDouble(myList.get(0).toString());
                 if(duration==null){
                     duration_string="";
                 }else if(duration >=60){
                     duration_string=">5yrs";
                 }else if(duration >=24 && duration < 60 ){
                     duration_string="2- 5yrs";
                 }else if(duration >=12 && duration < 24 ){
                     duration_string="1-2yrs";
                 }else if(duration >=6 && duration < 12){
                     duration_string="6 months - <1yr";
                 }else if( duration < 6){
                     duration_string="< 6months";
                 }
            }

            List obs_pregnantList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),90041,date_activated),true);

            Boolean pregnant=false;
            Boolean breastfeeding=false;
            if(obs_pregnantList.size()>0){
                ArrayList myList = (ArrayList) obs_pregnantList.get(0);
                int preg_status = Integer.parseInt(myList.get(0).toString());
              if(preg_status==1065){
                  pregnant= true;
              }else if(preg_status==99601){
                  breastfeeding = true;
              }
            }

            List obs_tbList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),90216,date_activated),true);

            Boolean hasActiveTB = false;
            String tb_phase="";
            List<Integer> diagnosed_concepts = Arrays.asList(165295, 165296, 165297, 165298, 165299,165300);
            if(obs_tbList.size()>0){
                ArrayList myList = (ArrayList) obs_tbList.get(0);
                int tb_status_answer =Integer.parseInt(myList.get(0).toString());
              if(tb_status_answer==90071){
                  tb_phase= "Continuation Phase";
                  hasActiveTB =true;
              }else if (diagnosed_concepts.contains(tb_status_answer) ){
                  tb_phase= "Initiation Phase";
                  hasActiveTB=true;
              }
            }

            List artStartList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_datetime", patient.getPatientId(),99161,date_activated),true);
            Date artStartDate = null;
            if(artStartList.size()>0){
                ArrayList myList = (ArrayList) artStartList.get(0);
                artStartDate = (Date) myList.get(0);
            }

            List obs_indication_for_VL = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),168689,date_activated),true);

            String vl_testing_for="";
            if(obs_dsdmList.size()>0){
                ArrayList myList = (ArrayList) obs_indication_for_VL.get(0);
                int vl_indicator_code = Integer.parseInt(myList.get(0).toString());
                vl_testing_for = Context.getConceptService().getConcept(vl_indicator_code).getName().getName();
            }

            List obs_WHOList = administrationService.executeSQL(String.format(Latest_obs_of_Person,"value_coded", patient.getPatientId(),90203,date_activated),true);

            String who_code = "";
            String who_display = "";
            if(obs_WHOList.size()>0){
                ArrayList myList = (ArrayList) obs_WHOList.get(0);
                int who_stage_concept =(Integer)myList.get(0);

                switch (who_stage_concept) {
                    case 90033:
                        //1
                        who_code="737378009";
                        who_display="WHO 2007 HIV infection clinical stage 1";
                        break;
                    case 90034:
                        //2
                        who_code="737379001";
                        who_display="WHO 2007 HIV infection clinical stage 2";
                        break;
                    case 90035:
                        //3
                        who_code="737380003";
                        who_display="WHO 2007 HIV infection clinical stage 3";
                        break;
                    case 90036:
                        //4
                        who_code="737381004";
                        who_display="WHO 2007 HIV infection clinical stage 4";
                        break;
                    case 90293:
                        //T1
                        who_code="737378009";
                        who_display="WHO 2007 HIV infection clinical stage 1";
                        break;
                    case 90294:
                        //T2
                        who_code="737379001";
                        who_display="WHO 2007 HIV infection clinical stage 2";
                        break;
                    case 90295:
                        //T3
                        who_code="737380003";
                        who_display="WHO 2007 HIV infection clinical stage 3";
                        break;
                    case 90296:
                        //T4
                        who_code="737381004";
                        who_display="WHO 2007 HIV infection clinical stage 4";
                        break;
                    default:
                        who_code="737378009";
                        who_display="WHO 2007 HIV infection clinical stage 1";
                        break;
                }
            }

            String regimenline = getRegimenLineOfPatient(patient);
            String line_body ="";
            if(regimenline=="first"){
               line_body= firstLineBody;
            }else if (regimenline=="second"){
               line_body=secondLineBody;
            }else if(regimenline=="third"){
               line_body=thirdLineBody;
            }

            filledJsonFile = String.format(jsonFHIRMap,patientARTNO,sampleID,patientARTNO,patientOpenMRSID,patientNATIONALID,patientANCID,otherID,patientPNC_ID,gender, patient.getBirthdate(),healthCenterCode,patient.getAge(),artStartDate,who_code,who_display,duration_string,pregnant,breastfeeding,hasActiveTB,tb_phase,adherence,dsdm,vl_testing_for,line_body,current_regimen);
        }
        jsonMap.put("json", filledJsonFile);
        return jsonMap;
    }


    public List<Order> getOrders() throws IOException, ParseException {
        OrderService orderService = Context.getOrderService();
        List<Order> orders = new ArrayList<>();
        List list = Context.getAdministrationService().executeSQL(VIRAL_LOAD_ORDERS_QUERY, true);
        if (list.size() > 0) {
            for (Object o : list) {
                Order order = orderService.getOrder(Integer.parseUnsignedInt(((ArrayList) o).get(0).toString()));
                if (order.getAccessionNumber() != null && order.isActive() && order.getInstructions().equalsIgnoreCase("REFER TO cphl")) {
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    private String getRegimenLineOfPatient(Patient patient){
        int patientno = patient.getPatientId();
        String line="";
        String firstLineQuery = String.format(REGIMEN_LINE_QUERY,"ab6d1f1d-fcf6-4255-8b6f-2bf8959ad8f2",patientno);

        List firstLineResults = Context.getAdministrationService().executeSQL(firstLineQuery,true);
        if(firstLineResults.size()>0 ){
            line ="first";
        }else {
            String secondLineQuery = String.format(REGIMEN_LINE_QUERY, "9a42a3ad-d8a4-4f2e-9fa0-04d5f2e6436e", patientno);
            List secondLineResults = Context.getAdministrationService().executeSQL(secondLineQuery, true);
            if (secondLineResults.size() > 0) {
                line = "second";
            }

            String thirdLineQuery = String.format(REGIMEN_LINE_QUERY,"5d2d0e7e-69a6-408a-b5ce-8d93fb72bc21",patientno);
            List thirdLineResults = Context.getAdministrationService().executeSQL(thirdLineQuery,true);
            if(thirdLineResults.size()>0 ){
                line ="third";
            }
        }

        return line;
    }


}
