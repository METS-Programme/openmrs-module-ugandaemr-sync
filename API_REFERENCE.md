# UgandaEMR Sync Module - API Reference

Complete REST API reference for the UgandaEMR Sync Module.

## 🌐 Base URL

All API endpoints are prefixed with the OpenMRS REST API base URL:

```
https://your-openmrs-server/openmrs/ws/rest/v1/
```

## 🔐 Authentication

All endpoints require authentication. Use one of the following methods:

### Basic Authentication
```bash
curl -u username:password https://your-openmrs/openmrs/ws/rest/v1/endpoint
```

### Session Authentication
```bash
# First, authenticate and get session
curl -X POST https://your-openmrs/openmrs/ws/rest/v1/session \
  -u username:password

# Use session ID in subsequent requests
curl -https://your-openmrs/openmrs/ws/rest/v1/endpoint \
  -Cookie JSESSIONID=your-session-id
```

## 📚 API Endpoints

### FHIR Profile Management

#### Get All FHIR Profiles
```http
GET /ws/rest/v1/syncfhirprofile
```

**Response:**
```json
{
  "results": [
    {
      "uuid": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
      "name": "HIV Case Surveillance",
      "resourceTypes": "Patient,Condition,Observation",
      "profileEnabled": true,
      "isCaseBasedProfile": true,
      "url": "https://central-server.example.com/fhir",
      "display": "HIV Case Surveillance"
    }
  ]
}
```

#### Get Specific Profile
```http
GET /ws/rest/v1/syncfhirprofile/:uuid
```

**Parameters:**
- `uuid` (required) - Profile UUID or ID

**Example:**
```bash
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/a1b2c3d4-5678-90ab-cdef-1234567890ab
```

#### Create FHIR Profile
```http
POST /ws/rest/v1/syncfhirprofile
```

**Required Privilege:** `UgandaemrSync: Manage FHIR Profiles`

**Request Body:**
```json
{
  "name": "DHIS2 Reporting",
  "description": "Aggregate data reporting to DHIS2",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 50,
  "url": "https://dhis2.example.com/api/fhir",
  "urlUserName": "dhis2_user",
  "urlPassword": "dhis2_password",
  "syncLimit": 1000,
  "durationToKeepSyncedResources": 30
}
```

**Response:** `201 Created`
```json
{
  "uuid": "b2c3d4e5-6789-01ab-cdef-234567890abc",
  "name": "DHIS2 Reporting",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "display": "DHIS2 Reporting"
}
```

#### Update FHIR Profile
```http
POST /ws/rest/v1/syncfhirprofile/:uuid
```

**Required Privilege:** `UgandaemrSync: Manage FHIR Profiles`

**Request Body:** (partial update)
```json
{
  "profileEnabled": false,
  "syncLimit": 500
}
```

#### Delete FHIR Profile
```http
DELETE /ws/rest/v1/syncfhirprofile/:uuid
```

**Required Privilege:** `UgandaemrSync: Manage FHIR Profiles`

#### Get Profile with Full Representation
```http
GET /ws/rest/v1/syncfhirprofile/:uuid?v=full
```

**Response:** Includes all profile fields including scheduling configuration, authentication details, and execution history.

---

### FHIR Resource Management

#### Get All FHIR Resources
```http
GET /ws/rest/v1/syncfhirresource
```

**Query Parameters:**
- `profile` (optional) - Filter by profile UUID
- `syncStatus` (optional) - Filter by sync status (true/false)
- `limit` (optional) - Maximum number of results (default: 10)
- `startIndex` (optional) - Starting index (default: 0)

**Examples:**
```bash
# Get unsynced resources
curl -u admin:Admin123 \
  "https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource?syncStatus=false"

# Get resources for specific profile
curl -u admin:Admin123 \
  "https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource?profile=profile-uuid&limit=50"
```

**Response:**
```json
{
  "results": [
    {
      "uuid": "c3d4e5f6-7890-12ab-cdef-34567890abcd",
      "resource": "Patient resource JSON",
      "resourceType": "Patient",
      "synced": false,
      "dateCreated": "2024-05-01T10:30:00Z",
      "profile": {
        "uuid": "profile-uuid",
        "display": "HIV Surveillance"
      }
    }
  ]
}
```

#### Get Resource Statistics
```http
GET /ws/rest/v1/syncfhirresource/stats
```

