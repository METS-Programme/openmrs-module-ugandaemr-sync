# UgandaEMR Sync Module

[![License](https://img.shields.io/badge/license-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![OpenMRS](https://img.shields.io/badge/OpenMRS-2.7%2B-blue.svg)](https://openmrs.org)
[![Version](https://img.shields.io/badge/version-2.0.6--SNAPSHOT-orange.svg)](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync)

## Overview

The UgandaEMR Sync Module provides comprehensive data sharing and health information exchange (HIE) capabilities for OpenMRS installations. It enables seamless interoperability between facility-level EMR systems and external health information systems using FHIR (Fast Healthcare Interoperability Resources) standards.

### Key Capabilities

- **FHIR-based Data Exchange**: HL7 FHIR R4 standard for healthcare data interoperability
- **Multi-system Integration**: Connect with DHIS2, central servers, lab systems, and other HIEs
- **Case-based Surveillance**: Advanced disease surveillance and reporting capabilities
- **Resource-based Sync**: Flexible FHIR resource synchronization profiles
- **Bi-directional Communication**: Send data to and receive data from external systems
- **Secure & Reliable**: Enterprise-grade security with circuit breaker patterns and retry mechanisms

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Core Components](#core-components)
- [Integration Types](#integration-types)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Security](#security)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)

## Features

### 🔥 FHIR Profile Management
- **Resource-based Profiles**: Sync specific FHIR resource types (Patient, Observation, Encounter, etc.)
- **Case-based Profiles**: Advanced surveillance profiles with case management
- **Profile Scheduling**: Configurable execution schedules with anti-blocking protection
- **Bulk Operations**: Efficient processing of large datasets with configurable batch sizes

### 🔄 Data Exchange Capabilities
- **Push to Central Servers**: Upload patient data, lab results, and surveillance reports
- **Pull from External Systems**: Import lab results, patient data, and reference data
- **DHIS2 Integration**: Aggregate data reporting to DHIS2 health information systems
- **Lab System Integration**: Connect with central public health laboratories (CPHL)
- **Cross-border Exchange**: Share patient data across national boundaries

### ⚙️ Advanced Features
- **Scheduled Tasks**: Automated synchronization with configurable intervals
- **Execution History**: Comprehensive audit trail of all sync operations
- **Error Handling**: Robust error handling with retry mechanisms
- **Circuit Breaker Pattern**: Prevent cascading failures when external systems are down
- **Resource Management**: Connection pooling and efficient resource utilization
- **Validation Framework**: Input validation and data integrity checks

### 🛡️ Security & Compliance
- **Role-based Access Control**: Fine-grained permissions for different user roles
- **Secure Communication**: HTTPS/TLS support with certificate validation
- **Audit Logging**: Complete audit trail of all operations
- **Data Encryption**: Support for encrypted data transmission

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    UgandaEMR Installation                   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           UgandaEMR Sync Module                      │   │
│  │                                                      │   │ 
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │   │
│  │  │ FHIR Profile │  │   Scheduled  │  │  REST API  │  │   │
│  │  │   Manager    │  │    Tasks     │  │  Endpoints │  │   │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │   │
│  │                                                      │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │   │
│  │  │   FHIR       │  │  Circuit     │  │  Security  │  │   │
│  │  │  Generator   │  │   Breaker    │  │  Manager   │  │   │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↓↑
                    HTTP/HTTPS (FHIR JSON)
                           ↓↑
┌─────────────────────────────────────────────────────────────┐
│                    External Systems                         │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐    │
│  │  eHMIS   │  │ National │  │CentralLab│  │   Other   │    │
│  │ (DHIS2)  │  │   DWH    │  │ Systems  │  │    HIE    │    │
│  └──────────┘  └──────────┘  └──────────┘  └───────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

#### Outbound Data Flow (Push)
```
OpenMRS Data → FHIR Resource Generator → Bundle Creation → 
HTTP Client → Circuit Breaker → External System → Response Processing
```

#### Inbound Data Flow (Pull)
```
External System → HTTP Request → Authentication → 
FHIR Parser → Data Validation → OpenMRS Integration → Confirmation
```

## Core Components

### 1. FHIR Profile System
**Purpose**: Define what data to sync and how to transform it

**Key Classes**:
- `SyncFhirProfile`: Core profile configuration model
- `SyncFhirResource`: Generated FHIR resources awaiting synchronization
- `SyncFhirCase`: Case-based surveillance data management
- `FhirQueryExecutor`: Query execution and resource extraction
- `FhirResourceTransformer`: Data transformation to FHIR format

### 2. Scheduled Task System
**Purpose**: Automate data exchange operations

**Key Classes**:
- `GenericFhirProfileSchedulerTask`: Universal profile scheduler (recommended)
- Profile-specific tasks (legacy): `ClientRegistryIntegrationTask`, `ECBSSIntegrationTask`, etc.
- `SendFhirResourceTask`: Send generated resources to external systems
- `ReceiveVisitsDataFromARTAccessTask`: Import patient visit data

### 3. HTTP Communication Layer
**Purpose**: Handle external system communication

**Key Classes**:
- `UgandaEMRHttpURLConnection`: HTTP client with connection pooling
- `SSLConfiguration`: Secure SSL/TLS configuration
- `CircuitBreaker`: Circuit breaker pattern for fault tolerance
- `CircuitBreakerRegistry`: Circuit breaker state management

### 4. Security Layer
**Purpose**: Authentication, authorization, and security

**Key Classes**:
- `ResourceSecurityInterceptor`: REST API security interceptor
- `Secured`: Method-level security annotation
- `SyncPrivileges`: Privilege definitions
- `ApiKeyAuthentication`: API key authentication support

### 5. REST API Layer
**Purpose**: Provide RESTful endpoints for module management

**Key Resources**:
- `SyncFhirProfileResource`: Manage FHIR profiles
- `SyncFhirResourceResource`: Manage generated resources
- `SyncFhirCaseResource`: Manage case-based surveillance
- `SyncTaskResource`: Manage sync tasks
- `SyncTaskTypeResource`: Manage task types
- `RequestLabResultsResource`: Request lab results from external systems
- `ReferralOrderResource`: Manage referral orders

## Integration Types

### 1. DHIS2 Integration
**Purpose**: Aggregate data reporting for district health information systems

**Features**:
- Automated data aggregation
- Configurable data sets and indicators
- Scheduled reporting
- Data validation and quality checks

**Configuration**:
```properties
ugandaemrsync.sendtoDHIS2.server.url=https://dhis2.example.com
ugandaemrsync.sendtoDHIS2.server.username=username
ugandaemrsync.sendtoDHIS2.server.password=password
```

### 2. Central Server Integration
**Purpose**: Share patient data with central/national servers

**Features**:
- Patient demographics synchronization
- Encounter data sharing
- Lab results reporting
- Analytics data submission

**Profile Types**:
- HIV Case Based Surveillance
- Mortality Surveillance
- Viral Load Reporting
- TB Surveillance
- COVID-19 Reporting

### 3. Lab System Integration
**Purpose**: Exchange laboratory data with central labs

**Features**:
- Lab order submission
- Lab results retrieval
- Result status tracking
- Error handling and retry logic

**Supported Systems**:
- Central Public Health Laboratories (CPHL)
- Regional lab hubs
- Private lab networks

### 4. Health Information Exchange (HIE)
**Purpose**: Inter-facility data exchange

**Features**:
- Client registry integration
- Cross-border data sharing
- Patient data portability
- Referral management

## Installation & Setup

### Prerequisites
- OpenMRS 2.7+ or OpenMRS 3.x
- Java 8 or higher
- Required OpenMRS modules:
  - `fhir2` (FHIR R4 implementation)
  - `webservices.rest` (REST API)
  - `ugandaemrreports` (Reporting)
  - `idgen` (Identifier generation)
  - `stockmanagement` (Stock management)

### Installation Steps

1. **Build the Module**
   ```bash
   git clone https://github.com/METS-Programme/openmrs-module-ugandaemr-sync.git
   cd openmrs-module-ugandaemr-sync
   mvn clean install
   ```

2. **Deploy the Module**
   - Copy `omod/target/openmrs-module-ugandaemr-sync-2.0.6-SNAPSHOT.oam` to OpenMRS modules directory
   - Restart OpenMRS or use Module Management UI to upload

3. **Configure Global Properties**
   - Navigate to Administration → Advanced Settings → Global Properties
   - Configure server URLs, credentials, and sync settings

4. **Set Up User Privileges**
   - Grant appropriate privileges to users
   - Configure role-based access control

5. **Create FHIR Profiles**
   - Use REST API or Admin UI to create sync profiles
   - Configure resource types, schedules, and endpoints

## Configuration

### Global Properties

#### Server Configuration
```properties
# Facility Identification
ugandaemrsync.healthCenterSyncId=FACILITY-001
ugandaemrsync.serverIP=192.168.1.100
ugandaemrsync.protocol=https

# Analytics Server
ugandaemrsync.analytics.server.url=https://analytics.example.com
ugandaemrsync.analytics.server.username=admin
ugandaemrsync.analytics.server.password=secret
```

#### DHIS2 Configuration
```properties
ugandaemrsync.sendtoDHIS2.server.url=https://dhis2.example.com
ugandaemrsync.sendtoDHIS2.server.username=dhis2_user
ugandaemrsync.sendtoDHIS2.server.password=dhis2_password
```

#### Recency Testing
```properties
ugandaemrsync.recency.server.url=https://recency.example.com
ugandaemrsync.recency.server.password=recency_password
ugandaemrsync.recency.submit.data.once.daily=true
```

### FHIR Profile Configuration

#### Resource-based Profile Example
```json
{
  "name": "Patient Data Exchange",
  "resourceTypes": "Patient,Encounter,Observation",
  "profileEnabled": true,
  "generateBundle": true,
  "numberOfResourcesInBundle": 50,
  "url": "https://central-server.example.com/fhir",
  "urlUserName": "sync_user",
  "urlPassword": "sync_password"
}
```

#### Case-based Profile Example
```json
{
  "name": "HIV Case Surveillance",
  "isCaseBasedProfile": true,
  "caseBasedPrimaryResourceType": "Condition",
  "caseBasedPrimaryResourceTypeId": "hiv-condition-concept",
  "resourceTypes": "Patient,Condition,Observation,Encounter",
  "profileEnabled": true,
  "scheduleEnabled": true,
  "scheduleType": "FIXED_RATE",
  "fixedRateInterval": 3600000
}
```

## Usage

### Creating a FHIR Profile

#### Via REST API
```bash
curl -X POST \
  https://openmrs.example.com/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "DHIS2 Reporting",
    "resourceTypes": "Patient,Encounter",
    "profileEnabled": true,
    "url": "https://dhis2.example.com/api/fhir",
    "urlUserName": "dhis2_user",
    "urlPassword": "dhis2_password"
  }'
```

#### Via Database
```sql
INSERT INTO sync_fhir_profile (
  name, resource_types, profile_enabled, url, 
  url_username, url_password, generate_bundle
) VALUES (
  'DHIS2 Reporting', 'Patient,Encounter', true,
  'https://dhis2.example.com/api/fhir',
  'dhis2_user', 'dhis2_password', true
);
```

### Setting Up Scheduled Tasks

#### Generic Scheduler (Recommended)
```java
// Create one scheduler task in OpenMRS Scheduler UI
Task Name: Generic FHIR Profile Scheduler
Task Class: org.openmrs.module.ugandaemrsync.tasks.GenericFhirProfileSchedulerTask
Schedule: Run every 5 minutes
Description: Universal FHIR profile scheduler with anti-blocking protection
```

#### Profile-Specific Scheduler (Legacy)
```java
// Create individual tasks for each profile
Task Name: HIV Surveillance Task
Task Class: org.openmrs.module.ugandaemrsync.tasks.HIVCaseBasedSurveillanceTask
Schedule: 0 0 * * * (hourly)
Property: syncFhirProfileUUID = <profile-uuid>
```

### Monitoring Sync Operations

#### Check Profile Status
```sql
SELECT 
  name,
  profile_enabled,
  last_execution_status,
  last_execution_date,
  next_execution_date,
  CASE 
    WHEN next_execution_date <= NOW() THEN 'DUE NOW'
    WHEN last_execution_status = 'RUNNING' THEN 'RUNNING'
    ELSE 'WAITING'
  END as execution_state
FROM sync_fhir_profile
WHERE schedule_enabled = TRUE
ORDER BY execution_priority, next_execution_date;
```

#### View Execution History
```sql
SELECT 
  sp.name as profile_name,
  eph.execution_date,
  eph.execution_status,
  eph.resources_processed,
  eph.error_message
FROM sync_fhir_profile_execution_history eph
JOIN sync_fhir_profile sp ON eph.profile_id = sp.sync_fhir_profile_id
ORDER BY eph.execution_date DESC
LIMIT 50;
```

### Troubleshooting

#### Enable Debug Logging
```xml
<!-- Add to log4j.xml -->
<logger name="org.openmrs.module.ugandaemrsync">
  <level value="DEBUG"/>
</logger>
```

#### Check Circuit Breaker Status
```sql
SELECT * FROM sync_fhir_profile WHERE last_execution_status = 'FAILED';
```

#### View Sync Statistics
```bash
curl -u admin:Admin123 \
  https://openmrs.example.com/openmrs/ws/rest/v1/syncfhirresource/stats
```

## API Reference

### FHIR Profile Endpoints

#### Get All Profiles
```
GET /ws/rest/v1/syncfhirprofile
```

#### Get Specific Profile
```
GET /ws/rest/v1/syncfhirprofile/{uuid}
```

#### Create Profile
```
POST /ws/rest/v1/syncfhirprofile
Content-Type: application/json

{
  "name": "Profile Name",
  "resourceTypes": "Patient,Observation",
  "profileEnabled": true
}
```

#### Update Profile
```
POST /ws/rest/v1/syncfhirprofile/{uuid}
Content-Type: application/json

{
  "profileEnabled": false
}
```

### FHIR Resource Endpoints

#### Get Generated Resources
```
GET /ws/rest/v1/syncfhirresource
```

#### Get Resource Statistics
```
GET /ws/rest/v1/syncfhirresource/stats
```

#### Get Sync Logs
```
GET /ws/rest/v1/syncfhirprofilelog
```

### Lab Results Endpoints

#### Request Lab Results
```
POST /ws/rest/v1/requestlabresults
Content-Type: application/json

{
  "patientIdentifier": "PAT-001",
  "dateFrom": "2024-01-01",
  "dateTo": "2024-12-31"
}
```

#### Receive Lab Results
```
POST /ws/rest/v1/recievelabresult
Content-Type: application/json

{
  "results": "FHIR Bundle JSON",
  "sourceSystem": "CPHL"
}
```

### Task Management Endpoints

#### Get All Tasks
```
GET /ws/rest/v1/synctask
```

#### Get Task Details
```
GET /ws/rest/v1/synctaskdetails/{uuid}
```

#### Create Task
```
POST /ws/rest/v1/synctask
Content-Type: application/json

{
  "syncTaskType": "TASK_TYPE_UUID",
  "dateCreated": "2024-01-01",
  "status": "PENDING"
}
```

## Security

### Authentication & Authorization

#### REST API Authentication
All REST endpoints require authentication. Use one of:
- Basic Auth (username/password)
- Session Auth (after login)
- API Key (if configured)

#### Required Privileges

**View Privileges**:
- `UgandaemrSync: View Sync Tasks` - View sync task history
- `UgandaemrSync: View FHIR Resources` - View FHIR resources
- `UgandaemrSync: View FHIR Profiles` - View profile configurations
- `UgandaemrSync: View Lab Results` - View lab results from external systems

**Manage Privileges**:
- `UgandaemrSync: Manage Sync Tasks` - Create and manage tasks
- `UgandaemrSync: Manage FHIR Resources` - Modify FHIR resources
- `UgandaemrSync: Manage FHIR Profiles` - Create and modify profiles
- `UgandaemrSync: Manage Lab Results` - Import and manage lab results

### Security Best Practices

1. **Use HTTPS**: Always use HTTPS for external system communication
2. **Secure Credentials**: Store credentials in encrypted global properties
3. **Limit Access**: Grant minimum required privileges
4. **Audit Logs**: Regularly review sync logs for suspicious activity
5. **Network Security**: Use firewalls and network segmentation
6. **Certificate Validation**: Enable proper SSL certificate validation

## Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/METS-Programme/openmrs-module-ugandaemr-sync.git
cd openmrs-module-ugandaemr-sync

# Build all modules
mvn clean install

# Skip tests during build
mvn clean install -DskipTests

# Build specific module
cd api
mvn clean install

# Build with specific profile
mvn clean install -P openmrs-2.7
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UgandaEMRSyncServiceTest

# Run with coverage
mvn test jacoco:report
```

### Project Structure

```
openmrs-module-ugandaemr-sync/
├── api/                          # API module
│   ├── src/main/java/
│   │   └── org/openmrs/module/ugandaemrsync/
│   │       ├── api/             # Service interfaces and implementations
│   │       ├── model/           # Data models
│   │       ├── tasks/           # Scheduled tasks
│   │       ├── server/          # FHIR processing logic
│   │       ├── security/        # Security components
│   │       ├── circuitbreaker/  # Circuit breaker implementation
│   │       ├── exception/       # Custom exceptions
│   │       └── util/            # Utility classes
│   └── src/main/resources/
│       ├── scripts/             # SQL scripts
│       └── liquibase/           # Database migrations
├── omod/                         # OSGi module
│   └── src/main/java/
│       └── org/openmrs/module/ugandaemrsync/
│           ├── web/             # Web controllers
│           └── resource/        # REST resources
└── pom.xml                       # Maven POM
```

### Code Style

- Follow OpenMRS coding standards
- Use meaningful variable and method names
- Add JavaDoc for all public APIs
- Write unit tests for new functionality
- Keep methods small and focused

## Contributing

We welcome contributions from the community!

### Contribution Guidelines

1. **Fork the Repository**
2. **Create a Feature Branch** (`git checkout -b feature/amazing-feature`)
3. **Commit Your Changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the Branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Development Setup

1. Set up development environment with OpenMRS SDK
2. Configure database connection
3. Install required dependencies
4. Run module in development mode
5. Write tests for new features

### Code Review Process

- All pull requests require review
- Ensure tests pass
- Follow coding standards
- Update documentation as needed

## Support

### Documentation
- [GitHub Wiki](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/wiki)
- [API Documentation](https://raw.githubusercontent.com/wiki/METS-Programme/openmrs-module-ugandaemr-sync/API-Reference.md)
- [Implementation Guides](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/tree/master/docs)

### Community
- [GitHub Issues](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- [OpenMRS Talk](https://talk.openmrs.org/)
- [METS Programme Website](http://mets.or.ug)

### Professional Support
For enterprise support and custom integrations, contact the METS Programme.

## License

This project is licensed under the Mozilla Public License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **METS Programme**: Monitoring and Evaluation Technical Support (Uganda)
- **OpenMRS Community**: For the excellent EMR platform
- **HAPI FHIR**: For the FHIR Java implementation
- **Health Facilities**: Implementing facilities using this module

## Changelog

### Version 2.0.6-SNAPSHOT (Current)
- Generic FHIR profile scheduler with anti-blocking protection
- Enhanced security with role-based access control
- Circuit breaker pattern for fault tolerance
- Improved error handling and logging
- Connection pooling and performance optimizations
- FHIR R4 compliance
- REST API enhancements

### Version 2.0.5
- Initial release with FHIR R4 support
- Basic profile management
- DHIS2 integration
- Lab system integration

## Version Compatibility

| OpenMRS Version | Module Version | Status |
|-----------------|----------------|---------|
| 2.7.x           | 2.0.6-SNAPSHOT | ✅ Tested |
| 2.6.x           | 2.0.5          | ✅ Tested |
| 3.x             | 2.1.0-SNAPSHOT | 🚧 In Development |

---

**Maintained by**: [METS Programme](http://mets.or.ug)  
**Last Updated**: May 2, 2026  
**For questions or support**: [GitHub Issues](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)