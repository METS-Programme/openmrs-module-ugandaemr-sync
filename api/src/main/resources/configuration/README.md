# UgandaEMR Sync Configuration Files

This directory contains JSON configuration files for UgandaEMR Sync module profiles and task types.

## About This Directory

**Location**: `api/src/main/resources/configuration/hie/`

These configuration files are packaged with the module JAR and automatically imported on module startup. They provide default configurations for common FHIR sync profiles and task types.

## Directory Structure

```
configuration/
├── hie/
│   ├── syncprofile/          # FHIR sync profile configurations
│   │   ├── profile-art-access-integration.json
│   │   ├── profile-hiv-case-based-surveillance.json
│   │   ├── profile-send-lab-request-to-alis-task.json
│   │   ├── profile-ecbss-integration.json
│   │   ├── profile-hts-data.json
│   │   ├── profile-art-regimen-change.json
│   │   ├── profile-tb-data.json
│   │   ├── profile-hiv-exposed-infant-data.json
│   │   ├── profile-client-registry-integration.json
│   │   ├── profile-cross-border-integration.json
│   │   ├── profile-mortality-surveillance.json
│   │   ├── profile-mpox-screening.json
│   │   ├── profile-example-resource-based.json (example)
│   │   └── profile-example-case-based.json (example)
│   └── synctasktype/         # Task type configurations
│       ├── synctasktype-send-viral-load-to-cphl.json
│       ├── synctasktype-receive-art-access-visits-data.json
│       ├── synctasktype-request-viral-load-results-form-cphl.json
│       ├── synctasktype-sync-data-to-shared-health-record.json
│       ├── synctasktype-send-mer-reports.json
│       ├── synctasktype-send-hmis-reports.json
│       ├── synctasktype-send-appointmentreminder-reports.json
│       ├── synctasktype-sent-sms-messages-from-central-server.json
│       ├── synctasktype-send-vl-program-data-to-cphl.json
│       ├── synctasktype-receive-results-from-alis.json
│       ├── synctasktype-eafya-stock-integration.json
│       └── synctasktype-sends-prescriptions-eafya.json
└── README.md                 # This file
```

## How It Works

### Automatic Import on Startup

1. When the UgandaEMR Sync module starts, it automatically imports these configurations
2. Configurations are loaded from the module's classpath (bundled in JAR)
3. Each profile and task type is created or updated in the database
4. Import activity is logged for troubleshooting

### Override Configurations

To provide custom configurations per installation:

1. **Create External Directory**:
   ```bash
   mkdir -p {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/
   ```

2. **Copy and Customize**: Copy configuration files from this directory to the external location and customize them

3. **Priority**: External configurations take precedence over bundled defaults

## File Organization

Each profile and task type has its own JSON configuration file. Files are named using the pattern:
- **Profiles**: `profile-{profile-id}.json`
- **Task Types**: `synctasktype-{task-type-slug}.json`

This modular structure allows:
- Easy version control of individual configurations
- Selective override of specific profiles or tasks
- Clear separation of concerns
- Simpler maintenance and updates

## Sync Profiles

Individual profile files contain complete configuration for FHIR synchronization including:
- Resource types to sync (Patient, Observation, Encounter, etc.)
- Search parameters and filters
- Endpoint configuration
- Scheduling information
- Patient identifier mappings
- Case-based vs resource-based settings

**Profiles Included** (12 production + 2 examples):
- Lab integrations (CPHL Viral Load, ALIS)
- Data exchange (ART Access, Shared Health Record)
- Disease surveillance (HIV, MPOX, Mortality)
- External systems (eCBSS, Client Registry, Cross Border)
- Specific programs (HTS, TB, ART Regimen, HIV Exposed Infant)

## Sync Task Types

**Note:** These are `org.openmrs.module.ugandaemrsync.model.SyncTaskType` entities - definitions of task types for data exchange operations.

Individual task type files contain:
- Task name and description
- Data type configuration (e.g., TestOrder, Encounter, Drug)
- Data type identifier (concept UUID)
- Endpoint configuration (URL, authentication)

