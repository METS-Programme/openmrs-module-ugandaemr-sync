# UgandaEMR Sync Module

[![License](https://img.shields.io/badge/license-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![OpenMRS](https://img.shields.io/badge/OpenMRS-2.7%2B-blue.svg)](https://openmrs.org)
[![Version](https://img.shields.io/badge/version-2.0.6--SNAPSHOT-orange.svg)](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync)

## Overview

The UgandaEMR Sync Module provides comprehensive data sharing and health information exchange (HIE) capabilities for OpenMRS installations. It enables seamless interoperability between facility-level EMR systems and external health information systems using FHIR (Fast Healthcare Interoperability Resources) standards.

### Key Capabilities

- **FHIR-based Data Exchange**: HL7 FHIR R4 standard for healthcare data interoperability
- **Multi-system Integration**: Connect with DHIS2, central servers, lab systems, and other HIEs
- **National DWH**: Advanced disease surveillance and reporting capabilities
- **Resource-based Sync**: Flexible FHIR resource synchronization profiles
- **Bi-directional Communication**: Send data to and receive data from external systems
- **Secure & Reliable**: Enterprise-grade security with circuit breaker patterns and retry mechanisms

## 📚 Documentation

**Complete documentation is available in the [docs/](docs/) folder.**

### Essential Reading
- **[Getting Started](docs/Getting-Started.md)** - 15-minute installation guide ⭐
- **[Home](docs/Home.md)** - Complete module overview and features  
- **[API Reference](docs/API_REFERENCE.md)** - REST API documentation

### Configuration & Usage
- **[Configuration](docs/Configuration.md)** - Configuration options and settings
- **[FHIR Profiles](docs/FHIR-Profiles.md)** - FHIR profile configuration guide
- **[Troubleshooting](docs/Troubleshooting.md)** - Common issues and solutions

### Development
- **[Development](docs/Development.md)** - Developer guide and contribution
- **[Security](docs/Security.md)** - Security considerations and best practices
- **[CHANGELOG](docs/CHANGELOG.md)** - Version history and changes

### Quick Links

| What You Need | Where to Look |
|---------------|---------------|
| "I'm new here" | [Home](docs/Home.md) |
| "Get me started fast" | [Getting Started](docs/Getting-Started.md) |
| "API documentation" | [API Reference](docs/API_REFERENCE.md) |
| "I want to contribute" | [Development](docs/Development.md) |
| "What's new?" | [CHANGELOG](docs/CHANGELOG.md) |
| "Something's broken" | [Troubleshooting](docs/Troubleshooting.md) |

## Quick Start

```bash
# 1. Download the latest OAM file
wget https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/releases/download/v2.0.5/openmrs-module-ugandaemr-sync-2.0.5.oam

# 2. Upload via OpenMRS Admin UI
# Navigate to: Administration → Manage Modules → Add Module
# Select the downloaded OAM file and click "Upload"

# 3. Configure global properties
# Navigate to: Administration → Advanced Settings → Global Properties
ugandaemrsync.healthCenterSyncId = YOUR_FACILITY_ID
ugandaemrsync.protocol = https

# 4. Create your first FHIR profile
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Patient Data Sync",
    "description": "Sync patient demographics and encounters",
    "resourceTypes": "Patient,Encounter,Observation",
    "profileEnabled": true,
    "url": "https://central-server.health.gov/fhir",
    "urlUserName": "facility-id",
    "urlPassword": "api-key"
  }'
```

**For detailed installation instructions**, see the [Getting Started guide](docs/Getting-Started.md).

## Features

### 🔥 Key Features

- **FHIR Profile Management**: Resource-based and case-based profiles with flexible scheduling
- **Data Exchange Capabilities**: Push/pull data to central servers, DHIS2, lab systems
- **Advanced Scheduling**: Anti-blocking protection with priority-based execution
- **Security & Compliance**: Role-based access control, audit logging, HIPAA-compliant
- **Developer-Friendly**: REST APIs, extensive documentation, community support

**For complete feature list**, see the [module overview](docs/Home.md).

## Installation

### Prerequisites
- OpenMRS Platform 2.7.0 or higher
- Administrator access to OpenMRS
- MySQL 5.7+ or PostgreSQL 9.6+
- Java 8 or higher

### Quick Install
1. Download the latest OAM file from [releases](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/releases)
2. Upload via OpenMRS Admin UI (Administration → Manage Modules → Add Module)
3. Configure global properties
4. Set up scheduled tasks

**For detailed installation instructions**, see the [Getting Started guide](docs/Getting-Started.md).

## Usage Examples

### Sync Patient Data to Central Server

```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "Central Server Sync",
    "resourceTypes": "Patient,Encounter,Observation",
    "profileEnabled": true,
    "url": "https://central-server.health.gov/fhir",
    "urlUserName": "your-facility-id",
    "urlPassword": "your-api-key",
    "syncLimit": 1000
  }'
```

### HIV Case Surveillance

```bash
curl -X POST \
  https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile \
  -H 'Content-Type: application/json' \
  -u admin:Admin123 \
  -d '{
    "name": "HIV Case Surveillance",
    "isCaseBasedProfile": true,
    "caseBasedPrimaryResourceType": "Condition",
    "resourceTypes": "Patient,Condition,Observation,Encounter",
    "profileEnabled": true,
    "scheduleEnabled": true,
    "fixedRateInterval": 3600000,
    "executionPriority": 1
  }'
```

**For more usage examples**, see the [Getting Started guide](docs/Getting-Started.md).

## Support

### Documentation
- **[docs/](docs/)** - Complete documentation
- **[API Reference](docs/API_REFERENCE.md)** - REST API documentation
- **[Troubleshooting](docs/Troubleshooting.md)** - Common issues and solutions

### Community
- **GitHub Issues**: [Report bugs or request features](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- **OpenMRS Talk**: [Community discussions](https://talk.openmrs.org/)
- **METS Programme**: [Professional support](http://mets.or.ug)

## Contributing

We welcome contributions! Please see the [Development Guide](docs/Development.md) for details on how to contribute to this project.

## License

This project is licensed under the Mozilla Public License 2.0 - see the [LICENSE](LICENSE) file for details.

## Version Information

- **Current Version**: 2.0.6-SNAPSHOT
- **Minimum OpenMRS Version**: 2.7.0
- **FHIR Version**: R4
- **Maintainer**: METS Programme

---

**For complete documentation**, visit the [docs/](docs/) folder.
