package org.openmrs.module.ugandaemrsync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.FhirProfileExportService;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.ugandaemrsync.security.Secured;
import org.openmrs.module.ugandaemrsync.security.SyncPrivileges;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for exporting FHIR profiles and task types to JSON format
 */
@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/ugandaemrsync/export")
@Secured(authenticated = true)
public class SyncExportController {
	private static final Log log = LogFactory.getLog(SyncExportController.class);

	private FhirProfileExportService getExportService() {
		return Context.getService(FhirProfileExportService.class);
	}

	@RequestMapping(value = "/profiles/all", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = SyncPrivileges.VIEW_FHIR_PROFILES)
	@ResponseBody
	public String exportAllProfiles(HttpServletResponse response) {
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\"ugandaemr-profiles.json\"");
			return getExportService().exportAllProfilesToJson();
		} catch (Exception e) {
			log.error("Error exporting all profiles", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export profiles: " + e.getMessage() + "\"}";
		}
	}

	@RequestMapping(value = "/profiles/{uuid}", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = SyncPrivileges.VIEW_FHIR_PROFILES)
	@ResponseBody
	public String exportProfile(@PathVariable String uuid, HttpServletResponse response) {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			SyncFhirProfile profile = service.getSyncFhirProfileByUUID(uuid);

			if (profile == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return "{\"error\": \"Profile not found with UUID: " + uuid + "\"}";
			}

			return getExportService().exportProfileToJson(profile);
		} catch (Exception e) {
			log.error("Error exporting profile: " + uuid, e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export profile: " + e.getMessage() + "\"}";
		}
	}

	@RequestMapping(value = "/profiles/batch", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = SyncPrivileges.VIEW_FHIR_PROFILES)
	@ResponseBody
	public String exportProfilesByUuids(@RequestParam("uuids") String uuids, HttpServletResponse response) {
		try {
			if (uuids == null || uuids.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "{\"error\": \"uuids parameter is required\"}";
			}

			List<String> uuidList = Arrays.asList(uuids.split(","));
			return getExportService().exportProfilesByUuids(uuidList);
		} catch (Exception e) {
			log.error("Error exporting profiles by UUIDs", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export profiles: " + e.getMessage() + "\"}";
		}
	}

	@RequestMapping(value = "/tasktypes/all", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = SyncPrivileges.VIEW_SYNC_TASK_TYPES)
	@ResponseBody
	public String exportAllTaskTypes(HttpServletResponse response) {
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\"ugandaemr-tasktypes.json\"");
			return getExportService().exportAllTaskTypesToJson();
		} catch (Exception e) {
			log.error("Error exporting all task types", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export task types: " + e.getMessage() + "\"}";
		}
	}

	@RequestMapping(value = "/tasktypes/{uuid}", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = SyncPrivileges.VIEW_SYNC_TASK_TYPES)
	@ResponseBody
	public String exportTaskType(@PathVariable String uuid, HttpServletResponse response) {
		try {
			UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
			SyncTaskType taskType = service.getSyncTaskTypeByUUID(uuid);

			if (taskType == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return "{\"error\": \"Task type not found with UUID: " + uuid + "\"}";
			}

			return getExportService().exportTaskTypeToJson(taskType);
		} catch (Exception e) {
			log.error("Error exporting task type: " + uuid, e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export task type: " + e.getMessage() + "\"}";
		}
	}

	@RequestMapping(value = "/tasktypes/batch", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = SyncPrivileges.VIEW_SYNC_TASK_TYPES)
	@ResponseBody
	public String exportTaskTypesByUuids(@RequestParam("uuids") String uuids, HttpServletResponse response) {
		try {
			if (uuids == null || uuids.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "{\"error\": \"uuids parameter is required\"}";
			}

			List<String> uuidList = Arrays.asList(uuids.split(","));
			return getExportService().exportTaskTypesByUuids(uuidList);
		} catch (Exception e) {
			log.error("Error exporting task types by UUIDs", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export task types: " + e.getMessage() + "\"}";
		}
	}

	@RequestMapping(value = "/all", method = RequestMethod.GET, produces = "application/json")
	@Secured(privilege = {SyncPrivileges.VIEW_FHIR_PROFILES, SyncPrivileges.VIEW_SYNC_TASK_TYPES})
	@ResponseBody
	public String exportAllConfiguration(HttpServletResponse response) {
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\"ugandaemr-sync-config.json\"");
			return getExportService().exportAllConfigurationToJson();
		} catch (Exception e) {
			log.error("Error exporting all configuration", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"error\": \"Failed to export configuration: " + e.getMessage() + "\"}";
		}
	}
}