**Response:**
```json
{
  "totalResources": 15420,
  "syncedResources": 12350,
  "unsyncedResources": 3070,
  "profiles": [
    {
      "profileName": "HIV Surveillance",
      "total": 8500,
      "synced": 7200,
      "unsynced": 1300
    }
  ]
}
```

#### Get Specific Resource Details
```http
GET /ws/rest/v1/syncfhirresourcedetails/:uuid
```

**Response:** Full resource including FHIR content, sync status, and metadata.

---

### FHIR Case Management

#### Get All Cases
```http
GET /ws/rest/v1/syncfhircase
```

**Query Parameters:**
- `profile` (optional) - Filter by profile UUID
- `status` (optional) - Filter by case status
- `startDate` (optional) - Filter by creation date (from)
- `endDate` (optional) - Filter by creation date (to)

#### Create New Case
```http
POST /ws/rest/v1/syncfhircase
```

**Required Privilege:** `UgandaemrSync: Manage FHIR Cases`

**Request Body:**
```json
{
  "profile": "profile-uuid",
  "caseIdentifier": "HIV-2024-001",
  "patient": "patient-uuid",
  "primaryResource": "condition-uuid",
  "caseStatus": "OPEN",
  "description": "New HIV case for surveillance"
}
```

#### Update Case Status
```http
POST /ws/rest/v1/syncfhircase/:uuid
```

**Request Body:**
```json
{
  "caseStatus": "CLOSED",
  "closureReason": "Case resolved"
}
```

---

### Sync Task Management

#### Get All Sync Tasks
```http
GET /ws/rest/v1/synctask
```

**Query Parameters:**
- `taskType` (optional) - Filter by task type UUID
- `status` (optional) - Filter by status (PENDING, IN_PROGRESS, COMPLETED, FAILED)

#### Get Task Details
```http
GET /ws/rest/v1/synctaskdetails/:uuid
```

**Response:**
```json
{
  "uuid": "task-uuid",
  "syncTaskType": {
    "uuid": "task-type-uuid",
    "name": "Lab Results Request",
    "display": "Lab Results Request"
  },
  "dateCreated": "2024-05-01T10:30:00Z",
  "status": "COMPLETED",
  "statusCode": 200,
  "statusMessage": "Successfully retrieved lab results",
  "request": "Request details",
  "response": "Response details"
}
```

#### Create Sync Task
```http
POST /ws/rest/v1/synctask
```

**Required Privilege:** `UgandaemrSync: Manage Sync Tasks`

**Request Body:**
```json
{
  "syncTaskType": "task-type-uuid",
  "dateCreated": "2024-05-01",
  "request": "Task request details"
}
```

#### Get Task Statistics
```http
GET /ws/rest/v1/synctaskstats
```

**Query Parameters:**
- `startDate` (optional) - Filter by start date
- `endDate` (optional) - Filter by end date

**Response:**
```json
{
  "totalTasks": 5420,
  "completedTasks": 4850,
  "failedTasks": 450,
  "pendingTasks": 120,
  "byType": [
    {
      "taskTypeName": "Lab Results Request",
      "total": 2500,
      "completed": 2200,
      "failed": 250,
      "pending": 50
    }
  ]
}
```

---

### Task Type Management

#### Get All Task Types
```http
GET /ws/rest/v1/synctasktype
```

**Response:**
```json
{
  "results": [
    {
      "uuid": "task-type-uuid",
      "name": "Lab Results Request",
      "description": "Request lab results from CPHL",
      "taskUrl": "https://cphl.example.com/api/lab-results",
      "requiresAuthentication": true,
      "display": "Lab Results Request"
    }
  ]
}
```

#### Create Task Type
```http
POST /ws/rest/v1/synctasktype
```

**Required Privilege:** `UgandaemrSync: Manage Sync Task Types`

**Request Body:**
```json
{
  "name": "Custom Data Export",
  "description": "Export custom data format",
  "taskUrl": "https://external-system.com/api/export",
  "requestType": "POST",
  "requiresAuthentication": true,
  "authenticationMethod": "BASIC"
}
```

---

### Lab Results Integration

#### Request Lab Results
```http
POST /ws/rest/v1/requestlabresults
```

**Required Privilege:** `UgandaemrSync: Manage Lab Orders`

**Request Body:**
```json
{
  "patientIdentifier": "PAT-001",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31",
  "labSystem": "CPHL",
  "testTypes": ["Viral Load", "CD4 Count"]
}
```

**Response:** `202 Accepted`
```json
{
  "taskId": "task-uuid",
  "status": "PENDING",
  "message": "Lab results request submitted successfully"
}
```

