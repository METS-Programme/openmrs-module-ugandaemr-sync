# UgandaEMR Sync Module - Developer Guide

Comprehensive guide for developers working on the UgandaEMR Sync Module.

## 📚 Table of Contents

- [Development Environment Setup](#development-environment-setup)
- [Architecture Overview](#architecture-overview)
- [Core Components Deep Dive](#core-components-deep-dive)
- [FHIR Processing Pipeline](#fhir-processing-pipeline)
- [Adding New Features](#adding-new-features)
- [Testing Strategy](#testing-strategy)
- [Performance Optimization](#performance-optimization)
- [Debugging Techniques](#debugging-techniques)
- [Code Style Guidelines](#code-style-guidelines)
- [Release Process](#release-process)

## 🔧 Development Environment Setup

### Prerequisites

- **Java Development Kit (JDK)**: 8 or higher
- **Maven**: 3.6+
- **Git**: For version control
- **IDE**: IntelliJ IDEA (recommended) or Eclipse
- **OpenMRS SDK**: For local development
- **Database**: MySQL 5.7+ or PostgreSQL 9.6+

### Step 1: Clone and Build

```bash
# Clone the repository
git clone https://github.com/METS-Programme/openmrs-module-ugandaemr-sync.git
cd openmrs-module-ugandaemr-sync

# Build the project
mvn clean install

# Skip tests (for faster builds during development)
mvn clean install -DskipTests

# Build specific module
cd api
mvn clean install
```

### Step 2: Set Up Development Environment

#### Using OpenMRS SDK (Recommended)

```bash
# Install OpenMRS SDK
curl -s https://raw.githubusercontent.com/openmrs/openmrs-sdk/master/bin/install | bash

# Create new OpenMRS project
sdk create-project openmrs.openmrs-reference-module-2.9.0 mydevserver

# Add sync module to project
cd mydevserver
echo "openmrs.module.ugandaemrsync.enabled=true" >> openmrs-run.properties
```

#### Manual Setup

```bash
# Download OpenMRS standalone
wget https://sourceforge.net/projects/openmrs/files/releases/OpenMRS_Platform/2.7.0/openmrs-2.7.0.war

# Deploy to servlet container (Tomcat)
cp openmrs-module-ugandaemr-sync-2.0.6-SNAPSHOT.oam $TOMCAT_HOME/webapps/openmrs/WEB-INF/bundles/

# Start Tomcat
cd $TOMCAT_HOME/bin
./startup.sh
```

### Step 3: Configure IDE

#### IntelliJ IDEA

1. **Import Project**: File → Open → Select `pom.xml`
2. **Enable Annotation Processing**: 
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Enable "Enable annotation processing"
3. **Configure Code Style**: Import Java code style from OpenMRS
4. **Set Up Database**: Configure database connection in Data Sources

#### Eclipse

1. **Import as Maven Project**: File → Import → Maven → Existing Maven Projects
2. **Enable Project Facets**: Right-click project → Properties → Project Facets
3. **Configure Server**: Add Tomcat server in Server view

## 🏗️ Architecture Overview

### Module Structure

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
│   │       │   └── HIVCaseBasedSurveillanceTask.java
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

### Data Flow Architecture

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

## 🔍 Core Components Deep Dive

### 1. SyncFhirProfile

**Purpose**: Configuration template for FHIR data exchange

**Key Features**:
- Resource type selection (Patient, Observation, etc.)
- Case-based vs resource-based profiles
- Scheduling configuration
- Endpoint configuration
- Authentication settings

**Database Schema**:
```sql
CREATE TABLE sync_fhir_profile (
  sync_fhir_profile_id INT PRIMARY KEY AUTO_INCREMENT,
  uuid VARCHAR(38) UNIQUE,
  name VARCHAR(255),
  resource_types VARCHAR(255),
  profile_enabled BOOLEAN,
  is_case_based_profile BOOLEAN,
  case_based_primary_resource_type VARCHAR(255),
  url VARCHAR(500),
  url_username VARCHAR(255),
  url_password VARCHAR(255),
  -- ... 20+ scheduling and configuration fields
);
```

**Usage Example**:
```java
// Create new profile
SyncFhirProfile profile = new SyncFhirProfile();
profile.setName("HIV Surveillance");
profile.setResourceTypes("Patient,Condition,Observation");
profile.setIsCaseBasedProfile(true);
profile.setProfileEnabled(true);

// Save profile
UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
SyncFhirProfile saved = service.saveSyncFhirProfile(profile);
```

### 2. SyncFHIRRecord

**Purpose**: Core FHIR processing engine

**Key Methods**:
```java
// Generate FHIR resources
public Collection<String> generateFHIRResourceBundles(SyncFhirProfile profile)

// Generate case-based resources
public Collection<SyncFhirResource> generateCaseBasedFHIRResourceBundles(SyncFhirProfile profile)

// Send resources to external system
public List<Map> sendFhirResourcesTo(SyncFhirProfile profile)

// Identify new cases for surveillance
public void identifyNewCases(SyncFhirProfile profile, Date currentDate)
```

**Processing Flow**:
```
1. Query OpenMRS data (via FhirQueryExecutor)
2. Transform to FHIR format (via FhirResourceTransformer)
3. Create FHIR bundles
4. Save to SyncFhirResource table
5. Send to external system (if configured)
6. Update sync status
```

### 3. FhirQueryExecutor

**Purpose**: Execute queries and extract data from OpenMRS

**Key Methods**:
```java
// Execute parameterized queries
public List<Map<String, Object>> executeQuery(String sql, Map<String, Object> params)

// Get patients by criteria
public List<Patient> getPatientsForProfile(SyncFhirProfile profile)

// Get encounters for patients
public List<Encounter> getEncountersForPatients(List<Patient> patients)
```

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

**Usage Example**:
```java
UgandaEMRHttpURLConnection conn = new UgandaEMRHttpURLConnection();

// Send POST request
Map<String, String> response = conn.postRequest(
    "https://external-system.com/fhir",
    fhirBundleJson,
    "application/json",
    username,
    password
);

// Check response
if (response.get("responseCode").equals("200")) {
    // Success
} else {
    // Handle error
}
```

### 5. CircuitBreaker

**Purpose**: Prevent cascading failures when external systems are down

**States**:
- **CLOSED**: Normal operation
- **OPEN**: Circuit tripped, stop calling
- **HALF_OPEN**: Testing if system recovered

**Configuration**:
```java
CircuitBreakerConfig config = new CircuitBreakerConfig()
    .withFailureThreshold(5)        // Trip after 5 failures
    .withTimeoutDuration(60000)     // Try again after 60 seconds
    .withSuccessThreshold(2);       // Need 2 successes to close

CircuitBreaker breaker = new CircuitBreaker("ExternalSystem", config);
```

## 🔄 FHIR Processing Pipeline

### Resource-based Profile Flow

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

### Case-based Profile Flow

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

## 🚀 Adding New Features

### Adding a New Scheduled Task

```java
package org.openmrs.module.ugandaemrsync.tasks;

import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;

public class MyCustomSyncTask extends AbstractTask {
    
    @Override
    public void execute() {
        try {
            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);
            
            // Your custom logic here
            log.info("Executing custom sync task");
            
            // Example: Process specific data
            processCustomData(service);
            
        } catch (Exception e) {
            log.error("Error in custom sync task", e);
            throw new RuntimeException("Custom sync task failed", e);
        }
    }
    
    private void processCustomData(UgandaEMRSyncService service) {
        // Implementation here
    }
}
```

### Adding a New REST Endpoint

```java
package org.openmrs.module.ugandaemrsync.web.resource;

import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.annotation.Resource;

@Resource(name = RestConstants.VERSION_1 + "/mycustomresource", 
         supportedClass = MyCustomModel.class)
public class MyCustomResource extends DelegatingCrudResource<MyCustomModel> {
    
    @Override
    public MyCustomModel newDelegate() {
        return new MyCustomModel();
    }
    
    @Override
    public MyCustomModel save(MyCustomModel delegate) {
        return Context.getService(UgandaEMRSyncService.class)
                     .saveMyCustomModel(delegate);
    }
    
    @Override
    public MyCustomModel getByUniqueId(String uniqueId) {
        return Context.getService(UgandaEMRSyncService.class)
                     .getMyCustomModelByUuid(uniqueId);
    }
    
    // Implement other required methods...
}
```

### Adding Database Migrations

```xml
<!-- liquibase/changelog/my-custom-changes.xml -->
<changeSet id="add-custom-table" author="developer">
    <createTable tableName="my_custom_table">
        <column name="id" type="INT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="uuid" type="VARCHAR(38)">
            <constraints unique="true"/>
        </column>
        <column name="name" type="VARCHAR(255)"/>
        <column name="description" type="TEXT"/>
        <column name="date_created" type="DATETIME"/>
    </createTable>
</changeSet>
```

## 🧪 Testing Strategy

### Unit Testing

```java
public class UgandaEMRSyncServiceTest extends BaseModuleContextSensitiveTest {
    
    @Autowired
    private UgandaEMRSyncService service;
    
    @Test
    public void testSaveSyncFhirProfile() {
        // Create test profile
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Profile");
        profile.setProfileEnabled(true);
        
        // Save
        SyncFhirProfile saved = service.saveSyncFhirProfile(profile);
        
        // Verify
        assertNotNull(saved.getSyncFhirProfileId());
        assertEquals("Test Profile", saved.getName());
    }
    
    @Test
    public void testGetSyncFhirProfileByUuid() {
        // Setup test data
        SyncFhirProfile profile = createTestProfile();
        
        // Test
        SyncFhirProfile found = service.getSyncFhirProfileByUUID(profile.getUuid());
        
        // Verify
        assertNotNull(found);
        assertEquals(profile.getName(), found.getName());
    }
}
```

### Integration Testing

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/applicationContext-service.xml"})
public class FhirIntegrationTest {
    
    @Autowired
    private UgandaEMRSyncService service;
    
    @Test
    public void testEndToEndFhirGeneration() {
        // Create profile
        SyncFhirProfile profile = createTestProfile();
        
        // Generate FHIR resources
        SyncFHIRRecord processor = new SyncFHIRRecord();
        Collection<String> bundles = processor.generateFHIRResourceBundles(profile);
        
        // Verify FHIR output
        assertFalse(bundles.isEmpty());
        
        // Validate FHIR format
        for (String bundle : bundles) {
            IBaseResource resource = FhirContext.forR4().newJsonParser().parseResource(bundle);
            assertTrue(resource instanceof Bundle);
        }
    }
}
```

### Performance Testing

```java
public class PerformanceTest {
    
    @Test
    public void testLargeDatasetProcessing() {
        int datasetSize = 10000;
        
        // Create test data
        createTestData(datasetSize);
        
        // Measure processing time
        long startTime = System.currentTimeMillis();
        
        SyncFHIRRecord processor = new SyncFHIRRecord();
        processor.generateFHIRResourceBundles(createTestProfile());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify performance (should process < 5 minutes)
        assertTrue("Processing took too long: " + duration + "ms", 
                  duration < 300000);
    }
}
```

## ⚡ Performance Optimization

### Database Query Optimization

```java
// BAD: N+1 query problem
public List<Patient> getPatientsBad() {
    List<Integer> patientIds = getPatientIds();
    List<Patient> patients = new ArrayList<>();
    for (Integer id : patientIds) {
        patients.add(getPatientById(id));  // N queries
    }
    return patients;
}

// GOOD: Single query with JOIN
public List<Patient> getPatientsGood() {
    String sql = "SELECT p.* FROM patient p " +
                 "JOIN encounter e ON p.patient_id = e.patient_id " +
                 "WHERE e.voided = 0 " +
                 "GROUP BY p.patient_id";
    return executeQuery(sql);
}
```

### Memory Optimization

```java
// BAD: Load all data into memory
public List<Bundle> generateBundlesBad() {
    List<Patient> allPatients = getAllPatients();  // Could be 100k+
    List<Bundle> bundles = new ArrayList<>();
    
    for (Patient patient : allPatients) {
        bundles.add(createBundle(patient));
    }
    return bundles;  // Huge memory usage
}

// GOOD: Process in batches
public void generateBundlesGood() {
    int batchSize = 100;
    int offset = 0;
    
    while (true) {
        List<Patient> batch = getPatientBatch(offset, batchSize);
        if (batch.isEmpty()) break;
        
        for (Patient patient : batch) {
            Bundle bundle = createBundle(patient);
            saveAndSendBundle(bundle);  // Process immediately
        }
        
        offset += batchSize;
    }
}
```

### Connection Pool Optimization

```java
// Configure connection pool
PoolingHttpClientConnectionManager connectionManager =
    new PoolingHttpClientConnectionManager();
connectionManager.setMaxTotal(50);        // Max connections
connectionManager.setDefaultMaxPerRoute(10); // Per route

CloseableHttpClient client = HttpClients.custom()
    .setConnectionManager(connectionManager)
    .build();
```

## 🐛 Debugging Techniques

### Enable Debug Logging

```xml
<!-- log4j.xml -->
<logger name="org.openmrs.module.ugandaemrsync">
  <level value="DEBUG"/>
</logger>

<logger name="org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord">
  <level value="TRACE"/>
</logger>
```

### Remote Debugging

```bash
# Start OpenMRS with remote debugging enabled
export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"
catalina.sh run

# Connect from IDE
# IntelliJ: Run → Edit Configurations → Remote → Debug
```

### SQL Query Debugging

```java
// Enable Hibernate SQL logging
log.info("Executing query: {}", sql);

// Log query parameters
log.info("Query parameters: {}", params);

// Log result count
log.info("Query returned {} results", results.size());
```

### FHIR Validation

```java
// Validate FHIR resources
FhirContext ctx = FhirContext.forR4();
FhirValidator validator = ctx.newValidator();

try {
    ValidationResult result = validator.validateWithResult(resource);
    if (!result.isSuccessful()) {
        for (SingleValidationMessage message : result.getMessages()) {
            log.error("FHIR validation error: {}", message.getMessage());
        }
    }
} catch (Exception e) {
    log.error("FHIR validation failed", e);
}
```

## 📝 Code Style Guidelines

### Java Code Style

1. **Naming Conventions**
   - Classes: PascalCase (`SyncFhirProfile`)
   - Methods: camelCase (`generateFHIRResources`)
   - Constants: UPPER_SNAKE_CASE (`CONNECTION_SUCCESS_200`)
   - Variables: camelCase (`patientList`)

2. **Method Organization**
   ```java
   public class Example {
       // 1. Public methods
       public void publicMethod() {}
       
       // 2. Protected methods
       protected void protectedMethod() {}
       
       // 3. Private methods
       private void privateMethod() {}
       
       // 4. Inner classes
       private class InnerClass {}
   }
   ```

3. **Exception Handling**
   ```java
   // GOOD: Specific exceptions
   try {
       processFhirData();
   } catch (FHIRValidationException e) {
       log.error("FHIR validation failed", e);
       throw new UgandaEMRSyncException("Invalid FHIR data", e);
   }
   
   // BAD: Generic exceptions
   try {
       processFhirData();
   } catch (Exception e) {
       // Don't do this
   }
   ```

4. **Documentation**
   ```java
   /**
    * Generate FHIR resources for a specific profile.
    * 
    * This method queries OpenMRS data based on the profile configuration,
    * transforms it to FHIR format, and creates bundles for synchronization.
    * 
    * @param profile the FHIR profile configuration
    * @return collection of FHIR resource bundles
    * @throws UgandaEMRSyncException if FHIR generation fails
    * @see SyncFhirProfile
    */
   public Collection<String> generateFHIRResources(SyncFhirProfile profile) {
       // Implementation
   }
   ```

### SQL Code Style

1. **Use Parameterized Queries**
   ```sql
   -- GOOD
   SELECT * FROM patient WHERE patient_id = :patientId
   
   -- BAD
   SELECT * FROM patient WHERE patient_id = 123
   ```

2. **Naming Conventions**
   ```sql
   -- Tables: snake_case
   sync_fhir_profile, sync_fhir_resource
   
   -- Columns: snake_case
   profile_enabled, case_based_primary_resource_type
   
   -- Indexes: idx_tablename_columnname
   INDEX idx_sync_fhir_profile_uuid (uuid)
   ```

## 🚢 Release Process

### Versioning

- Use semantic versioning: `MAJOR.MINOR.PATCH`
- Example: `2.0.6-SNAPSHOT` → `2.0.6` → `2.0.7-SNAPSHOT`

### Release Checklist

1. **Update Version Numbers**
   ```xml
   <!-- pom.xml -->
   <version>2.0.6</version> <!-- Remove -SNAPSHOT -->
   ```

2. **Update Documentation**
   - Update README.md with new features
   - Update CHANGELOG.md
   - Update API documentation

3. **Run Full Test Suite**
   ```bash
   mvn clean test
   ```

4. **Create Release Tag**
   ```bash
   git tag -a v2.0.6 -m "Release version 2.0.6"
   git push origin v2.0.6
   ```

5. **Build Release Artifacts**
   ```bash
   mvn clean deploy
   ```

6. **Publish to Module Repository**
   - Upload to OpenMRS modules repository
   - Create GitHub release
   - Announce on community forums

## 📚 Additional Resources

### OpenMRS Documentation
- [OpenMRS Developer Guide](https://wiki.openmrs.org/display/docs/Developer+Guide)
- [Module Development](https://wiki.openmrs.org/display/docs/Module+Development+Guide)
- [FHIR2 Module](https://wiki.openmrs.org/display/docs/FHIR2+Module)

### FHIR Resources
- [HL7 FHIR Specification](https://hl7.org/fhir/)
- [HAPI FHIR Java Implementation](https://hapifhir.io/)
- [FHIR Validation](https://hl7.org/fhir/validation.html)

### Community
- [OpenMRS Talk](https://talk.openmrs.org/)
- [GitHub Discussions](https://github.com/openmrs/openmrs-module-fhir2/discussions)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/openmrs)

---

**Last Updated**: May 2, 2026  
**Maintained By**: METS Programme Development Team