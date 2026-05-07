# Architecture

The UgandaEMR Sync Module is built on a modular architecture that follows OpenMRS module development best practices and healthcare interoperability standards.

## System Architecture

### High-Level Architecture

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

## Module Structure

### Project Organization

```
openmrs-module-ugandaemr-sync/
├── api/                          # API Module (business logic)
│   ├── src/main/java/
│   │   └── org/openmrs/module/ugandaemrsync/
│   │       ├── api/             # Service layer
│   │       │   ├── UgandaEMRSyncService.java
│   │       │   └── impl/
│   │       │       └── UgandaEMRSyncServiceImpl.java
│   │       ├── model/           # Domain models
│   │       │   ├── SyncFhirProfile.java
│   │       │   ├── SyncFhirResource.java
│   │       │   ├── SyncFhirCase.java
│   │       │   └── SyncTask.java
│   │       ├── dao/             # Data access layer
│   │       │   └── UgandaEMRSyncDao.java
│   │       ├── tasks/           # Scheduled tasks
│   │       │   ├── GenericFhirProfileSchedulerTask.java
│   │       │   └── NationalDataWareHouseTask.java
│   │       ├── server/          # FHIR processing
│   │       │   ├── SyncFHIRRecord.java
│   │       │   ├── FhirQueryExecutor.java
│   │       │   └── FhirResourceTransformer.java
│   │       ├── security/        # Security components
│   │       │   ├── SSLConfiguration.java
│   │       │   └── ResourceSecurityInterceptor.java
│   │       ├── circuitbreaker/  # Circuit breaker pattern
│   │       │   ├── CircuitBreaker.java
│   │       │   └── CircuitBreakerRegistry.java
│   │       ├── exception/       # Custom exceptions
│   │       │   └── UgandaEMRSyncException.java
│   │       └── util/            # Utilities
│   │           └── UgandaEMRSyncUtil.java
│   └── src/main/resources/
│       ├── liquibase/           # Database migrations
│       │   └── liquibase.xml
│       └── scripts/             # SQL scripts
├── omod/                         # OSGi Module (web/rest)
│   ├── src/main/java/
│   │   └── org/openmrs/module/ugandaemrsync/
│   │       ├── UgandaEMRSyncActivator.java
│   │       ├── web/             # Web controllers
│   │       └── resource/        # REST resources
│   │           ├── SyncFhirProfileResource.java
│   │           └── RequestLabResultsResource.java
│   └── src/main/resources/
│       ├── config.xml           # Module configuration
│       └── messages.properties  # UI messages
└── pom.xml                       # Parent POM
```

## Data Flow Architecture

### Data Flow Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     External Systems                        │
│  (DHIS2, Central Servers, Lab Systems, etc.)                │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/HTTPS (FHIR JSON)
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                  UgandaEMR Sync Module                      │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              REST API Layer                           │  │
│  │  SyncFhirProfileResource, SyncFhirResourceResource   │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       ↓                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Service Layer                               │  │
│  │  UgandaEMRSyncService → UgandaEMRSyncServiceImpl     │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       ↓                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Business Logic Layer                          │  │
│  │  SyncFHIRRecord, FhirQueryExecutor,                  │  │
│  │  FhirResourceTransformer, Scheduled Tasks            │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       ↓                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Data Access Layer                           │  │
│  │  UgandaEMRSyncDao → Hibernate → Database             │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       ↓                                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Cross-cutting Concerns                        │  │
│  │  Security, Logging, Circuit Breaker, Validation      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
                  ┌─────────────────┐
                  │   OpenMRS API   │
                  │  (Patient, etc.) │
                  └─────────────────┘
```

### Resource-based Sync Flow

```
1. Scheduled Task Execution
   ↓
2. Load SyncFhirProfile
   ↓
3. Query OpenMRS Data
   - SELECT patients, encounters, observations
   - Apply profile filters
   ↓
4. Transform to FHIR
   - Patient → FHIR Patient Resource
   - Encounter → FHIR Encounter Resource
   - Observation → FHIR Observation Resource
   ↓
5. Create FHIR Bundle
   - Bundle.type = "collection"
   - Add resources to bundle.entry
   ↓
6. Save to Database
   - SyncFhirResource table
   - Status = "NOT_SYNCED"
   ↓
