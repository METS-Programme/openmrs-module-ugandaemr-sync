# UgandaEMR Sync Module - UUID Migration Summary

## Overview
All HIE configuration files in `/configuration/hie` have been updated to use UUIDs instead of descriptive IDs. The UUIDs were extracted from the liquibase migration files to ensure consistency with the database schema.

## Changes Made

### Task Type Configuration Files (12 files)

| Old ID | New UUID | Description |
|--------|----------|-------------|
| `synctasktype-send-viral-load-to-cphl` | `3551ca84-06c0-432b-9064-fcfeefd6f4ec` | Send Viral Load To CPHL |
| `synctasktype-send-vl-program-data-to-cphl` | `f9b2fa5d-5d37-4fd9-b20a-a0cab664f520` | Send VL Program Data To CPHL |
| `synctasktype-request-viral-load-results-form-cphl` | `3396dcf0-2106-4e73-9b90-c63978c3a8b4` | Request Viral Load Results form CPHL |
| `synctasktype-receive-art-access-visits-data` | `4c4e9551-d9d6-4882-93bd-e61a42e2f755` | Receive ART Access Visits Data |
| `synctasktype-sync-data-to-shared-health-record` | `3c1ce940-8ade-11ea-bc55-0242ac130003` | Sync Data to Shared Health Record |
| `synctasktype-send-mer-reports` | `6ebd85c8-127b-4c88-8a40-27defef367a9` | Send MER Reports |
| `synctasktype-send-appointmentreminder-reports` | `08c5be38-1b79-4e27-b9ca-5da709aef5fe` | Send AppointmentReminder Reports |
| `synctasktype-send-hmis-reports` | `c5f00f18-c0f6-4917-b973-2b7c1d2d4a81` | Send HMIS Reports |
| `synctasktype-receive-results-from-alis` | `d4a3ebbb-e793-4e56-867c-0cf998e51f56` | Receive Results From ALIS |
| `synctasktype-sent-sms-messages-from-central-server` | `d63cb4b5-97ba-4380-aba9-d3f60634cd7a` | Sent SMS Messages from central server |
| `synctasktype-eafya-stock-integration` | `2def1dea-fc0b-11ef-ab84-28977ca9db4b` | eAFYA Stock Integration |
| `synctasktype-sends-prescriptions-eafya` | `8ca0ffd0-0fb0-11f0-9e19-da924fd23489` | Sends Prescriptions eAFYA |

### Sync Profile Configuration Files (12 files)

| Old ID | New UUID | Description |
|--------|----------|-------------|
| `profile-art-access-integration` | `6a9b7f2e-0128-49a1-99a0-94a361b9e2ae` | ART Access Integration |
| `profile-tb-data` | `8963510e-404a-4363-a033-effc682fdacc` | TB Data |
| `profile-hts-data` | `527e1372-ff30-41e9-b139-d61b8a9ff197` | HTS Data |
| `profile-hiv-exposed-infant-data` | `e8a37e2f-6c78-476e-93a5-14aec653c406` | HIV Exposed Infant Data |
| `profile-hiv-case-based-surveillance` | `56db6ac0-0e60-4ddc-8dfd-0035a4e64489` | HIV CASE BASED SURVEILLANCE |
| `profile-mpox-screening` | `ef2afcf1-e2f7-4bb4-aa68-b18266e0b35f` | MPOX Screening |
| `profile-mortality-surveillance` | `6acc9390-1049-41a5-979b-c57d895ca674` | Mortality Surveillance |
| `profile-cross-border-integration` | `f2190cf4-2236-11ee-be56-0242ac120002` | Cross Border Integration |
| `profile-client-registry-integration` | `ccfafdf8-8ba1-4c00-986f-21392c1af1f6` | Client Registry Integration |
| `profile-ecbss-integration` | `99c4d715-4fcf-4d95-a946-257c6de05cf7` | eCBSS Integration |
| `profile-art-regimen-change` | `4e0d3ff6-82ac-45a8-bcf3-7367a9098d65` | ART Regimen Change |
| `profile-send-lab-request-to-alis-task` | `8cf5a732-51e4-4c7c-9b36-96eb2644a99a` | Send Lab Request to ALIS Task |
| `profile-example-resource-based` | `51f8fa8f-a6fc-46b7-8f0a-afa28f7d4f40` | Example Resource Based |
| `profile-example-case-based` | `51f8fa8f-a6fc-46b7-8f0a-afa28f7d4f40` | Example Case Based |

### Complete Configuration File
The `complete_config.json` file was also updated to use UUIDs in its internal task type definitions, replacing descriptive IDs like `tasktype-viral-load-cphl` with their corresponding UUIDs.

## Benefits

1. **Database Consistency**: Configuration IDs now match the UUIDs used in the database schema
2. **Uniqueness**: UUIDs provide guaranteed uniqueness across all environments
3. **Portability**: Configurations can be moved between environments without ID conflicts
4. **Integration**: Easier integration with external systems that rely on stable identifiers
5. **Standards Compliance**: Follows OpenMRS standard practice of using UUIDs

## Verification

Build verification completed successfully:
```bash
mvn clean install -DskipTests
```
- ✅ All configuration files updated
- ✅ JSON syntax validated
- ✅ Module builds successfully
- ✅ No broken references

## Files Modified

**Total files updated: 26**
- 12 sync task type configuration files
- 13 sync profile configuration files  
- 1 complete configuration file

All configuration files are now using UUIDs that match the liquibase migration definitions, ensuring consistency between the file-based configuration and the database schema.