# UgandaEMR Sync Configuration Import on Startup

## Overview

The UgandaEMR Sync module automatically imports sync profiles and task types from configuration files when the module starts. This enables configuration-as-code and easier deployment across multiple EMR instances.

## Configuration Locations

### Primary: Bundled with Module (Default)

**Location**: Packaged in module JAR at `api/src/main/resources/configuration/hie/`

These configurations are:
- ✅ Bundled with the module JAR
- ✅ Automatically imported on first startup
- ✅ Used as defaults for all installations
- ✅ Suitable for development and testing

### Secondary: External Configuration (Override)

**Location**: `{OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/`

External configurations:
- ✅ Override bundled defaults
- ✅ Used for production-specific settings
- ✅ Can contain actual credentials
- ✅ Optional - if not present, bundled configs are used

### Priority

1. External configurations (if present)
2. Bundled configurations (default)

## Directory Structure

```
{OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/
├── syncprofile/          # Sync FHIR profile configurations
│   ├── profile-art-access-integration.json
│   ├── profile-hiv-case-based-surveillance.json
│   ├── profile-send-lab-request-to-alis-task.json
│   ├── profile-ecbss-integration.json
│   ├── profile-hts-data.json
│   ├── profile-art-regimen-change.json
│   ├── profile-tb-data.json
│   ├── profile-hiv-exposed-infant-data.json
│   ├── profile-client-registry-integration.json
│   ├── profile-cross-border-integration.json
│   ├── profile-mortality-surveillance.json
│   └── profile-mpox-screening.json
└── synctasktype/         # Sync task type configurations
    ├── synctasktype-send-viral-load-to-cphl.json
    ├── synctasktype-receive-art-access-visits-data.json
    ├── synctasktype-request-viral-load-results-form-cphl.json
    ├── synctasktype-sync-data-to-shared-health-record.json
    ├── synctasktype-send-mer-reports.json
    ├── synctasktype-send-hmis-reports.json
    ├── synctasktype-send-appointmentreminder-reports.json
    ├── synctasktype-sent-sms-messages-from-central-server.json
    ├── synctasktype-send-vl-program-data-to-cphl.json
    ├── synctasktype-receive-results-from-alis.json
    ├── synctasktype-eafya-stock-integration.json
    └── synctasktype-sends-prescriptions-eafya.json
```

## Import Behavior

### On Module Startup

1. **Classpath Import First**: Module loads bundled configurations from JAR
2. **External Override**: If external directory exists, those configs override bundled ones
3. **Database Import**: Each configuration is imported into the database
4. **Create or Update**: 
   - New configurations are created
   - Existing configurations are updated
5. **Logging**: All import activity is logged

### Import Process

```java
// In UgandaEMRSyncActivator.started()
FhirProfileImportServiceImpl importService = new FhirProfileImportServiceImpl();

// Try bundled configurations first
boolean importedFromClasspath = importService.importConfigurationsFromClasspath();

// If external directory exists, override with those configs
if (externalConfigDir.exists()) {
    importService.importConfigurationsFromDirectory(externalConfigPath);
}
```

### Error Handling

- **Import Failures**: Logged but don't prevent module startup
- **Invalid Files**: Skipped with warnings
- **Database Errors**: Logged, module continues with other configs
- **Missing Directory**: Logged at info level, not an error

## Configuration File Format

### SyncProfile Format

```json
{
  "metadata": {
    "id": "profile-mpox-screening",
    "name": "MPOX Screening",
    "category": "DEFAULT",
    "description": "FHIR sync profile for MPOX Screening",
    "version": "1.0"
  },
  "configuration": {
    "name": "MPOX Screening",
    "resourceTypes": "Patient,Person,EpisodeOfCare,Encounter,Observation",
    "isCaseBasedProfile": false,
    "caseBasedPrimaryResourceType": "Encounter",
    "caseBasedPrimaryResourceTypeId": "09478ad9-ccc1-4cbe-9e55-473447984158",
    "resourceSearchParameter": "",
    "generateBundle": false,
    "numberOfResourcesInBundle": 100,
    "profileEnabled": false,
    "keepProfileIdentifierOnly": false,
    "durationToKeepSyncedResources": 1,
    "syncLimit": null,
    "searchable": false,
    "searchURL": "",
    "patientIdentifierType": "3",
    "patientIdentifierTypeName": "",
    "endpoint": {
      "url": "https://example.com/fhir",
      "authentication": "BASIC",
      "username": "user",
      "password": "{{CONFIGURE_PASSWORD}}",
      "token": "{{CONFIGURE_TOKEN}}"
    }
  }
}
```

