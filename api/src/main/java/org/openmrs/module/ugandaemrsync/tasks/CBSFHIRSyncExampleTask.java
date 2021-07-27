package org.openmrs.module.ugandaemrsync.tasks;

import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord;
import org.openmrs.scheduler.tasks.AbstractTask;

public class CBSFHIRSyncExampleTask extends AbstractTask {
    @Override
    public void execute() {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        SyncFhirProfile syncFhirProfile = ugandaEMRSyncService.getSyncFhirProfileByScheduledTaskName("CBS FHIR SYNC TEST");
        SyncFHIRRecord syncFHIRRecord = new SyncFHIRRecord();

        syncFHIRRecord.generateCaseBasedFHIRResourceBundles(syncFhirProfile);
    }
}
