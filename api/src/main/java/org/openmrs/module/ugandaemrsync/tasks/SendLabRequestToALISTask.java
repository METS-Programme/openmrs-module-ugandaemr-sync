package org.openmrs.module.ugandaemrsync.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord;
import org.openmrs.scheduler.tasks.AbstractTask;

public class SendLabRequestToALISTask extends AbstractTask {
    Log log = LogFactory.getLog(SyncFHIRRecord.class);
    @Override
    public void execute() {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        SyncFhirProfile syncFhirProfile = ugandaEMRSyncService.getSyncFhirProfileByScheduledTaskName("Send Lab Request to ALIS Task");
        SyncFHIRRecord syncFHIRRecord = new SyncFHIRRecord();

        if(syncFhirProfile.getProfileEnabled()) {
            log.info("Generating Resources and cases for Profile " + syncFhirProfile.getName());

            syncFHIRRecord.generateCaseBasedFHIRResourceBundles(syncFhirProfile);
        }

    }
}