#### Receive Lab Results
```http
POST /ws/rest/v1/recievelabresult
```

**Required Privilege:** `UgandaemrSync: Manage Lab Results`

**Request Body:**
```json
{
  "results": "FHIR Bundle JSON with DiagnosticReport resources",
  "sourceSystem": "CPHL",
  "receivedDate": "2024-05-01T10:30:00Z"
}
```

**Response:** `200 OK`
```json
{
  "status": "SUCCESS",
  "processedResults": 15,
  "failedResults": 0,
  "message": "Lab results processed successfully"
}
```

---

### Referral Management

#### Create Referral Order
```http
POST /ws/rest/v1/referralorder
```

**Required Privilege:** `UgandaemrSync: Manage Referrals`

**Request Body:**
```json
{
  "patient": "patient-uuid",
  "referringFacility": "current-facility-uuid",
  "receivingFacility": "referral-facility-uuid",
  "referralReason": "Require specialized care",
  "urgency": "URGENT",
  "referralDate": "2024-05-01"
}
```

#### Get Referral Status
```http
GET /ws/rest/v1/referralorder/:uuid
```

**Response:**
```json
{
  "uuid": "referral-uuid",
  "patient": {
    "uuid": "patient-uuid",
    "display": "John Doe"
  },
  "referralStatus": "PENDING",
  "referralDate": "2024-05-01T10:30:00Z",
  "receivingFacility": {
    "name": "Regional Referral Hospital"
  }
}
```

---

### Reporting Integration

#### Send Report
```http
POST /ws/rest/v1/sendreports
```

**Required Privilege:** `UgandaemrSync: Send Reports`

**Request Body:**
```json
{
  "reportType": "MONTHLY_AGGREGATE",
  "reportDate": "2024-05-01",
  "dataFormat": "JSON",
  "destination": "DHIS2",
  "reportData": {
    "indicators": [
      {
        "indicatorId": "HIV_PATIENTS",
        "value": 150
      }
    ]
  }
}
```

**Response:** `200 OK`
```json
{
  "status": "SUCCESS",
  "reportId": "report-uuid",
  "message": "Report sent successfully to DHIS2"
}
```

---

### Profile Execution History

#### Get Execution History
```http
GET /ws/rest/v1/syncfhirprofilelog
```

**Query Parameters:**
- `profile` (optional) - Filter by profile UUID
- `startDate` (optional) - Filter by start date
- `endDate` (optional) - Filter by end date
- `limit` (optional) - Maximum results (default: 50)

**Response:**
```json
{
  "results": [
    {
      "uuid": "log-uuid",
      "profile": {
        "uuid": "profile-uuid",
        "display": "HIV Surveillance"
      },
      "lastGenerationDate": "2024-05-01T11:00:00Z",
      "numberOfResources": 250,
      "resourceType": "Condition",
      "executionStatus": "COMPLETED",
      "executionDuration": 125000
    }
  ]
}
```

#### Get Latest Execution Status
```http
GET /ws/rest/v1/syncfhirprofilelog/latest
```

**Query Parameters:**
- `profile` (required) - Profile UUID

**Response:**
```json
{
  "lastExecutionDate": "2024-05-01T11:00:00Z",
  "lastExecutionStatus": "COMPLETED",
  "nextExecutionDate": "2024-05-01T12:00:00Z",
  "isRunning": false,
  "lastExecutionDuration": 125000,
  "resourcesProcessed": 250,
  "errorMessage": null
}
```

---

## 🔒 Error Responses

All endpoints may return error responses in the following format:

```json
{
  "error": {
    "message": "Error description",
    "code": "ERROR_CODE",
    "details": {
      "field": "Additional error details"
    }
  }
}
```

### Common HTTP Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `202 Accepted` - Request accepted for processing
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient privileges
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate)
- `500 Internal Server Error` - Server error
- `503 Service Unavailable` - Service temporarily unavailable

### Error Examples

**Authentication Error**
```json
{
  "error": {
    "message": "Not authenticated",
    "code": "UNAUTHORIZED"
  }
}
```

**Validation Error**
```json
{
  "error": {
    "message": "Validation failed",
    "code": "VALIDATION_ERROR",
    "details": {
      "field": "resourceTypes",
      "rejectedValue": "InvalidResource",
      "message": "Invalid FHIR resource type"
    }
  }
}
```

**Privilege Error**
```json
{
  "error": {
    "message": "Access denied. Required privilege: UgandaemrSync: Manage FHIR Profiles",
    "code": "INSUFFICIENT_PRIVILEGES"
  }
}
```

