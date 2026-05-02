# FHIR Profiles

Comprehensive guide to FHIR (Fast Healthcare Interoperability Resources) profiles in the UgandaEMR Sync Module.

## Overview

FHIR Profiles define how healthcare data is transformed and exchanged between OpenMRS and external systems. The module supports two types of profiles:

1. **Resource-based Profiles**: Sync specific FHIR resource types
2. **Case-based Profiles**: Advanced disease surveillance with case management

## Profile Types

### Resource-based Profiles

Designed for straightforward data synchronization of standard FHIR resource types.

#### Supported Resource Types

- **Patient**: Patient demographics and administrative information
- **Encounter**: Patient visits and interactions
- **Observation**: Clinical observations and measurements
- **Condition**: Health conditions and diagnoses
- **Procedure**: Medical procedures and interventions
- **MedicationRequest**: Medication orders and prescriptions
- **Location**: Healthcare facilities and locations
- **Practitioner**: Healthcare providers and staff

#### Example: Patient Data Sync

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

### Case-based Profiles

Designed for disease surveillance and case reporting with complex data relationships.

#### Features

- **Case Identification**: Automatically identify new cases based on conditions
- **Case Management**: Track case status and progression
- **Resource Linking**: Link all related resources to a case
- **Surveillance Reporting**: Generate surveillance reports

#### Example: HIV Case Surveillance

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

## Profile Configuration

### Basic Configuration Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Unique profile name |
| `description` | String | No | Profile description |
| `resourceTypes` | String | Yes | Comma-separated FHIR resource types |
| `profileEnabled` | Boolean | No | Enable/disable profile (default: false) |
| `isCaseBasedProfile` | Boolean | No | Case-based or resource-based (default: false) |

### Scheduling Configuration

| Field | Type | Description |
|-------|------|-------------|
| `scheduleEnabled` | Boolean | Enable scheduled execution |
| `scheduleType` | String | Schedule type: FIXED_RATE, FIXED_DELAY, CRON, MANUAL |
| `fixedRateInterval` | Long | Interval in milliseconds (for FIXED_RATE) |
| `fixedDelayInterval` | Long | Delay in milliseconds (for FIXED_DELAY) |
| `cronExpression` | String | Cron expression (for CRON) |
| `executionPriority` | Integer | Execution priority (1-10, 1=highest) |

### Data Processing Configuration

| Field | Type | Description |
|-------|------|-------------|
| `generateBundle` | Boolean | Generate FHIR bundles (default: true) |
| `numberOfResourcesInBundle` | Integer | Resources per bundle (default: 50) |
| `syncLimit` | Integer | Maximum resources to sync per run (unlimited if null) |
| `resourceSearchParameter` | String | Custom search parameters for resource queries |

### External System Configuration

| Field | Type | Description |
|-------|------|-------------|
| `url` | String | External system endpoint URL |
| `urlUserName` | String | Authentication username |
| `urlPassword` | String | Authentication password |
| `httpMethod` | String | HTTP method: POST, PUT (default: POST) |

### Case-based Configuration

| Field | Type | Description |
|-------|------|-------------|
| `caseBasedPrimaryResourceType` | String | Primary resource type for case identification |
| `caseBasedPrimaryResourceTypeId` | String | Concept UUID for case identification |
| `caseBasedArchivePeriod` | Integer | Days before archiving old cases |
| `caseBasedCaseStatus` | String | Default case status |

## Common Profile Patterns

### 1. High-Volume Data Sync

```bash
{
  "name": "High-Volume Patient Sync",
  "resourceTypes": "Patient,Encounter",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 100,
  "syncLimit": 2000,
  "scheduleEnabled": true,
  "scheduleType": "FIXED_RATE",
  "fixedRateInterval": 1800000,
  "executionPriority": 3
}
```

### 2. Real-time Updates

```bash
{
  "name": "Real-time Lab Results",
  "resourceTypes": "Observation",
  "resourceSearchParameter": "conceptUuid=lab-result-concept-uuid",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 10,
  "syncLimit": 100,
  "scheduleEnabled": true,
  "scheduleType": "FIXED_RATE",
  "fixedRateInterval": 300000,
  "executionPriority": 1
}
```

### 3. Disease Surveillance

```bash
{
  "name": "COVID-19 Surveillance",
  "isCaseBasedProfile": true,
  "caseBasedPrimaryResourceType": "Condition",
  "caseBasedPrimaryResourceTypeId": "covid19-concept-uuid",
  "resourceTypes": "Patient,Condition,Observation,Encounter,Location",
  "profileEnabled": true,
  "scheduleEnabled": true,
  "scheduleType": "FIXED_RATE",
  "fixedRateInterval": 86400000,
  "executionPriority": 2
}
```

### 4. Reporting and Analytics

```bash
{
  "name": "Aggregate Data Reporting",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 500,
  "syncLimit": 5000,
  "scheduleEnabled": true,
  "scheduleType": "CRON",
  "cronExpression": "0 0 0 * * ?",
  "executionPriority": 8
}
```

