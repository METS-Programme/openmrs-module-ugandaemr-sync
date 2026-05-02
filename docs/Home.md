# UgandaEMR Sync Module

[![License](https://img.shields.io/badge/license-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![OpenMRS](https://img.shields.io/badge/OpenMRS-2.7%2B-blue.svg)](https://openmrs.org)
[![Version](https://img.shields.io/badge/version-2.0.6--SNAPSHOT-orange.svg)](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync)

## Overview

The **UgandaEMR Sync Module** provides comprehensive data sharing and health information exchange (HIE) capabilities for OpenMRS installations. It enables seamless interoperability between facility-level EMR systems and external health information systems using FHIR (Fast Healthcare Interoperability Resources) standards.

## Key Capabilities

- **FHIR-based Data Exchange**: HL7 FHIR R4 standard for healthcare data interoperability
- **Multi-system Integration**: Connect with DHIS2, central servers, lab systems, and other HIEs
- **Case-based Surveillance**: Advanced disease surveillance and reporting capabilities
- **Resource-based Sync**: Flexible FHIR resource synchronization profiles
- **Bi-directional Communication**: Send data to and receive data from external systems
- **Secure & Reliable**: Enterprise-grade security with circuit breaker patterns and retry mechanisms

## Quick Links

| Documentation | Description |
|---------------|-------------|
| [Getting Started](Getting-Started) | Installation and basic setup (15 minutes) |
| [Architecture](Architecture) | System architecture and data flow |
| [Configuration](Configuration) | Configuration options and settings |
| [API Reference](API-Reference) | REST API documentation |
| [FHIR Profiles](FHIR-Profiles) | FHIR profile configuration guide |
| [Development](Development) | Developer guide and contribution |
| [Troubleshooting](Troubleshooting) | Common issues and solutions |
| [Security](Security) | Security considerations and best practices |

## Main Features

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

## Version Information

- **Current Version**: 2.0.6-SNAPSHOT
- **Minimum OpenMRS Version**: 2.7.0
- **License**: MPL 2.0
- **Maintainer**: METS Programme

## Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- **OpenMRS Talk**: [Community discussions](https://talk.openmrs.org/)
- **Documentation**: [Full documentation index](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/blob/master/DOCUMENTATION_INDEX.md)

## Contributing

We welcome contributions! Please see our [Development Guide](Development) for details on how to contribute to this project.

## License

This project is licensed under the Mozilla Public License 2.0 - see the [LICENSE](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/blob/master/LICENSE) file for details.

---

**Last Updated**: May 2, 2026  
**Documentation Version**: 2.0.6-SNAPSHOT