/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ugandaemrsync;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.ugandaemrsync.io.impl.FhirProfileImportServiceImpl;
import org.openmrs.module.ugandaemrsync.server.SyncGlobalProperties;
import org.openmrs.util.OpenmrsUtil;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class UgandaEMRSyncActivator extends BaseModuleActivator {

	private Log log = LogFactory.getLog(UgandaEMRSyncActivator.class);

	/**
	 * @see #started()
	 */
	public void started() {
		SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();

		try {
			log.info("Setting Global Properties For Sync Module");
			syncGlobalProperties.setSyncFacilityProperties();
		}
		catch (Exception e) {
			log.error("Failed to initialize sync module global properties", e);
			// Re-throw to prevent module from starting with broken configuration
			throw new RuntimeException("Failed to initialize UgandaEMR Sync module: " + e.getMessage(), e);
		}

		// Import configurations from directory on startup
		try {
			importConfigurationsOnStartup();
		}
		catch (Exception e) {
			log.warn("Failed to import configurations from directory (non-critical)", e);
			// Don't throw - module should still start even if import fails
		}

		log.info("Started UgandaemrSync");
	}

	/**
	 * Import profiles and task types from configuration directory
	 */
	private void importConfigurationsOnStartup() {
		try {
			// Try to import from classpath resources first (bundled with module)
			FhirProfileImportServiceImpl importService = new FhirProfileImportServiceImpl();
			boolean importedFromClasspath = importService.importConfigurationsFromClasspath();

			if (importedFromClasspath) {
				log.info("Successfully imported configurations from classpath resources");
			}

			// Always check for external configuration directory for overrides
			String configPath = OpenmrsUtil.getApplicationDataDirectory() + "/configuration";
			log.info("Checking for external configurations in: " + configPath);
			importService.importConfigurationsFromDirectory(configPath);

		}
		catch (Exception e) {
			log.error("Error importing configurations on startup", e);
		}
	}

	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		log.info("Shutdown UgandaemrSync");
	}

}
