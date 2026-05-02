# API Reference

Complete REST API documentation for the UgandaEMR Sync Module.

## Base URL

All API endpoints are prefixed with:
```
https://your-openmrs/openmrs/ws/rest/v1/
```

## Authentication

All API requests require authentication using Basic Authentication:

```bash
curl -u username:password https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

## Response Format

All responses are in JSON format:

```json
{
  "results": [
    {
      "uuid": "profile-uuid",
      "name": "Profile Name",
      "description": "Profile Description"
    }
  ],
  "totalCount": 1
}
```

## Endpoints

### FHIR Profile Management

#### Create FHIR Profile

**POST** `/syncfhirprofile`

Create a new FHIR synchronization profile.

**Request Body**:
```json
{
  "name": "Patient Data Sync",
  "description": "Sync patient demographics and encounters",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 100,
  "url": "https://central-server.health.gov/fhir",
  "urlUserName": "facility-id",
  "urlPassword": "api-key",
  "syncLimit": 1000,
  "isCaseBasedProfile": false
}
```

**Response**: `201 Created`
```json
{
  "uuid": "new-profile-uuid",
  "name": "Patient Data Sync",
  "description": "Sync patient demographics and encounters",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 100,
  "url": "https://central-server.health.gov/fhir",
  "urlUserName": "facility-id",
  "syncLimit": 1000,
  "dateCreated": "2026-05-02T10:00:00.000Z"
}
```

**Example**:
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

#### Get All FHIR Profiles

**GET** `/syncfhirprofile`

Retrieve all FHIR profiles.

**Query Parameters**:
- `name` (optional): Filter by profile name
- `enabled` (optional): Filter by enabled status (true/false)
- `caseBased` (optional): Filter by case-based profiles (true/false)

**Response**: `200 OK`
```json
{
  "results": [
    {
      "uuid": "profile-uuid-1",
      "name": "Patient Data Sync",
      "description": "Sync patient demographics",
      "resourceTypes": "Patient,Encounter",
      "profileEnabled": true,
      "isCaseBasedProfile": false
    },
    {
      "uuid": "profile-uuid-2",
      "name": "HIV Surveillance",
      "description": "HIV case surveillance",
      "profileEnabled": true,
      "isCaseBasedProfile": true
    }
  ]
}
```

**Example**:
```bash
# Get all profiles
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile

# Get only enabled profiles
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile?enabled=true

# Get case-based profiles
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile?caseBased=true
```

#### Get FHIR Profile by UUID

**GET** `/syncfhirprofile/:uuid`

Retrieve a specific FHIR profile.

**Response**: `200 OK`
```json
{
  "uuid": "profile-uuid",
  "name": "Patient Data Sync",
  "description": "Sync patient demographics and encounters",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 100,
  "url": "https://central-server.health.gov/fhir",
  "urlUserName": "facility-id",
  "syncLimit": 1000,
  "isCaseBasedProfile": false,
  "dateCreated": "2026-05-02T10:00:00.000Z",
  "lastExecutionDate": "2026-05-02T11:00:00.000Z",
  "lastExecutionStatus": "SUCCESS"
}
```

**Example**:
```bash
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID
```

#### Update FHIR Profile

**POST** `/syncfhirprofile/:uuid`

Update an existing FHIR profile.

**Request Body**: Same as Create FHIR Profile

**Response**: `200 OK`
```json
{
  "uuid": "profile-uuid",
  "name": "Updated Profile Name",
  "description": "Updated description",
  "profileEnabled": true
}
```

**Example**:
```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Updated Profile Name",
    "description": "Updated description",
    "profileEnabled": true
  }'
```

#### Delete FHIR Profile

**DELETE** `/syncfhirprofile/:uuid`

Delete a FHIR profile.

**Response**: `204 No Content`

**Example**:
```bash
curl -X DELETE \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID \
  -u admin:Admin123
```

#### Execute FHIR Profile

**POST** `/syncfhirprofile/:uuid/execute`

Manually trigger execution of a FHIR profile.

**Response**: `200 OK`
```json
{
  "status": "triggered",
  "profileUuid": "PROFILE_UUID",
  "executionDate": "2026-05-02T12:00:00.000Z"
}
```

**Example**:
```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID/execute \
  -u admin:Admin123