### SyncTaskType Format

```json
{
  "metadata": {
    "id": "synctasktype-send-viral-load-to-cphl",
    "name": "Send Viral Load To CPHL",
    "description": "Sync task type for Send Viral Load To CPHL"
  },
  "configuration": {
    "name": "Send Viral Load To CPHL",
    "dataType": "org.openmrs.TestOrder",
    "dataTypeId": "315124004",
    "endpoint": {
      "url": "https://ugisl.mets.or.ug/vl/send",
      "username": "emrlabClient",
      "password": "{{CONFIGURE_PASSWORD}}",
      "token": "{{CONFIGURE_TOKEN}}"
    }
  }
}
```

## Security Considerations

### Placeholder Replacement

Configuration files use placeholders for sensitive data:
- `{{CONFIGURE_PASSWORD}}` - Replace with actual password
- `{{CONFIGURE_TOKEN}}` - Replace with actual API token

**For Development**: 
- Bundled configs with placeholders are fine
- Use test credentials

**For Production**:
- Create external configuration directory
- Replace placeholders with actual credentials
- Set restrictive file permissions
- Consider using environment variables or secret management

### File Permissions

```bash
# Set appropriate permissions for external configs
chmod 640 {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/*.json
chmod 640 {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/synctasktype/*.json
chown openmrs:openmrs {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/*.json
chown openmrs:openmrs {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/synctasktype/*.json
```

## Deployment Scenarios

### Scenario 1: Development/Test

**Use bundled configurations** - No setup required:

1. Build module: `mvn clean install`
2. Deploy to OpenMRS
3. Start OpenMRS
4. Configurations auto-import from JAR
5. Review and adjust in Module Administration UI

### Scenario 2: Production with Custom Endpoints

**Override specific configurations**:

1. **Create External Directory**:
   ```bash
   mkdir -p {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/
   mkdir -p {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/synctasktype/
   ```

2. **Copy Configs to Customize**:
   ```bash
   # Copy only what you need to customize
   cp api/src/main/resources/configuration/hie/syncprofile/profile-mpox-screening.json \
      {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/
   ```

3. **Customize**:
   ```bash
   # Update with production values
   vim {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/profile-mpox-screening.json
   ```

4. **Restart OpenMRS**

### Scenario 3: Production with All Custom Configs

**Full external configuration**:

1. **Create External Structure**:
   ```bash
   mkdir -p {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/{syncprofile,synctasktype}
   ```

2. **Copy All Configs**:
   ```bash
   cp -r api/src/main/resources/configuration/hie/* \
         {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/
   ```

3. **Customize All Files**:
   - Update all endpoints
   - Replace all placeholders
   - Adjust schedules and settings

4. **Set Permissions**:
   ```bash
   chmod -R 640 {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/
   chown -R openmrs:openmrs {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/
   ```

5. **Restart OpenMRS**

## Verifying Import

### Check Logs

```bash
# Look for import messages
tail -f /var/log/openmrs/openmrs.log | grep -i "import"

# Successful import looks like:
# INFO  - Imported profile: MPOX Screening
# INFO  - Imported task type: Send Viral Load To CPHL
```

### Verify in Database

```sql
-- Check profiles
SELECT sync_fhir_profile_id, name, task_group, profile_enabled 
FROM ugandaemr_sync_fhir_profile 
ORDER BY name;

-- Check task types
SELECT sync_task_type_id, name, data_type 
FROM ugandaemr_sync_task_type 
ORDER BY name;
```