7. Send to External System
   - POST bundle to endpoint
   - Handle response
   - Update status
   ↓
8. Log Results
   - SyncFhirProfileLog entry
   - Execution history
```

### Case-based Sync Flow

```
1. Profile Execution
   ↓
2. Identify New Cases
   - Query primary resource type (e.g., Condition)
   - Filter by case criteria
   - Check for existing cases
   ↓
3. Create SyncFhirCase Records
   - Link primary resource
   - Set case status
   ↓
4. Generate Case Resources
   - Get all related resources for case
   - Transform to FHIR
   - Create case bundle
   ↓
5. Save and Send
   - Save to SyncFhirResource
   - Send to external system
   - Update case status
   ↓
6. Archive Old Cases
   - Move to history
   - Clean up resources
```

## Core Components

### 1. SyncFhirProfile
**Purpose**: Configuration template for FHIR data exchange

**Key Features**:
- Resource type selection (Patient, Observation, etc.)
- Case-based vs resource-based profiles
- Scheduling configuration
- Endpoint configuration
- Authentication settings

### 2. SyncFHIRRecord
**Purpose**: Core FHIR processing engine

**Key Methods**:
- `generateFHIRResourceBundles()`: Generate FHIR resources
- `generateCaseBasedFHIRResourceBundles()`: Generate case-based resources
- `sendFhirResourcesTo()`: Send resources to external system
- `identifyNewCases()`: Identify new cases for surveillance

### 3. FhirQueryExecutor
**Purpose**: Execute queries and extract data from OpenMRS

**Safety Features**:
- Parameterized queries (prevents SQL injection)
- Connection pooling
- Timeout handling
- Result size limiting

### 4. UgandaEMRHttpURLConnection
**Purpose**: Handle HTTP communication with external systems

**Key Features**:
- Connection pooling
- SSL/TLS configuration
- Circuit breaker integration
- Retry logic
- Timeout handling

### 5. CircuitBreaker
**Purpose**: Prevent cascading failures when external systems are down

**States**:
- **CLOSED**: Normal operation
- **OPEN**: Circuit tripped, stop calling
- **HALF_OPEN**: Testing if system recovered

## Design Patterns

### Circuit Breaker Pattern
Prevents system overload when external services are unavailable:
- Trips after configured failure threshold
- Automatically retries after timeout
- Provides fallback behavior

### Factory Pattern
Used for creating different types of FHIR resources and transformers

### Strategy Pattern
Different synchronization strategies for various profile types

### Observer Pattern
Event-driven architecture for monitoring sync operations

## Technology Stack

### Core Technologies
- **Java**: 8+
- **OpenMRS Platform**: 2.7.0+
- **Hibernate**: ORM and database abstraction
- **Spring**: Dependency injection and transaction management

### FHIR Technologies
- **HAPI FHIR**: FHIR Java implementation
- **FHIR R4**: HL7 FHIR R4 standard
- **FHIR Validation**: Resource validation

### Web Technologies
- **REST API**: RESTful web services
- **JSON**: Data serialization
- **HTTP/HTTPS**: Transport protocol

### Database
- **MySQL**: 5.7+ (primary)
- **PostgreSQL**: 9.6+ (supported)
- **Liquibase**: Database migrations

### Security
- **Spring Security**: Authentication and authorization
- **SSL/TLS**: Secure communications
- **RBAC**: Role-based access control

## Security Architecture

### Authentication & Authorization
- Role-based access control (RBAC)
- Method-level security with `@Secured` annotations
- Resource-level security interceptors
- Privilege-based operations

### Data Security
- HTTPS/TLS for all external communications
- Encrypted credential storage
- Audit logging for all operations
- Input validation and sanitization

### API Security
- Basic authentication for REST API
- Privilege verification
- Rate limiting capabilities
- Circuit breaker for fault tolerance

## Performance Considerations

### Scalability
- Connection pooling for HTTP and database
- Batch processing of resources
- Configurable memory limits
- Scheduled task optimization

### Reliability
- Circuit breaker pattern
- Retry mechanisms
- Transaction management
- Error recovery

### Monitoring
- Execution history tracking
- Performance metrics
- Error logging and alerting
- Status monitoring endpoints

---

**Last Updated**: May 2, 2026  
**Version**: 2.0.6-SNAPSHOT