**Task Types Included** (12 total):
- Lab integrations (CPHL Viral Load, ALIS results)
- Data exchange (ART Access visits, Shared Health Record)
- Reporting (MER, HMIS)
- Notifications (SMS appointment reminders, sent messages)
- External integrations (eAFYA stock, eAFYA prescriptions)

## Security Considerations

### Placeholder Replacement

Configuration files use placeholders for sensitive data:
- `{{CONFIGURE_PASSWORD}}` - Replace with actual password
- `{{CONFIGURE_TOKEN}}` - Replace with actual API token

**Important**: Before deploying to production:
1. Either replace placeholders in this directory OR
2. Provide external configuration files with actual credentials
3. Set appropriate file permissions
4. Use environment variables or secret management systems where possible

### Development vs Production

**Development**: These bundled configurations are suitable for development and testing.

**Production**: Create external configuration directory with production-specific:
- Endpoint URLs
- Authentication credentials
- API tokens
- Schedules and intervals

## Configuration Format

### Individual Profile File Format

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
    "generateBundle": false,
    "numberOfResourcesInBundle": 100,
    "profileEnabled": false,
    "patientIdentifierType": "3",
    "endpoint": {
      "url": "https://example.com/fhir",
      "authentication": "BASIC",
      "username": "username",
      "password": "{{CONFIGURE_PASSWORD}}"
    }
  }
}
```

### Individual SyncTaskType File Format

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

## Usage

### Using Bundled Configurations

1. Build and deploy the module
2. Start OpenMRS
3. Configurations are automatically imported on module startup
4. Verify in Module Administration → UgandaEMR Sync

### Overriding Specific Configurations

To override a specific profile:

```bash
# Create external directory structure
mkdir -p {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/

# Copy only the profile you want to customize
cp api/src/main/resources/configuration/hie/syncprofile/profile-mpox-screening.json \
   {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/

# Edit the external file with your custom settings
vim {OPENMRS_DATA_DIR}/modules/ugandaemrsync/configuration/hie/syncprofile/profile-mpox-screening.json

# Restart OpenMRS
systemctl restart openmrs
```

### Importing via REST API

```bash
# Import a specific profile
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@api/src/main/resources/configuration/hie/syncprofile/profile-mpox-screening.json"

# Import all configurations
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@api/src/main/resources/configuration/hie/complete_config.json"
```

## Validation

Validate JSON syntax before modifying:

```bash
# Validate all profile files
for file in api/src/main/resources/configuration/hie/syncprofile/*.json; do
  jq empty "$file" && echo "✓ Valid: $(basename $file)" || echo "✗ Invalid: $(basename $file)"
done

# Validate all task type files
for file in api/src/main/resources/configuration/hie/synctasktype/*.json; do
  jq empty "$file" && echo "✓ Valid: $(basename $file)" || echo "✗ Invalid: $(basename $file)"
done
```

## Updating Configurations

When updating configurations in this directory:

1. **Test Changes**: Validate JSON syntax
2. **Rebuild Module**: `mvn clean install`
3. **Redeploy Module**: Copy new JAR to OpenMRS modules directory
4. **Restart OpenMRS**: Changes will be imported on startup
5. **Verify**: Check logs and module administration UI

## Troubleshooting

### Configurations Not Importing

1. **Check Logs**:
   ```bash
   tail -f /var/log/openmrs/openmrs.log | grep -i "import"
   ```

2. **Verify Module Version**: Ensure you have the latest module with import functionality

3. **Check Database**: Verify profiles exist in `ugandaemr_sync_fhir_profile` table

4. **Manual Import**: Use REST API to import manually

### Conflicts with Existing Configurations

If profiles already exist in database:
- Bundled configurations will **update** existing records
- Changes are logged in module startup logs
- Database records take precedence until module restart

## Best Practices

1. **Version Control**: Keep these configurations in version control
2. **Documentation**: Document customizations in comments or separate docs
3. **Testing**: Test configuration changes in development first
4. **Backup**: Backup database before major configuration changes
5. **Security**: Never commit actual passwords/tokens to version control

## Support

For issues or questions:
- Check the module documentation
- Review the UgandaEMR Wiki
- Contact METS support team
- Open GitHub issues