### Verify in UI

1. Navigate to Module Administration
2. Find UgandaEMR Sync module
3. Open "Sync Profiles" - should see 12+ profiles
4. Open "Task Types" - should see 12 task types

## Troubleshooting

### Configurations Not Importing

**Symptom**: No profiles or task types after module starts

**Solutions**:
1. **Check Logs**: Look for import errors
2. **Verify Module**: Ensure correct version is deployed
3. **Database Check**: Verify tables exist and are empty
4. **Manual Import**: Use REST API to test import

```bash
# Check if import service is working
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@api/src/main/resources/configuration/hie/syncprofile/profile-mpox-screening.json"
```

### Partial Import

**Symptom**: Some configs import, others fail

**Solutions**:
1. **Check Logs**: Look for specific file errors
2. **Validate JSON**: Ensure files are valid JSON
3. **Check Permissions**: Verify file read permissions
4. **Database Constraints**: Check for unique constraint violations

```bash
# Validate all JSON files
find api/src/main/resources/configuration/hie -name "*.json" -exec jq empty {} \;
```

### External Configs Not Loading

**Symptom**: Changes to external configs not reflected

**Solutions**:
1. **Verify Path**: Check directory path is correct
2. **Check Permissions**: Ensure OpenMRS can read files
3. **Restart Required**: External configs only load on startup
4. **Check Logs**: Look for path errors in logs

```bash
# Verify directory exists
ls -la {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/

# Check OpenMRS can read
sudo -u openmrs ls {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/
```

## Current Status

### Bundled Configurations (Default)

**Profiles (12 from database + 2 examples = 14 total)**:
- ✅ ART Access Integration
- ✅ NATIONAL DATA WARE HOUSE
- ✅ Send Lab Request to ALIS Task
- ✅ eCBSS Integration
- ✅ HTS Data
- ✅ ART Regimen Change
- ✅ TB Data
- ✅ HIV Exposed Infant Data
- ✅ Client Registry Integration
- ✅ Cross Border Integration
- ✅ Mortality Surveillance
- ✅ MPOX Screening
- ✅ Example Profile (resource-based)
- ✅ Case Example Profile (case-based)

**Task Types (12 total)**:
- ✅ Send Viral Load To CPHL
- ✅ Receive ART Access Visits Data
- ✅ Request Viral Load Results form CPHL
- ✅ Sync Data to Shared Health Record
- ✅ Send MER Reports
- ✅ Send HMIS Reports
- ✅ Send AppointmentReminder Reports
- ✅ Sent SMS Messages from central server
- ✅ Send VL Program Data To CPHL
- ✅ Receive Results From ALIS
- ✅ eAFYA Stock Integration
- ✅ Sends Prescriptions eAFYA

### Import Components

**Services Created**:
- `FhirProfileImportService` - Interface for import operations
- `FhirProfileImportServiceImpl` - Implementation with classpath and directory support

**Classes Created**:
- `ImportResult`, `ImportPreview`, `ValidationResult`
- `ProfileSummary`, `ConflictInfo`

**Updated Files**:
- `UgandaEMRSyncActivator` - Added startup import logic

## Best Practices

1. **Development**: Use bundled configurations
2. **Production**: Use external configurations with real credentials
3. **Version Control**: Keep bundled configs in version control
4. **Documentation**: Document customizations
5. **Testing**: Test config changes in development first
6. **Backup**: Backup database before major changes
7. **Security**: Never commit actual credentials
8. **Monitoring**: Check logs after each deployment

## Next Steps

- [ ] Add encryption support for sensitive credentials
- [ ] Implement configuration versioning and rollback
- [ ] Add web UI for uploading/managing configurations
- [ ] Support for YAML format in addition to JSON
- [ ] Configuration validation before import
- [ ] Import/export via REST API endpoints
- [ ] Configuration diff viewer
- [ ] Rollback to previous configurations
