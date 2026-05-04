package org.openmrs.module.ugandaemrsync.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.ViralLoadUploadResult;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.ugandaemrsync.security.Secured;
import org.openmrs.module.ugandaemrsync.security.SyncPrivileges;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * REST Controller for uploading viral load results from CSV files.
 * Uses Lab Request Encounter Type (UUID: cbf01392-ca29-11e9-a32f-2a2ae2dbcce4)
 * for encounter lookup instead of HIV encounter.
 */
@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/ugandaemrsync/viralload")
@Secured(authenticated = true)
public class ViralLoadUploadController {

    private static final Log log = LogFactory.getLog(ViralLoadUploadController.class);

    private UgandaEMRSyncService getSyncService() {
        return Context.getService(UgandaEMRSyncService.class);
    }

    /**
     * Upload and process viral load CSV file.
     * Searches for Lab Request encounters on the viral load collection date
     * and adds viral load results to existing encounters.
     *
     * @param file CSV file containing viral load results
     * @return Upload result with statistics and errors
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured(privilege = SyncPrivileges.MANAGE_FHIR_PROFILES)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadViralLoadResults(
            @RequestParam("file") MultipartFile file) {

        ViralLoadUploadResult result = new ViralLoadUploadResult();

        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("No file uploaded");
                return ResponseEntity.badRequest().body(createResponseMap(result));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                result.setSuccess(false);
                result.setErrorMessage("Invalid file format. Only CSV files are allowed");
                return ResponseEntity.badRequest().body(createResponseMap(result));
            }

            // Process the CSV file using service layer
            result = getSyncService().processViralLoadCSV(file.getInputStream());

            return ResponseEntity.ok(createResponseMap(result));

        } catch (Exception e) {
            log.error("Failed to process viral load upload", e);
            result.setSuccess(false);
            result.setErrorMessage("Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(createResponseMap(result));
        }
    }

    /**
     * Get template for viral load CSV upload format
     */
    @RequestMapping(value = "/template", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(privilege = SyncPrivileges.VIEW_FHIR_PROFILES)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUploadTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("description", "Viral Load CSV Upload Template");
        template.put("format", "CSV");
        template.put("requiredColumns", Arrays.asList(
            "facility_dhis2_id",
            "facility_name",
            "date_collected",
            "patient_art_id",
            "vl_result_numeric",
            "vl_result_alpha"
        ));
        template.put("exampleRow", Arrays.asList(
            "facility-uuid-123",
            "Health Center Name",
            "2024-01-15",
            "12345-67890",
            "750",
            "Detected"
        ));
        template.put("notes", Arrays.asList(
            "Date format: yyyy-MM-dd or yyyy-MM-dd HH:mm:ss",
            "Patient ART ID must exist in the system",
            "Facility DHIS2 ID must match the configured facility",
            "VL result can be numeric (copies/mL) or alpha (Detected/Not Detected)"
        ));

        return ResponseEntity.ok(template);
    }

    /**
     * Validate uploaded CSV without processing
     */
    @RequestMapping(value = "/validate", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured(privilege = SyncPrivileges.VIEW_FHIR_PROFILES)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateCSV(
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> validationResult = new HashMap<>();

        try {
            if (file == null || file.isEmpty()) {
                validationResult.put("valid", false);
                validationResult.put("errors", Arrays.asList("No file uploaded"));
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("validation", validationResult);
                return ResponseEntity.badRequest().body(wrapper);
            }

            List<String> errors = getSyncService().validateCSVFormat(file.getInputStream());
            Map<String, Object> response = new HashMap<>();
            response.put("valid", errors.isEmpty());
            response.put("errors", errors);

            if (errors.isEmpty()) {
                response.put("message", "CSV file format is valid");
            }

            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("validation", response);
            return ResponseEntity.ok(wrapper);

        } catch (Exception e) {
            log.error("Failed to validate CSV file", e);
            validationResult.put("valid", false);
            validationResult.put("errors", Arrays.asList("Validation failed: " + e.getMessage()));
            Map<String, Object> response = new HashMap<>();
            response.put("validation", validationResult);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Create response map from result
     */
    private Map<String, Object> createResponseMap(ViralLoadUploadResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getErrorMessage());
        response.put("healthCenterName", result.getHealthCenterNameValidator());
        response.put("processedCount", result.getProcessedCount());
        response.put("successCount", result.getSuccessCount());
        response.put("noEncounterFound", result.getNoEncounterFound());
        response.put("noPatientFound", result.getNoPatientFound());
        response.put("patientResultNotReleased", result.getPatientResultNotReleased());

        if (!result.isSuccess()) {
            response.put("error", result.getErrorMessage());
        }

        return response;
    }
}