## FHIR Resource Mapping

### Patient Resource

```json
{
  "resourceType": "Patient",
  "id": "patient-uuid",
  "identifier": [
    {
      "system": "http://facility-identifiers.org",
      "value": "PAT-001"
    }
  ],
  "name": [
    {
      "family": "Doe",
      "given": ["John"],
      "use": "official"
    }
  ],
  "gender": "male",
  "birthDate": "1980-01-01",
  "address": [
    {
      "use": "home",
      "district": "Kampala",
      "country": "Uganda"
    }
  ]
}
```

### Encounter Resource

```json
{
  "resourceType": "Encounter",
  "id": "encounter-uuid",
  "subject": {
    "reference": "Patient/patient-uuid",
    "display": "John Doe"
  },
  "encounterType": {
    "coding": [
      {
        "system": "http://encounter-types.org",
        "code": "OPD"
      }
    ]
  },
  "period": {
    "start": "2026-05-02T10:00:00.000Z",
    "end": "2026-05-02T11:00:00.000Z"
  },
  "location": [
    {
      "location": {
        "reference": "Location/location-uuid",
        "display": "Outpatient Department"
      }
    }
  ]
}
```

### Observation Resource

```json
{
  "resourceType": "Observation",
  "id": "observation-uuid",
  "status": "final",
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "8480-6",
        "display": "Systolic blood pressure"
      }
    ]
  },
  "subject": {
    "reference": "Patient/patient-uuid",
    "display": "John Doe"
  },
  "encounter": {
    "reference": "Encounter/encounter-uuid"
  },
  "valueQuantity": {
    "value": 120,
    "unit": "mmHg",
    "system": "http://unitsofmeasure.org",
    "code": "mm[Hg]"
  }
}
```

## Profile Execution Flow

### Resource-based Profile Execution

1. **Load Profile**: Retrieve profile configuration
2. **Query Data**: Query OpenMRS for specified resource types
3. **Transform**: Convert OpenMRS data to FHIR format
4. **Bundle**: Create FHIR bundles with specified batch size
5. **Save**: Store FHIR resources in database
6. **Send**: Send to external system (if URL configured)
7. **Log**: Record execution history

### Case-based Profile Execution

1. **Identify Cases**: Find new cases based on primary resource type
2. **Create Case Records**: Create SyncFhirCase records
3. **Gather Related Data**: Collect all related resources for each case
4. **Transform**: Convert to FHIR format
5. **Bundle**: Create case-specific FHIR bundles
6. **Send**: Submit to external system
7. **Update Status**: Update case and resource status
8. **Archive**: Archive old cases as configured

## Profile Monitoring

### Check Execution Status

```sql
-- View profile execution status
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

### View Execution History

```sql
-- Get recent execution history
SELECT 
  sp.name as profile,
  eph.execution_date,
  eph.execution_status,
  eph.resources_processed,
  eph.execution_duration_ms,
  eph.error_message
FROM sync_fhir_profile_execution_history eph
JOIN sync_fhir_profile sp ON eph.profile_id = sp.sync_fhir_profile_id
WHERE sp.uuid = 'profile-uuid'
ORDER BY eph.execution_date DESC
LIMIT 20;
```

## Best Practices

### 1. Profile Naming
- Use descriptive, unique names
- Include purpose or destination in name
- Example: "Central Server Patient Sync", "DHIS2 Aggregation"

### 2. Resource Type Selection
- Only include necessary resource types
- Avoid over-inclusion of resources
- Consider data volume and frequency

### 3. Batch Size Tuning
- Start with default (50 resources per bundle)
- Increase for high-volume profiles (100-200)
- Decrease for low-latency requirements (10-25)

### 4. Sync Limits
- Always set sync limits for production profiles
- Prevents runaway processes
- Consider daily data volume

### 5. Priority Assignment
- Critical data: priority 1-3
- Important data: priority 4-6
- Background tasks: priority 7-10

### 6. Scheduling Strategy
- Real-time data: 5-15 minute intervals
- Surveillance data: Hourly to daily
- Reporting/analytics: Daily to weekly

### 7. Error Handling
- Monitor execution history regularly
- Set up alerts for failed executions
- Review error messages and logs

### 8. Security
- Use dedicated service accounts
- Rotate credentials regularly
- Monitor access logs

## Troubleshooting

### Profile Not Executing

1. Check if profile is enabled
2. Verify scheduler is running
3. Review execution history for errors
4. Check next_execution_date

### No Resources Generated

1. Verify data exists in OpenMRS
2. Check resource search parameters
3. Review query filters
4. Enable debug logging

### Connection Failures

1. Test external system connectivity
2. Verify credentials and URL
3. Check SSL/TLS configuration
4. Review firewall settings

### Performance Issues

1. Reduce batch size
2. Add sync limit
3. Adjust schedule interval
4. Review database performance

---

**Last Updated**: May 2, 2026  
**FHIR Version**: R4  
**Module Version**: 2.0.6-SNAPSHOT