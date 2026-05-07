# UgandaEMR Sync Module - Quick Start Guide

Get up and running with UgandaEMR Sync Module in 15 minutes!

## 🚀 Quick Installation (5 minutes)

### Step 1: Install the Module
```bash
# Download the latest OAM file from releases
wget https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/releases/download/v2.0.5/openmrs-module-ugandaemr-sync-2.0.5.oam

# Upload via OpenMRS Admin UI
# Navigate to: Administration → Manage Modules → Add Module
# Select the downloaded OAM file and click "Upload"
```

### Step 2: Configure Basic Settings
```bash
# Navigate to: Administration → Advanced Settings → Global Properties
# Search for "ugandaemrsync" and configure:

ugandaemrsync.healthCenterSyncId = YOUR_FACILITY_ID
ugandaemrsync.protocol = https
```

## 🎯 Common Use Cases

### Use Case 1: Sync Patient Data to Central Server

**Scenario**: Share patient demographics and encounters with a national health repository

```bash
# Create FHIR profile via REST API
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

# Verify profile creation
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

### Use Case 2: DHIS2 Data Reporting

**Scenario**: Send aggregate indicator data to DHIS2

```bash
# Configure DHIS2 connection
# Via Global Properties:
ugandaemrsync.sendtoDHIS2.server.url = https://dhis2.health.gov
ugandaemrsync.sendtoDHIS2.server.username = dhis2_user
ugandaemrsync.sendtoDHIS2.server.password = dhis2_password

# The module will automatically handle DHIS2 reporting
# Check sync status via:
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource/stats
```

### Use Case 3: HIV Case Surveillance

**Scenario**: Report HIV cases for surveillance purposes

```bash
# Create case-based surveillance profile
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "HIV Case Surveillance",
    "description": "HIV case-based surveillance reporting",
    "isCaseBasedProfile": true,
    "caseBasedPrimaryResourceType": "Condition",
    "caseBasedPrimaryResourceTypeId": "hiv-infection-concept-uuid",
    "resourceTypes": "Patient,Condition,Observation,Encounter",
    "profileEnabled": true,
    "scheduleEnabled": true,
    "scheduleType": "FIXED_RATE",
    "fixedRateInterval": 3600000,
    "executionPriority": 1
  }'

# Create scheduled task
# Navigate to: Administration → Scheduler → Manage Tasks
# Click "Add Task"
# Name: HIV Surveillance Task
# Class: org.openmrs.module.ugandaemrsync.tasks.GenericFhirProfileSchedulerTask
# Schedule: 0 0 * * * (hourly)
# Save and start the task
```

### Use Case 4: Lab Results Exchange

**Scenario**: Receive lab results from Central Public Health Laboratory (CPHL)

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

# Check results
curl -u admin:Admin123 \
  "https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource?syncStatus=true"
```

## ⚙️ Scheduler Setup (3 minutes)

### Option 1: Generic Scheduler (Recommended)

Create ONE scheduler task that handles ALL profiles:

```bash
# Via OpenMRS Scheduler UI
Task Name: Generic FHIR Profile Scheduler
Task Class: org.openmrs.module.ugandaemrsync.tasks.GenericFhirProfileSchedulerTask
Schedule: Run every 5 minutes
Description: Universal FHIR profile scheduler
```

**Benefits**:
- Single task for all profiles
- Anti-blocking protection
- Individual profile scheduling
- Better resource management

### Option 2: Profile-Specific Tasks (Legacy)

Create individual tasks for each profile:

```bash
# National Data Ware House Task
Task Name: National Data Ware House Task
Task Class: org.openmrs.module.ugandaemrsync.tasks.NationalDataWareHouseTask
Schedule: 0 0 * * * (hourly)
Property: syncFhirProfileUUID = <your-profile-uuid>

# Mortality Surveillance Task
Task Name: Mortality Surveillance Task
Task Class: org.openmrs.module.ugandaemrsync.tasks.MortalitySurveillanceTask
Schedule: 0 0 0 * * (daily)
```

## 🔍 Monitoring & Troubleshooting (5 minutes)

### Check Sync Status

```sql
-- See what's running vs waiting
SELECT 
  name,
  last_execution_status,
  last_execution_date,
  next_execution_date,
  CASE 
    WHEN next_execution_date <= NOW() THEN 'DUE NOW'
    WHEN last_execution_status = 'RUNNING' THEN 'RUNNING'
    WHEN last_execution_status = 'FAILED' THEN 'FAILED'
    ELSE 'WAITING'
  END as status
FROM sync_fhir_profile
WHERE profile_enabled = TRUE
ORDER BY execution_priority, next_execution_date;
```

### View Recent Activity

```sql
-- Last 20 sync operations
SELECT 
  sp.name as profile,
  eph.execution_date,
  eph.execution_status,
  eph.resources_processed,
  eph.execution_duration_ms,
  eph.error_message
FROM sync_fhir_profile_execution_history eph
JOIN sync_fhir_profile sp ON eph.profile_id = sp.sync_fhir_profile_id
ORDER BY eph.execution_date DESC
LIMIT 20;
```

### Enable Debug Logging

```xml
<!-- Add to OpenMRS log4j.xml -->
<logger name="org.openmrs.module.ugandaemrsync">
  <level value="DEBUG"/>
</logger>

<!-- Restart OpenMRS -->
```

### Common Issues & Solutions

#### Issue 1: Profile Not Running

**Symptoms**: Profile shows "WAITING" but never executes

**Solutions**:
```sql
-- Check if profile is enabled
SELECT name, profile_enabled, schedule_enabled 
FROM sync_fhir_profile 
WHERE name = 'Your Profile Name';

-- Enable profile
UPDATE sync_fhir_profile 
SET profile_enabled = TRUE, schedule_enabled = TRUE 
WHERE name = 'Your Profile Name';

-- Check next execution date
UPDATE sync_fhir_profile 
SET next_execution_date = NOW() 
WHERE name = 'Your Profile Name';
```

