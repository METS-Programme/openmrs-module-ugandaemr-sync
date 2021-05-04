/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ugandaemrsync.fragment.controller;

import org.json.JSONObject;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.ugandaemrsync.server.SyncDataRecord;
import org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.page.PageModel;

import java.util.List;
import java.util.Map;

import static org.openmrs.module.ugandaemrsync.server.SyncConstant.FHIR_FILTER_OBJECT_STRING;

/**
 *  * Controller for a fragment that shows all users  
 */
public class GenerateInitialFHIRBundleDataFragmentController {

	SyncDataRecord syncDataRecord = new SyncDataRecord();

	public void controller(UiSessionContext sessionContext, FragmentModel model) {
	}

	public void get(@SpringBean PageModel pageModel) throws Exception {
		SyncFHIRRecord syncFHIRRecord = new SyncFHIRRecord();
		List<Map> totals;


		syncFHIRRecord.sendFHIRBundleObject("Patient",null);
		syncFHIRRecord.sendFHIRBundleObject("Practitioner",null);
		syncFHIRRecord.sendFHIRBundleObject("Observation",null);
		syncFHIRRecord.sendFHIRBundleObject("Encounter",null);
		JSONObject patientFilterObject = new JSONObject(FHIR_FILTER_OBJECT_STRING);
		//pageModel.put("persons", totals);


	}

}
