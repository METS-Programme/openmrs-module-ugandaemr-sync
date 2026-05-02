# Configuration Guide

Comprehensive configuration guide for the UgandaEMR Sync Module.

## Table of Contents

- [Global Properties](#global-properties)
- [FHIR Profile Configuration](#fhir-profile-configuration)
- [Scheduler Configuration](#scheduler-configuration)
- [Security Configuration](#security-configuration)
- [Performance Tuning](#performance-tuning)
- [External System Configuration](#external-system-configuration)

## Global Properties

Configure these properties via **Administration → Advanced Settings → Global Properties**

### Basic Configuration

```
# Facility Identification
ugandaemrsync.healthCenterSyncId = YOUR_FACILITY_ID

# Protocol Configuration
ugandaemrsync.protocol = https
```

### DHIS2 Integration

```
# DHIS2 Server Configuration
ugandaemrsync.sendtoDHIS2.server.url = https://dhis2.health.gov
ugandaemrsync.sendtoDHIS2.server.username = dhis2_user
ugandaemrsync.sendtoDHIS2.server.password = dhis2_password
ugandaemrsync.sendtoDHIS2.server.version = 2.35
```

### Advanced Settings

```
# Connection Settings
ugandaemrsync.connection.timeout = 30000
ugandaemrsync.read.timeout = 60000

# Retry Configuration
ugandaemrsync.retry.maxAttempts = 3
ugandaemrsync.retry.backoffDelay = 1000

# Batch Processing
ugandaemrsync.default.batchSize = 100
ugandaemrsync.max.batchSize = 500
```

## FHIR Profile Configuration

### Profile Types

#### Resource-based Profile
Configured for syncing specific FHIR resource types:

```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Patient Data Sync",
    "description": "Sync patient demographics and encounters",
    "resourceTypes": "Patient,Encounter,Observation",
    "profileEnabled": true,
    "generateBundle": true,
    "numberOfResourcesInBundle": 100,
    "url": "https://central-server.health.gov/fhir",
    "urlUserName": "facility-id",
    "urlPassword": "api-key",
    "syncLimit": 1000
  }'
```

#### Case-based Profile
Configured for disease surveillance:

```bash
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
```

### Profile Configuration Fields

| Field | Type | Description | Default |
|-------|------|-------------|---------|
| `name` | String | Unique profile name | Required |
| `description` | String | Profile description | Optional |
| `resourceTypes` | String | Comma-separated FHIR resource types | Required |
| `profileEnabled` | Boolean | Enable/disable profile | false |
| `isCaseBasedProfile` | Boolean | Case-based or resource-based | false |
| `url` | String | External system endpoint | Optional |
| `urlUserName` | String | Authentication username | Optional |
| `urlPassword` | String | Authentication password | Optional |
| `generateBundle` | Boolean | Generate FHIR bundles | true |
| `numberOfResourcesInBundle` | Integer | Resources per bundle | 50 |
| `syncLimit` | Integer | Max resources to sync | Unlimited |
| `executionPriority` | Integer | Execution priority (1-10) | 5 |

## Scheduler Configuration

### Generic Scheduler (Recommended)

**Single scheduler for all profiles**:

```xml
<!-- Via Admin UI: Administration → Scheduler → Manage Tasks -->
Task Name: Generic FHIR Profile Scheduler
Task Class: org.openmrs.module.ugandaemrsync.tasks.GenericFhirProfileSchedulerTask
Schedule: Run every 5 minutes
Description: Universal FHIR profile scheduler
Enabled: true
```

**Benefits**:
- Anti-blocking protection
- Priority-based execution
- Individual profile scheduling
- Better resource management

### Profile-Specific Scheduling

Configure individual profile schedules:

```bash
# Fixed Rate Schedule
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/UUID \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "scheduleEnabled": true,
    "scheduleType": "FIXED_RATE",
    "fixedRateInterval": 3600000,
    "executionPriority": 1
  }'

# Fixed Delay Schedule
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/UUID \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "scheduleEnabled": true,
    "scheduleType": "FIXED_DELAY",
    "fixedDelayInterval": 1800000,
    "executionPriority": 2
  }'

# Cron Schedule
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/UUID \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "scheduleEnabled": true,
    "scheduleType": "CRON",
    "cronExpression": "0 0 * * *",
    "executionPriority": 3
  }'
```

### Schedule Types

| Type | Description | Example |
|------|-------------|---------|
| `FIXED_RATE` | Run at fixed intervals | Every 5 minutes |
| `FIXED_DELAY` | Run with delay between executions | 30 minutes after completion |
| `CRON` | Cron expression | Daily at midnight |
| `MANUAL` | Only manual execution | On-demand |

## Security Configuration

### Role-Based Access Control

#### Create Sync Administrator Role

```sql
-- Create role
INSERT INTO role (name, description, uuid)
VALUES ('Sync Administrator', 'Full access to sync functions', UUID());

-- Grant privileges via Admin UI:
-- Administration → Users → Roles → Manage Roles
-- Find "Sync Administrator"
-- Check all UgandaemrSync privileges
-- Save
```

#### Available Privileges

- **UgandaemrSync: Manage FHIR Profiles**: Create, edit, delete profiles
- **UgandaemrSync: Manage FHIR Resources**: Manage FHIR resources
- **UgandaemrSync: Manage FHIR Cases**: Manage case-based surveillance
- **UgandaemrSync: View Execution History**: View execution history and logs
- **UgandaemrSync: Run Manual Execution**: Trigger manual sync operations

### API Security

#### Create API-only User

```bash
# Via Admin UI:
# 1. Create new user (e.g., "sync-api-user")
# 2. Set strong password
# 3. Grant "UgandaemrSync: Manage FHIR Resources" privilege
# 4. Disable UI access if needed

# Test API access
curl -u sync-api-user:password \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

### SSL/TLS Configuration

```bash
# Enable HTTPS for external connections
ugandaemrsync.protocol = https

# Configure SSL context (in Java):
System.setProperty("javax.net.ssl.keyStore", "/path/to/keystore.jks");
System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
System.setProperty("javax.net.ssl.trustStore", "/path/to/truststore.jks");
System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
```

## Performance Tuning

### Memory Optimization

```bash
# Reduce batch size for limited memory
UPDATE sync_fhir_profile 
SET number_of_resources_in_bundle = 25  -- Reduce from 50/100
WHERE name = 'Your Profile Name';

# Add sync limit to prevent overload
UPDATE sync_fhir_profile 
SET sync_limit = 500  -- Process 500 resources per run
WHERE name = 'Your Profile Name';
```

### High-Volume Facilities

```bash
# Profile for high-volume data exchange
UPDATE sync_fhir_profile 
SET 
  number_of_resources_in_bundle = 100,      -- Larger bundles
  sync_limit = 2000,                         -- Higher limit
  execution_priority = 1,                    -- High priority
  fixed_rate_interval = 1800000              -- Every 30 minutes
WHERE name = 'High Volume Profile';
```

### Low-Latency Updates

```bash
# Profile for real-time updates
UPDATE sync_fhir_profile 
SET 
  number_of_resources_in_bundle = 10,       -- Smaller bundles
  sync_limit = 100,                          -- Lower limit
  execution_priority = 1,                    -- High priority
  fixed_rate_interval = 300000               -- Every 5 minutes
WHERE name = 'Real-time Profile';
```

### Priority-Based Scheduling

```bash
# Critical profiles (priority 1-3)
UPDATE sync_fhir_profile 
SET execution_priority = 1, fixed_rate_interval = 300000
WHERE name IN ('Alerts', 'Emergency Data');

# Important profiles (priority 4-6)
UPDATE sync_fhir_profile 
SET execution_priority = 5, fixed_rate_interval = 3600000
WHERE name IN ('HIV Surveillance', 'Lab Results');

# Background profiles (priority 7-10)
UPDATE sync_fhir_profile 
SET execution_priority = 9, fixed_rate_interval = 21600000
WHERE name IN ('Analytics', 'Reporting');
```

## External System Configuration

### DHIS2 Configuration

```
ugandaemrsync.sendtoDHIS2.server.url = https://dhis2.health.gov
ugandaemrsync.sendtoDHIS2.server.username = dhis2_user
ugandaemrsync.sendtoDHIS2.server.password = dhis2_password
ugandaemrsync.sendtoDHIS2.server.version = 2.35
```

### Central Server Configuration

```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Central Server Sync",
    "url": "https://central-server.health.gov/fhir",
    "urlUserName": "your-facility-id",
    "urlPassword": "your-api-key",
    "resourceTypes": "Patient,Encounter,Observation",
    "profileEnabled": true
  }'
```

### Lab System Configuration

```bash
# Configure lab results endpoint
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/requestlabresults \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "patientIdentifier": "PAT-001",
    "dateFrom": "2024-01-01",
    "dateTo": "2024-12-31",
    "labSystem": "CPHL",
    "endpoint": "https://cphl.health.gov/api/results"
  }'
```

## Circuit Breaker Configuration

Configure circuit breaker for fault tolerance:

```bash
# Via code or configuration
CircuitBreakerConfig config = new CircuitBreakerConfig()
    .withFailureThreshold(5)        # Trip after 5 failures
    .withTimeoutDuration(60000)     # Try again after 60 seconds
    .withSuccessThreshold(2);       # Need 2 successes to close
```

## Monitoring Configuration

### Enable Debug Logging

```xml
<!-- Add to OpenMRS log4j.xml -->
<logger name="org.openmrs.module.ugandaemrsync">
  <level value="DEBUG"/>
</logger>

<logger name="org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord">
  <level value="TRACE"/>
</logger>
```

### Execution History Monitoring

```sql
-- Monitor profile executions
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

## Troubleshooting Configuration

### Common Issues

#### Issue: Profile Not Executing

```sql
-- Check profile configuration
SELECT name, profile_enabled, schedule_enabled, next_execution_date
FROM sync_fhir_profile 
WHERE name = 'Your Profile Name';

-- Enable profile
UPDATE sync_fhir_profile 
SET profile_enabled = TRUE, schedule_enabled = TRUE 
WHERE name = 'Your Profile Name';
```

#### Issue: Connection Failures

```sql
-- Verify connection details
SELECT name, url, url_user_name 
FROM sync_fhir_profile 
WHERE name = 'Your Profile Name';

-- Update credentials
UPDATE sync_fhir_profile 
SET url = 'https://correct-url.example.com/fhir',
    url_user_name = 'correct-username',
    url_password = 'correct-password'
WHERE name = 'Your Profile Name';
```

---

**Last Updated**: May 2, 2026  
**Version**: 2.0.6-SNAPSHOT