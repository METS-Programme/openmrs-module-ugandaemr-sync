# Development Guide

Comprehensive guide for developers working on the UgandaEMR Sync Module.

## Table of Contents

- [Development Environment Setup](#development-environment-setup)
- [Building the Module](#building-the-module)
- [Code Organization](#code-organization)
- [Adding New Features](#adding-new-features)
- [Testing](#testing)
- [Debugging](#debugging)
- [Code Style Guidelines](#code-style-guidelines)
- [Contributing](#contributing)

## Development Environment Setup

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

## Building the Module

### Full Build

```bash
# Clean and build with tests
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build specific module
cd api
mvn clean install
```

### Build Options

```bash
# Build with specific profile
mvn clean install -Pdevelopment

# Build with debug information
mvn clean install -Dmaven.compiler.debug=true

# Build with custom OpenMRS version
mvn clean install -Dopenmrs.version=2.7.0
```

## Code Organization

### Module Structure

```
openmrs-module-ugandaemr-sync/
├── api/                          # API Module (business logic)
│   ├── src/main/java/
│   │   └── org/openmrs/module/ugandaemrsync/
│   │       ├── api/             # Service layer
│   │       ├── model/           # Domain models
│   │       ├── dao/             # Data access layer
│   │       ├── tasks/           # Scheduled tasks
│   │       ├── server/          # FHIR processing
│   │       ├── security/        # Security components
│   │       ├── circuitbreaker/  # Circuit breaker pattern
│   │       ├── exception/       # Custom exceptions
│   │       └── util/            # Utilities
│   └── src/main/resources/
│       ├── liquibase/           # Database migrations
│       └── scripts/             # SQL scripts
├── omod/                         # OSGi Module (web/rest)
│   ├── src/main/java/
│   │   └── org/openmrs/module/ugandaemrsync/
│   │       ├── web/             # Web controllers
│   │       └── resource/        # REST resources
│   └── src/main/resources/
│       ├── config.xml           # Module configuration
│       └── messages.properties  # UI messages
└── pom.xml                       # Parent POM
```

### Key Packages

#### api/
Contains business logic and service interfaces

#### omod/
Contains REST resources and web controllers

#### model/
Contains domain models and database entities

#### dao/
Contains data access objects and database queries

#### tasks/
Contains scheduled task implementations

#### server/
Contains FHIR processing logic

#### security/
Contains security and authentication components

## Adding New Features

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

## Testing

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

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UgandaEMRSyncServiceTest

# Run specific test method
mvn test -Dtest=UgandaEMRSyncServiceTest#testSaveSyncFhirProfile

# Run integration tests
mvn verify
```

## Debugging

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

## Code Style Guidelines

### Java Code Style

1. **Naming Conventions**
   - Classes: PascalCase (`SyncFhirProfile`)
   - Methods: camelCase (`generateFHIRResources`)
   - Constants: UPPER_SNAKE_CASE (`CONNECTION_SUCCESS_200`)
   - Variables: camelCase (`patientList`)

2. **Exception Handling**
   ```java
   // GOOD: Specific exceptions
   try {
       processFhirData();
   } catch (FHIRValidationException e) {
       log.error("FHIR validation failed", e);
       throw new UgandaEMRSyncException("Invalid FHIR data", e);
   }
   ```

3. **Documentation**
   ```java
   /**
    * Generate FHIR resources for a specific profile.
    * 
    * @param profile the FHIR profile configuration
    * @return collection of FHIR resource bundles
    * @throws UgandaEMRSyncException if FHIR generation fails
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
   ```

## Contributing

### Contribution Process

1. **Fork the repository**
   - Create a fork on GitHub
   - Clone your fork locally

2. **Create a feature branch**
   ```bash
   git checkout -b feature/my-feature
   ```

3. **Make changes**
   - Write code following style guidelines
   - Add tests for new functionality
   - Update documentation

4. **Test changes**
   ```bash
   mvn clean test
   ```

5. **Commit changes**
   ```bash
   git add .
   git commit -m "Add feature: description of changes"
   ```

6. **Push to fork**
   ```bash
   git push origin feature/my-feature
   ```

7. **Create Pull Request**
   - Go to GitHub
   - Create pull request
   - Provide clear description of changes

### Pull Request Guidelines

- **Title**: Clear, concise description
- **Description**: Detailed explanation of changes
- **Testing**: Describe testing performed
- **Documentation**: Update relevant docs
- **Breaking Changes**: Clearly indicate if present

### Code Review Process

1. Automated checks (CI/CD)
2. Peer review
3. Address feedback
4. Approval and merge

## Release Process

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

6. **Publish Release**
   - Upload to OpenMRS modules repository
   - Create GitHub release
   - Announce on community forums

## Additional Resources

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