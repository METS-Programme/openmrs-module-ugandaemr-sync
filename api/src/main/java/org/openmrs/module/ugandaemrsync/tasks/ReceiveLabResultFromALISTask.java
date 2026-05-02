package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRHttpURLConnection;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.exception.UgandaEMRSyncException;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.validation.ValidationUtils;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import static org.openmrs.module.ugandaemrsync.server.SyncConstant.LAB_RESULT_PULL_SYNC_TASK_TYPE_UUID;
import static org.openmrs.module.ugandaemrsync.server.SyncConstant.LAB_ORDER_QUERY;

public class ReceiveLabResultFromALISTask extends AbstractTask {
    protected final Log log = LogFactory.getLog(ReceiveLabResultFromALISTask.class);

    @Override
    public void execute() {
        UgandaEMRHttpURLConnection ugandaEMRHttpURLConnection = new UgandaEMRHttpURLConnection();

        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);

        if (!ugandaEMRHttpURLConnection.isConnectionAvailable()) {
            return;
        }

        for (SyncTask syncTask : ugandaEMRSyncService.getIncompleteActionSyncTask(LAB_RESULT_PULL_SYNC_TASK_TYPE_UUID)) {

            Order order;
            try {
                order = getOrder(syncTask.getSyncTask());
            } catch (UgandaEMRSyncException e) {
                log.error("Failed to get order for syncTask: " + syncTask.getSyncTask(), e);
                continue;
            }

            Map results = new HashMap();

            // Fetch lab results for the order
            if (order != null) {
                try {
                    results = ugandaEMRSyncService.requestLabResult(order, syncTask);
                } catch (Exception e) {
                    log.error("Failed to fetch lab results for order: " + order.getOrderNumber(), e);
                    continue;
                }
            }

            if (results != null && results.size() > 0 ) {
                Map reasonReference = (Map) results.get("reasonReference");
                ArrayList<Map> result = (ArrayList<Map>) reasonReference.get("result");
                //Save Lab Results
                if (order.getEncounter() != null) {
                   ugandaEMRSyncService.addTestResultsToEncounter("", order);
                    syncTask.setActionCompleted(true);
                    ugandaEMRSyncService.saveSyncTask(syncTask);
                    try {
                        Context.getOrderService().discontinueOrder(order, "Completed", new Date(), order.getOrderer(), order.getEncounter());
                    } catch (Exception e) {
                        log.error("Failed to discontinue order", e);
                    }
                }
            }
        }
    }

    public Order getOrder(String orderNumber) throws UgandaEMRSyncException {
        // Validate order number to prevent SQL injection
        ValidationUtils.requireNotEmpty("orderNumber", orderNumber);
        ValidationUtils.requireLength("orderNumber", orderNumber, 1, 255);
        ValidationUtils.requireNoSQLInjection("orderNumber", orderNumber);

        OrderService orderService = Context.getOrderService();

        // Use parameterized query to prevent SQL injection
        org.hibernate.SQLQuery sqlQuery = Context.getRegisteredComponent("sessionFactory", org.hibernate.SessionFactory.class)
                .getCurrentSession()
                .createSQLQuery(LAB_ORDER_QUERY);
        sqlQuery.setParameter("orderNumber", orderNumber);

        List list = sqlQuery.list();
        if (list.size() > 0) {
            for (Object o : list) {
                return orderService.getOrder(Integer.parseUnsignedInt(((ArrayList) o).get(0).toString()));
            }
        }
        return null;
    }
}