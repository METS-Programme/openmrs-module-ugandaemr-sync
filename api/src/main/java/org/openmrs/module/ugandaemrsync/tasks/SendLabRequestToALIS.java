package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.api.impl.UgandaEMRSyncServiceImpl;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.util.UgandaEMRSyncUtil;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.openmrs.module.ugandaemrsync.server.SyncConstant.*;

/**
 * Send lab request to ALIS
 */

public class SendLabRequestToALIS extends AbstractTask {

    protected Log log = LogFactory.getLog(SendLabRequestToALIS.class);

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

        SyncTaskType syncTaskType = ugandaEMRSyncService.getSyncTaskTypeByUUID(OTHER_TESTS_SYNC_TYPE_UUID);

        for (Order order : orderList) {
            SyncTask syncTask = ugandaEMRSyncService.getSyncTaskBySyncTaskId(order.getAccessionNumber());
            if (syncTask == null) {
                Map<String, String> dataOutput = generateLabFHIROrderTestRequestBody((TestOrder) order, VL_SEND_SAMPLE_FHIR_JSON_STRING);
                String json = dataOutput.get("json");
                try {
                    Map map = ugandaEMRHttpURLConnection.sendPostBy(syncTaskType.getUrl(), syncTaskType.getUrlUserName(), syncTaskType.getUrlPassword(), "", json, false);
                    if ((map != null) && UgandaEMRSyncUtil.getSuccessCodeList().contains(map.get("responseCode"))) {
                        SyncTask newSyncTask = new SyncTask();
                        newSyncTask.setDateSent(new Date());
                        newSyncTask.setCreator(Context.getUserService().getUser(1));
                        newSyncTask.setSentToUrl(syncTaskType.getUrl());
                        newSyncTask.setRequireAction(true);
                        newSyncTask.setActionCompleted(false);
                        newSyncTask.setSyncTask(order.getAccessionNumber());
                        newSyncTask.setStatusCode((Integer) map.get("responseCode"));
                        newSyncTask.setStatus("SUCCESS");
                        newSyncTask.setSyncTaskType(ugandaEMRSyncService.getSyncTaskTypeByUUID(OTHER_TESTS_SYNC_TYPE_UUID));
                        ugandaEMRSyncService.saveSyncTask(newSyncTask);
                    }
                } catch (Exception e) {
                    log.error("Faied to create sync task",e);
                }
            }
        }
    }

    /**
     * Gererates FHIR MESSAGE Basing On Order To Lab That is refereed to Reference Lab
     *
     * @param testOrder
     * @param jsonFHIRMap
     * @return
     */
    public Map<String, String> generateLabFHIROrderTestRequestBody(TestOrder testOrder, String jsonFHIRMap) {
        Map<String, String> jsonMap = new HashMap<>();
        UgandaEMRSyncService ugandaEMRSyncService = new UgandaEMRSyncServiceImpl();
        String filledJsonFile = "";
        if (testOrder != null) {

            // need to aline these with the servicerequestFHIR json from ALIS

            String healthCenterName = ugandaEMRSyncService.getHealthCenterName();
            String healthCenterCode = ugandaEMRSyncService.getHealthCenterCode();
            String requestType = proccessMappings(testOrder.getConcept());
            String sourceSystem = "UgandaEMR";
            String patientARTNO = ugandaEMRSyncService.getPatientIdentifier(testOrder.getPatient(),PATIENT_IDENTIFIER_TYPE);
            String sampleID = testOrder.getAccessionNumber();
            String sampleCollectionDate = testOrder.getEncounter().getEncounterDatetime().toString();
            String clinicianNames = testOrder.getOrderer().getName();
            String labTechNames = testOrder.getCreator().getPersonName().getFullName();
            String labTechContact = "None";
            String ordererContact = "None";

            if (getProviderAttributeValue(Objects.requireNonNull(getProviderAppributesFromPerson(testOrder.getCreator().getPerson()))) != null) {
                labTechContact = getProviderAttributeValue(Objects.requireNonNull(getProviderAppributesFromPerson(testOrder.getCreator().getPerson())));
            }

            String obsSampleType = testOrder.getSpecimenSource().getName().getName();
            if (getProviderAttributeValue(testOrder.getOrderer().getActiveAttributes()) != null) {
                ordererContact = getProviderAttributeValue(testOrder.getOrderer().getActiveAttributes());
            }

            filledJsonFile = String.format(jsonFHIRMap, healthCenterCode, healthCenterName, requestType, sourceSystem, patientARTNO, sampleID, obsSampleType, sampleCollectionDate, labTechNames, labTechContact, clinicianNames, ordererContact, "CPHL");
        }
        jsonMap.put("json", filledJsonFile);
        System.out.print(filledJsonFile);
        return jsonMap;
    }

    private String proccessMappings(Concept concept) {
        for (ConceptMap conceptMap : concept.getConceptMappings()) {
            return conceptMap.getConceptReferenceTerm().getCode();
        }
        return null;
    }

    private String getProviderByEncounterRole(Encounter encounter, String encounterRoleName) {
        for (EncounterProvider provider : encounter.getActiveEncounterProviders()) {
            if (provider.getEncounterRole().getName() == encounterRoleName) {
                return provider.getProvider().getName();
            }
        }
        return null;
    }

    public List<Order> getOrders() throws IOException, ParseException {
        OrderService orderService = Context.getOrderService();
        List<Order> orders = new ArrayList<>();
        List list = Context.getAdministrationService().executeSQL(LAB_ORDERS_QUERY, true);
        if (list.size() > 0) {
            for (Object o : list) {
                Order order = orderService.getOrder(Integer.parseUnsignedInt(((ArrayList) o).get(0).toString()));
                if (order.getAccessionNumber() != null && order.isActive()) {
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    private String getProviderAttributeValue(Collection<ProviderAttribute> providerAttributes) {
        for (ProviderAttribute providerAttribute : providerAttributes) {
            if (providerAttribute.getAttributeType().getName().equals("Phone Number")) {
                return providerAttribute.getValue().toString();
            }

        }
        return null;
    }

    private Collection<ProviderAttribute> getProviderAppributesFromPerson(Person person) {
        List<Provider> providers = (List<Provider>) Context.getProviderService().getProvidersByPerson(person);
        if (providers != null) {
            return providers.get(0).getActiveAttributes();
        }
        return null;
    }

}