#### Issue 2: Connection Failures

**Symptoms**: "FAILED" status with connection errors

**Solutions**:
```bash
# Test external system connectivity
curl -v https://external-system.example.com/fhir

# Check credentials
SELECT name, url, url_user_name 
FROM sync_fhir_profile 
WHERE name = 'Your Profile Name';

# Update credentials
UPDATE sync_fhir_profile 
SET url = 'https://correct-url.example.com/fhir',
    url_user_name = 'correct-username',
    url_password = 'correct-password'
WHERE name = 'Your Profile Name';
```

#### Issue 3: No Resources Generated

**Symptoms**: Profile runs but generates 0 resources

**Solutions**:
```sql
-- Check profile configuration
SELECT name, resource_types, generate_bundle, 
       resource_search_parameter, case_based_primary_resource_type
FROM sync_fhir_profile 
WHERE name = 'Your Profile Name';

-- Check if data exists in system
SELECT COUNT(*) FROM patient WHERE voided = 0;
SELECT COUNT(*) FROM encounter WHERE voided = 0;
```

#### Issue 4: Memory Issues

**Symptoms**: OutOfMemoryError or slow performance

**Solutions**:
```sql
-- Reduce batch size
UPDATE sync_fhir_profile 
SET number_of_resources_in_bundle = 25  -- Reduce from 50/100
WHERE name = 'Your Profile Name';

-- Add sync limit
UPDATE sync_fhir_profile 
SET sync_limit = 500  -- Process 500 resources per run
WHERE name = 'Your Profile Name';
```

## 📊 Performance Tuning (2 minutes)

### Optimize for Large Facilities

```sql
-- Profile for high-volume data exchange
UPDATE sync_fhir_profile 
SET 
  number_of_resources_in_bundle = 100,      -- Larger bundles
  sync_limit = 2000,                         -- Higher limit
  execution_priority = 1,                    -- High priority
  fixed_rate_interval = 1800000              -- Every 30 minutes
WHERE name = 'High Volume Profile';

-- Profile for low-latency updates
UPDATE sync_fhir_profile 
SET 
  number_of_resources_in_bundle = 10,       -- Smaller bundles
  sync_limit = 100,                          -- Lower limit
  execution_priority = 1,                    -- High priority
  fixed_rate_interval = 300000               -- Every 5 minutes
WHERE name = 'Real-time Profile';
```

### Schedule Profiles by Priority

```sql
-- Critical profiles (priority 1-3)
-- Run frequently, fast execution
UPDATE sync_fhir_profile 
SET execution_priority = 1, fixed_rate_interval = 300000
WHERE name IN ('Alerts', 'Emergency Data');

-- Important profiles (priority 4-6)
-- Run moderately
UPDATE sync_fhir_profile 
SET execution_priority = 5, fixed_rate_interval = 3600000
WHERE name IN ('HIV Surveillance', 'Lab Results');

-- Background profiles (priority 7-10)
-- Run infrequently, heavy processing
UPDATE sync_fhir_profile 
SET execution_priority = 9, fixed_rate_interval = 21600000
WHERE name IN ('Analytics', 'Reporting');
```

## 🔐 Security Setup (2 minutes)

### Set Up User Privileges

```sql
-- Create sync administrator role
INSERT INTO role (name, description, uuid)
VALUES ('Sync Administrator', 'Full access to sync functions', UUID());

-- Grant all sync privileges
-- Via OpenMRS Admin UI:
# Navigate to: Administration → Users → Roles → Manage Roles
# Find "Sync Administrator"
# Check all UgandaemrSync privileges
# Save

-- Assign to users
# Navigate to: Administration → Users → Manage Users
# Edit user
# Add "Sync Administrator" role
# Save
```

### API Access

```bash
# Create API-only user for external integrations
# Via OpenMRS Admin UI:
# 1. Create new user (e.g., "sync-api-user")
# 2. Set weak password (only used for API)
# 3. Grant "UgandaemrSync: Manage FHIR Resources" privilege
# 4. Disable UI access

# Test API access
curl -u sync-api-user:password \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

## 📈 Next Steps

1. **Read Full Documentation**: Check README.md for detailed features
2. **Configure Profiles**: Set up profiles specific to your facility
3. **Monitor Performance**: Regularly check sync statistics
4. **Set Up Alerts**: Configure notifications for failed syncs
5. **Scale Up**: Add more profiles as needed

## 🆘 Getting Help

### Common Resources
- **Full README**: Complete module documentation
- **API Docs**: `/ws/rest/v1/` endpoint documentation
- **Database Schema**: See `liquibase.xml` for table structures
- **Log Files**: `/openmrs/logs/` for troubleshooting

### Community Support
- **GitHub Issues**: Report bugs or request features
- **OpenMRS Talk**: Community forum
- **METS Programme**: Professional support contact

### Emergency Troubleshooting

If sync is completely broken:

```bash
# 1. Stop all scheduled tasks
# Via OpenMRS Scheduler UI: Stop all sync tasks

# 2. Check module status
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/module\?name\=ugandaemrsync

# 3. Review error logs
tail -f /openmrs/logs/openmrs.log | grep ugandaemrsync

# 4. Restart module
# Via Admin UI: Stop and Start UgandaEMR Sync module

# 5. Test with simple profile
# Create a basic test profile and verify it works
```

---

**Estimated Setup Time**: 15 minutes  
**Support Level**: Basic (can be done by facility IT staff)  
**Requirements**: OpenMRS admin access, basic SQL knowledge