---

## 🎯 Common Use Cases

### Use Case 1: Set Up New FHIR Profile

```bash
# 1. Create the profile
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Patient Data Export",
    "resourceTypes": "Patient,Encounter",
    "profileEnabled": true,
    "url": "https://central-server.com/fhir",
    "urlUserName": "facility-user",
    "urlPassword": "api-key"
  }'

# 2. Verify profile creation
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile

# 3. Set up scheduled task via OpenMRS Scheduler UI
# Task: Generic FHIR Profile Scheduler
# Schedule: Run every hour
```

### Use Case 2: Monitor Sync Operations

```bash
# Check profile status
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofilelog/latest?profile=profile-uuid

# Get unsynced resources count
curl -u admin:Admin123 \
  "https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource?syncStatus=false&limit=1"

# Get execution history
curl -u admin:Admin123 \
  "https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofilelog?profile=profile-uuid&limit=10"
```

### Use Case 3: Request Lab Results

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

# Check task status
curl -u admin:Admin123 \
  https://your-openmrs/openmrs/ws/rest/v1/synctaskdetails/task-uuid
```

---

## 🧪 Testing the API

### Using cURL

```bash
# Simple GET request
curl -u username:password \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile

# POST request with data
curl -X POST \
  -H "Content-Type: application/json" \
  -u username:password \
  -d '{"name":"Test Profile"}' \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile

# Pretty print JSON response
curl -u username:password \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile | jq '.'
```

### Using Postman

1. **Set Authentication**
   - Type: Basic Auth
   - Username: your-username
   - Password: your-password

2. **Create Request**
   - Method: POST
   - URL: `https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile`
   - Headers: `Content-Type: application/json`
   - Body: JSON payload

3. **Save and Organize**
   - Create collections for different endpoints
   - Use environment variables for base URL
   - Save authentication details

---

## 📊 Rate Limiting

Currently, there are no strict rate limits enforced by the module. However, consider:

1. **Database Performance**: Large result sets may impact database performance
2. **Network Bandwidth**: Large FHIR bundles consume bandwidth
3. **External Systems**: Respect rate limits of external systems

**Recommended Best Practices:**
- Use pagination for large result sets
- Limit batch sizes to reasonable values (50-100 resources)
- Implement client-side caching for frequently accessed data
- Use appropriate HTTP methods (GET vs POST)

---

## 🔍 Filtering and Pagination

### Pagination

Most list endpoints support pagination:

```
GET /ws/rest/v1/syncfhirresource?limit=20&startIndex=40
```

- `limit`: Number of results per page (default: 10, max: 100)
- `startIndex`: Starting index (0-based)

### Filtering

```
GET /ws/rest/v1/syncfhirresource?syncStatus=false&profile=profile-uuid
```

### Sorting

Not all endpoints support sorting. Check individual endpoint documentation.

---

## 🔄 Webhooks and Callbacks

Currently, the module does not support webhooks. All status checks are done via polling.

---

## 📝 Changelog

### Version 2.0.6 (Current)
- Added execution history endpoints
- Enhanced filtering capabilities
- Improved error messages
- Added statistics endpoints

### Version 2.0.5
- Initial REST API implementation
- Core CRUD operations for profiles, resources, tasks
- Lab results integration endpoints

---

## 🆘 Support and Troubleshooting

### Common Issues

1. **Authentication Failed**
   - Verify credentials
   - Check user has required privileges
   - Ensure account is active

2. **Resource Not Found**
   - Verify UUID is correct
   - Check resource hasn't been deleted
   - Ensure proper permissions

3. **Validation Errors**
   - Check request body format
   - Verify required fields are present
   - Ensure data types are correct

4. **Server Errors**
   - Check OpenMRS logs
   - Verify database connectivity
   - Ensure sufficient memory

### Debug Mode

Enable detailed logging:

```xml
<!-- Add to log4j.xml -->
<logger name="org.openmrs.module.webservices.rest">
  <level value="DEBUG"/>
</logger>
```

---

## 📚 Additional Resources

- [OpenMRS REST API Documentation](https://wiki.openmrs.org/display/docs/REST+Web+Service+API)
- [FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [Module README](README.md)
- [Quick Start Guide](QUICKSTART.md)
- [Developer Guide](DEVELOPER_GUIDE.md)

---

**Last Updated**: May 2, 2026  
**API Version**: 1.0  
**Module Version**: 2.0.6-SNAPSHOT