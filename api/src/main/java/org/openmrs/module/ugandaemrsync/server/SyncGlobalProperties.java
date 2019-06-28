package org.openmrs.module.ugandaemrsync.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;

import static org.openmrs.module.ugandaemrsync.server.SyncConstant.*;

/**
 * Created by lubwamasamuel on 10/11/2016.
 */
public class SyncGlobalProperties {
	
	public SyncGlobalProperties() {
	}
	
	protected Log log = LogFactory.getLog(getClass());
	
	public void setSyncFacilityProperties() {
		/**
		 * Setting Server IP Address
		 */
		
		if (getGlobalProperty(SERVER_PROTOCOL) == null) {
			setGlobalProperty(SERVER_PROTOCOL, SERVER_PROTOCOL_PLACE_HOLDER);
			log.info("Default Server IP is Set");
		}
		
		if (getGlobalProperty(INITIAL_SYNC) == null) {
			setGlobalProperty(INITIAL_SYNC, INITIAL_SYNC_PLACE_HOLDER);
			log.info("Default Initial Sync State is Set");
		}
		
		if (getGlobalProperty(SERVER_IP) == null) {
			setGlobalProperty(SERVER_IP, SERVER_IP_PLACE_HOLDER);
			log.info("Default Server IP is Set");
		}
		
		if (getGlobalProperty(HEALTH_CENTER_SYNC_ID) == null) {
			setGlobalProperty(HEALTH_CENTER_SYNC_ID, HEALTH_CENTER_SYNC_ID_PLACE_HOLDER);
			log.info("Place Holder for HC Sync ID is set");
		}
		
		if (getGlobalProperty(LAST_SYNC_DATE) == null) {
			setGlobalProperty(LAST_SYNC_DATE, LAST_SYNC_DATE_PLACE_HOLDER);
			log.info("Place Holder for last sync date is set");
		}
		
		if (getGlobalProperty(MAX_NUMBER_OF_ROWS) == null) {
			setGlobalProperty(MAX_NUMBER_OF_ROWS, MAX_NUMBER_OF_ROWS_PLACE_HOLDER);
			log.info("Place Holder for max number of row is set");
		}

		if (getGlobalProperty(SERVER_URL) == null) {
			setGlobalProperty(SERVER_URL, SERVER_URL_PLACE_HOLDER);
			log.info("Place Holder server URL is set");
		}
	}
	
	public GlobalProperty setGlobalProperty(String property, String propertyValue) {
		GlobalProperty globalProperty = new GlobalProperty();
		
		globalProperty.setProperty(property);
		globalProperty.setPropertyValue(propertyValue);
		
		return Context.getAdministrationService().saveGlobalProperty(globalProperty);
	}
	
	public String getGlobalProperty(String property) {
		return Context.getAdministrationService().getGlobalProperty(property);
	}
	
}