```

### FHIR Resource Management

#### Get FHIR Resources

**GET** `/syncfhirresource`

Retrieve FHIR resources.

**Query Parameters**:
- `profile` (optional): Filter by profile UUID
- `syncStatus` (optional): Filter by sync status (SYNCED/NOT_SYNCED/FAILED)
- `limit` (optional): Maximum number of results (default: 50)
- `startIndex` (optional): Starting index (default: 0)

**Response**: `200 OK`
```json
{
  "results": [
    {
      "uuid": "resource-uuid",
      "profile": {
        "uuid": "profile-uuid",
        "name": "Patient Data Sync"
      },
      "resourceType": "Patient",
      "resourceUuid": "patient-uuid",
      "fhirResource": "{...FHIR JSON...}",
      "syncStatus": "SYNCED",
      "dateCreated": "2026-05-02T10:00:00.000Z",
      "dateSynced": "2026-05-02T11:00:00.000Z"
    }
  ],
  "totalCount": 100
}
```

**Example**:
```bash
# Get all resources
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource

# Get unsynced resources
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource?syncStatus=NOT_SYNCED

# Get resources for specific profile
curl -u admin:Admin123 \
  "https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource?profile=PROFILE_UUID"
```

#### Get Resource Statistics

**GET** `/syncfhirresource/stats`

Get statistics about FHIR resources.

**Response**: `200 OK`
```json
{
  "totalResources": 1000,
  "syncedResources": 850,
  "unsyncedResources": 120,
  "failedResources": 30,
  "byResourceType": {
    "Patient": 400,
    "Encounter": 350,
    "Observation": 250
  },
  "byProfile": [
    {
      "profileName": "Patient Data Sync",
      "total": 500,
      "synced": 450,
      "unsynced": 40,
      "failed": 10
    }
  ]
}
```

**Example**:
```bash
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource/stats
```

### Lab Results Integration

#### Request Lab Results

**POST** `/requestlabresults`

Request lab results from an external lab system.

**Request Body**:
```json
{
  "patientIdentifier": "PAT-001",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31",
  "labSystem": "CPHL"
}
```

**Response**: `200 OK`
```json
{
  "status": "success",
  "message": "Lab results requested successfully",
  "requestId": "request-uuid",
  "patientIdentifier": "PAT-001",
  "resultsCount": 5
}
```

**Example**:
```bash
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

### Execution History

#### Get Execution History

**GET** `/syncfhirprofile/:uuid/history`

Get execution history for a specific profile.

**Query Parameters**:
- `limit` (optional): Maximum number of results (default: 20)
- `startIndex` (optional): Starting index (default: 0)

**Response**: `200 OK`
```json
{
  "results": [
    {
      "uuid": "history-uuid",
      "executionDate": "2026-05-02T11:00:00.000Z",
      "executionStatus": "SUCCESS",
      "resourcesProcessed": 100,
      "executionDurationMs": 5000,
      "errorMessage": null
    },
    {
      "uuid": "history-uuid-2",
      "executionDate": "2026-05-02T10:00:00.000Z",
      "executionStatus": "FAILED",
      "resourcesProcessed": 50,
      "executionDurationMs": 3000,
      "errorMessage": "Connection timeout"
    }
  ]
}
```

**Example**:
```bash
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID/history
```

## Error Responses

All endpoints may return error responses:

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

### 403 Forbidden
```json
{
  "error": "Forbidden",
  "message": "Insufficient privileges"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

## Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 204 | No Content - Resource deleted successfully |
| 400 | Bad Request - Invalid request parameters |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient privileges |
| 404 | Not Found - Resource not found |
| 500 | Internal Server Error - Server error |

## Rate Limiting

API requests are rate-limited to prevent abuse:
- Default limit: 1000 requests per hour per user
- Headers included in response:
  - `X-RateLimit-Limit`: Request limit
  - `X-RateLimit-Remaining`: Remaining requests
  - `X-RateLimit-Reset`: Reset time

## Testing

### Test Authentication

```bash
# Test basic authentication
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

### Test Profile Creation

```bash
# Create test profile
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Test Profile",
    "description": "Test profile for API testing",
    "resourceTypes": "Patient",
    "profileEnabled": false,
    "generateBundle": true,
    "numberOfResourcesInBundle": 10
  }'
```

### Test Profile Execution

```bash
# Trigger manual execution
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID/execute \
  -u admin:Admin123
```

## Best Practices

1. **Always use HTTPS** in production environments
2. **Implement proper error handling** for all API calls
3. **Use filtering parameters** to reduce response size
4. **Implement exponential backoff** for rate limiting
5. **Cache frequently accessed data** to reduce API calls
6. **Monitor execution history** for troubleshooting
7. **Use appropriate privilege levels** for different use cases

---

**Last Updated**: May 2, 2026  
**API Version**: 1.0  
**Module Version**: 2.0.6-SNAPSHOT