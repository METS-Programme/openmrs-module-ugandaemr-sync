# Getting Started with UgandaEMR Sync Module

Get up and running with UgandaEMR Sync Module in 15 minutes!

## Prerequisites

Before installing the UgandaEMR Sync Module, ensure you have:

- **OpenMRS Platform**: Version 2.7.0 or higher
- **System Access**: Administrator access to OpenMRS
- **Database Access**: MySQL 5.7+ or PostgreSQL 9.6+
- **Network Connectivity**: Ability to connect to external systems (if applicable)
- **Java Runtime**: Java 8 or higher

## Installation (5 minutes)

### Step 1: Download the Module

Download the latest OAM (OpenMRS Archive Module) file from the releases page:

```bash
wget https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/releases/download/v2.0.5/openmrs-module-ugandaemr-sync-2.0.5.oam
```

### Step 2: Upload via OpenMRS Admin UI

1. Navigate to: **Administration → Manage Modules**
2. Click **Add Module**
3. Select the downloaded OAM file
4. Click **Upload**
5. Wait for the module to load and start

### Step 3: Verify Installation

Check the module status:

```bash
# Via command line
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/module?name=ugandaemrsync

# Via Admin UI
# Navigate to: Administration → Manage Modules
# Find "UgandaEMR Sync Module" in the list
# Verify status is "Started"
```

## Basic Configuration (5 minutes)

### Step 1: Configure Global Properties

Navigate to **Administration → Advanced Settings → Global Properties** and configure:

```
ugandaemrsync.healthCenterSyncId = YOUR_FACILITY_ID
ugandaemrsync.protocol = https
```

### Step 2: Set Up User Privileges

1. Navigate to: **Administration → Users → Roles → Manage Roles**
2. Find or create a role for sync users
3. Grant appropriate UgandaemrSync privileges
4. Assign the role to users who will manage sync operations

### Step 3: Verify Database Tables

The module should have created the following tables:

```sql
-- Check if tables exist
SHOW TABLES LIKE 'sync_%';

-- Expected tables:
-- sync_fhir_profile
-- sync_fhir_resource
-- sync_fhir_case
-- sync_task
```

## Quick Test (3 minutes)

### Create a Test Profile

```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Test Profile",
    "description": "Test profile for verification",
    "resourceTypes": "Patient",
    "profileEnabled": true,
    "generateBundle": true,
    "numberOfResourcesInBundle": 10,
    "url": "https://httpbin.org/post",
    "urlUserName": "test",
    "urlPassword": "test",
    "syncLimit": 5
  }'
```

### Verify Profile Creation

```bash
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

### Test Manual Execution

```bash
# Trigger manual execution (if configured)
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID/execute \
  -u admin:Admin123
```

## Scheduler Setup (2 minutes)

### Option 1: Generic Scheduler (Recommended)

Create ONE scheduler task that handles ALL profiles:

1. Navigate to: **Administration → Scheduler → Manage Tasks**
2. Click **Add Task**
3. Configure:
   - **Name**: Generic FHIR Profile Scheduler
   - **Class**: `org.openmrs.module.ugandaemrsync.tasks.GenericFhirProfileSchedulerTask`
   - **Schedule**: Run every 5 minutes
   - **Description**: Universal FHIR profile scheduler
4. Save and start the task

**Benefits**:
- Single task for all profiles
- Anti-blocking protection
- Individual profile scheduling
- Better resource management

### Option 2: Profile-Specific Tasks

Create individual tasks for each profile (legacy approach):

```bash
# Example: National Data Ware House Task
Task Name: National Data Ware House Task
Task Class: org.openmrs.module.ugandaemrsync.tasks.NationalDataWareHouseTask
Schedule: 0 0 * * * (hourly)
Property: syncFhirProfileUUID = <your-profile-uuid>
```

## Common Use Cases

### Use Case 1: Sync Patient Data to Central Server

```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Central Server Sync",
    "description": "Daily patient data sync to central server",
    "resourceTypes": "Patient,Encounter,Observation",
    "profileEnabled": true,
    "generateBundle": true,
    "numberOfResourcesInBundle": 100,
    "url": "https://central-server.health.gov/fhir",
    "urlUserName": "your-facility-id",
    "urlPassword": "your-api-key",
    "syncLimit": 1000
  }'
```

### Use Case 2: DHIS2 Data Reporting

Configure via Global Properties:

```
ugandaemrsync.sendtoDHIS2.server.url = https://dhis2.health.gov
ugandaemrsync.sendtoDHIS2.server.username = dhis2_user
ugandaemrsync.sendtoDHIS2.server.password = dhis2_password
```

### Use Case 3: Lab Results Exchange

```bash
# Request lab results for a patient
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/requestlabresults \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "patientIdentifier": "PAT-001",
    "dateFrom": "2024-01-01",
    "dateTo": "2024-12-31",
    "labSystem": "CPHL"
  }'
```

## Next Steps

1. **Read Full Documentation**: Check [Home](Home) for complete module overview
2. **Configure Profiles**: Set up profiles specific to your facility needs
3. **Set Up Monitoring**: Configure monitoring and alerting
4. **Security Hardening**: Implement security best practices
5. **Performance Tuning**: Optimize for your facility's data volume

## Troubleshooting

### Issue: Module Not Starting

**Solutions**:
1. Check OpenMRS logs: `/openmrs/logs/openmrs.log`
2. Verify Java version: `java -version`
3. Check database connectivity
4. Verify sufficient disk space

### Issue: Profile Not Executing

**Solutions**:
1. Verify profile is enabled
2. Check scheduler task is running
3. Review execution history
4. Enable debug logging

### Issue: Connection Failures

**Solutions**:
1. Test external system connectivity
2. Verify credentials
3. Check firewall settings
4. Review SSL/TLS configuration

## Getting Help

- **Documentation**: [Full documentation index](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/blob/master/DOCUMENTATION_INDEX.md)
- **GitHub Issues**: [Report problems](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- **Community**: [OpenMRS Talk](https://talk.openmrs.org/)
- **Quick Reference**: [API Documentation](API-Reference)

---

**Estimated Setup Time**: 15 minutes  
**Support Level**: Basic (can be done by facility IT staff)  
**Last Updated**: May 2, 2026