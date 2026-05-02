# UgandaEMR Sync Configuration Manifest

This directory contains modular configuration files for UgandaEMR sync profiles and sync task types.

## Sync Profiles (2 files)

### Resource-Based Profiles
- `profile-example-resource-based.json` - Example profile for Encounter, Patient, and Observation resources

### Case-Based Profiles
- `profile-example-case-based.json` - Example case-based profile using Encounter as primary resource

## Sync Task Types (12 files)

**Note:** These are `org.openmrs.module.ugandaemrsync.model.SyncTaskType` entities - definitions of task types for data exchange, not scheduled tasks.

### Lab Exchange
- `synctasktype-send-viral-load-to-cphl.json` - Send Viral Load To CPHL
- `synctasktype-request-viral-load-results-form-cphl.json` - Request Viral Load Results from CPHL
- `synctasktype-send-vl-program-data-to-cphl.json` - Send VL Program Data To CPHL
- `synctasktype-receive-results-from-alis.json` - Receive Results From ALIS

### Data Exchange
- `synctasktype-receive-art-access-visits-data.json` - Receive ART Access Visits Data
- `synctasktype-sync-data-to-shared-health-record.json` - Sync Data to Shared Health Record

### Reporting
- `synctasktype-send-mer-reports.json` - Send MER Reports
- `synctasktype-send-hmis-reports.json` - Send HMIS Reports

### Notifications
- `synctasktype-send-appointmentreminder-reports.json` - Send SMS Appointment Reminders
- `synctasktype-sent-sms-messages-from-central-server.json` - Sent SMS Messages from Central Server

### External Integrations
- `synctasktype-eafya-stock-integration.json` - eAFYA Stock Integration
- `synctasktype-sends-prescriptions-eafya.json` - Sends Prescriptions eAFYA

## Complete Configuration

- `complete_config.json` - Combined export of all profiles and task types for initial setup

## SyncTaskType Structure

Each SyncTaskType file contains:
- **metadata**: Identification and description
- **configuration**: 
  - name: Display name
  - dataType: OpenMRS data type (e.g., org.openmrs.TestOrder, org.openmrs.Encounter)
  - dataTypeId: Concept UUID for the data type
  - endpoint: URL and authentication details

## File Naming Convention

- **Profiles**: `profile-{profile-id}.json`
- **Task Types**: `synctasktype-{task-type-slug}.json`

Each file is self-contained with metadata and configuration, allowing for:
- Selective import/export
- Version control per configuration
- Easy maintenance and updates
- Clear separation of concerns

## Usage

See [README.md](../README.md) for detailed import/export instructions.
