# Changelog

All notable changes to the UgandaEMR Sync Module will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- GraphQL API support
- Real-time sync with WebSocket
- Enhanced batch processing
- Multi-threaded resource generation
- FHIR Subscription support
- Improved error recovery mechanisms

## [2.0.6-SNAPSHOT] - 2026-05-02

### Added - Major Enhancements
- **Generic FHIR Profile Scheduler**: Universal scheduler with anti-blocking protection
  - Single task handles all profiles
  - Priority-based execution
  - Support for multiple schedule types (FIXED_RATE, FIXED_DELAY, CRON, MANUAL)
  - Execution history tracking
- **Enhanced Security Framework**:
  - Role-based access control with fine-grained privileges
  - `@Secured` annotations for method-level security
  - Resource security interceptor
- **Circuit Breaker Pattern**: Fault tolerance for external system failures
- **Connection Pooling**: Optimized HTTP connection management
- **SSL Configuration**: Enhanced TLS/SSL support with certificate validation

### Changed - Improvements
- **Error Handling**: Comprehensive exception handling throughout the module
- **Logging**: Structured logging with contextual information
- **Performance**: Optimized database queries and resource generation
- **API Enhancements**: 
  - New endpoints for execution history
  - Statistics and monitoring endpoints
  - Enhanced filtering and pagination

### Fixed - Bug Fixes
- SQL injection vulnerabilities (replaced with parameterized queries)
- Memory leaks in resource processing
- Thread safety issues in concurrent execution
- Connection resource leaks
- Hardcoded UUID dependencies

### Security
- Fixed critical SQL injection vulnerabilities in DAO layer
- Enhanced SSL certificate validation
- Improved password handling in configurations
- Added input validation framework

### Documentation
- **Comprehensive README**: Complete module overview and usage guide
- **Quick Start Guide**: 15-minute setup guide
- **Developer Guide**: Architecture, development setup, contribution guidelines
- **API Reference**: Complete REST API documentation
- **Changelog**: Version history and changes

## [2.0.5] - 2024-12-15

### Added
- FHIR R4 compliance (upgraded from FHIR DSTU2)
- Case-based surveillance profiles
- Execution history tracking
- Profile scheduling capabilities
- Lab results integration with CPHL
- Referral management system
- DHIS2 integration improvements

### Changed
- Migrated to OpenMRS 2.7+ compatibility
- Updated HAPI FHIR library to latest version
- Refactored FHIR resource generation
- Improved HTTP client connection management

### Fixed
- FHIR validation errors in resource generation
- Date/time handling in scheduled tasks
- Character encoding issues in HTTP communication
- Database transaction management

## [2.0.4] - 2024-08-20

### Added
- Support for custom FHIR resource types
- Enhanced profile configuration options
- Resource search parameter support
- Sync statistics dashboard

### Changed
- Performance optimizations for large datasets
- Improved memory management
- Enhanced error messages for troubleshooting

### Fixed
- N+1 query problems in data access layer
- Timeout issues in long-running sync operations
- Database connection pool exhaustion

## [2.0.3] - 2024-05-10

### Added
- Circuit breaker implementation for external system calls
- Retry mechanism with exponential backoff
- Sync task type management
- Task execution monitoring

### Changed
- Refactored HTTP communication layer
- Improved resource cleanup in error scenarios
- Enhanced logging for debugging

### Fixed
- Connection leaks in HTTP client
- Race conditions in parallel task execution
- Memory leaks in FHIR bundle processing

## [2.0.2] - 2024-02-28

### Added
- Integration testing framework
- API key authentication support
- Profile validation framework

### Changed
- Updated dependencies to latest stable versions
- Improved database migration scripts
- Enhanced REST API error responses

### Fixed
- Authentication issues in REST endpoints
- Validation errors in profile configuration
- Date parsing issues in various components

## [2.0.1] - 2023-11-15

### Added
- Initial implementation of scheduled tasks
- Basic FHIR resource generation
- DHIS2 integration
- REST API endpoints for CRUD operations

### Changed
- Module architecture refactoring
- Improved separation of concerns
- Enhanced service layer

### Fixed
- Critical bugs in resource transformation
- Database schema issues
- Configuration management problems

## [2.0.0] - 2023-08-01

### Major Changes
- Complete rewrite of the module
- Migration from legacy sync system to FHIR-based system
- New database schema
- REST API implementation
- Enhanced security framework

### Added
- FHIR R4 support
- Profile-based configuration system
- Case-based surveillance
- Scheduled task framework
- Comprehensive audit logging

### Migration Notes
- Requires manual migration from version 1.x
- Database migration script required
- Configuration files need to be updated
- Old scheduled tasks need to be recreated

## [1.5.2] - 2023-05-20

### Added
- Basic data sharing capabilities
- Simple HTTP-based communication
- Manual sync operations

### Deprecated
- Legacy sync system (to be replaced in 2.0.0)

## Version Classification

### [Major.Minor.Patch]

- **Major**: Incompatible API changes, architectural changes
- **Minor**: New functionality (backwards compatible), deprecations
- **Patch**: Bug fixes, minor improvements, documentation updates

### SNAPSHOT Releases

SNAPSHOT versions (`2.0.6-SNAPSHOT`) are development versions that:
- Contain the latest features and improvements
- May have unstable APIs
- Are under active development
- Should not be used in production

### Stable Releases

Stable releases (`2.0.5`, `2.0.4`, etc.) are production-ready:
- Thoroughly tested
- Stable APIs
- Documented features
- Recommended for production use

## Migration Guides

### Upgrading from 2.0.5 to 2.0.6-SNAPSHOT

**Warning**: SNAPSHOT versions are for development only. Do not use in production.

**New Features**:
```sql
-- Add new scheduling columns (will be added automatically in stable release)
ALTER TABLE sync_fhir_profile ADD COLUMN schedule_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE sync_fhir_profile ADD COLUMN schedule_type VARCHAR(20);
ALTER TABLE sync_fhir_profile ADD COLUMN fixed_rate_interval INT;
```

**Configuration Changes**:
- No breaking changes to existing configurations
- New optional scheduling parameters can be added incrementally

### Upgrading from 2.0.4 to 2.0.5

**Database Migration**: Automatic via Liquibase

**Code Changes**:
```java
// Old approach (deprecated)
HIVCaseBasedSurveillanceTask task = new HIVCaseBasedSurveillanceTask();

// New approach (recommended)
GenericFhirProfileSchedulerTask scheduler = new GenericFhirProfileSchedulerTask();
```

### Upgrading from 1.x to 2.0.0

**Major Migration Required**:

1. **Database Schema Migration**:
   ```sql
   -- Run Liquibase migrations
   -- Backup database before migration
   ```

2. **Configuration Migration**:
   - Old global properties need to be migrated to profile-based configuration
   - Scheduled tasks need to be recreated

3. **Code Changes**:
   - Direct database calls replaced with service layer
   - FHIR resource generation completely rewritten

4. **Testing**:
   - Thoroughly test in development environment
   - Validate FHIR resources before production deployment

## Deprecation Policy

### Deprecated Features

The following features are deprecated and will be removed in future versions:

- **Profile-specific scheduled tasks** (2.0.6): Use `GenericFhirProfileSchedulerTask` instead
- **Direct SQL execution in service layer** (2.0.5): Use DAO layer with parameterized queries
- **HTTP connection creation without pooling** (2.0.5): Use connection pooling

### Removal Timeline

- **Announcement**: Feature marked as deprecated
- **6 months**: Feature still supported but warnings issued
- **12 months**: Feature removed in next major version

## Support Lifecycle

### Version Support Status

| Version | Status          | Support Ends | Recommended |
|---------|-----------------|--------------|-------------|
| 2.0.6   | Development     | N/A          | No (dev)    |
| 2.0.5   | Current Stable  | Jun 2025     | Yes         |
| 2.0.4   | Maintenance     | Dec 2024     | No          |
| 2.0.3   | Security Only   | Aug 2024     | No          |
| 1.x     | End of Life     | Aug 2023     | No          |

### Support Policy

- **Current Stable**: Full support (bug fixes, security updates, feature requests)
- **Maintenance**: Critical bug fixes and security updates only
- **Security Only**: Security vulnerability fixes only
- **End of Life**: No support

## Contributing to Changelog

When contributing to the module, please:

1. **Add entries to the "Unreleased" section**
2. **Categorize changes** (Added, Changed, Deprecated, Removed, Fixed, Security)
3. **Reference issues** (e.g., "Fixed #123")
4. **Include migration notes** for breaking changes
5. **Update dated sections** when releasing

Example entry:
```markdown
### Added
- New feature description (#123)

### Fixed
- Bug fix description (#124)
```

## Release Process

1. **Development**: Work on features in `develop` branch
2. **Testing**: Thorough testing in development environment
3. **Documentation**: Update README, API docs, migration guides
4. **Release**: Create release branch and tag
5. **Announcement**: Publish release notes and announcements

## Release Notes Template

```markdown
## [Version] - YYYY-MM-DD

### Summary
Brief description of the release

### Highlights
- Major feature 1
- Major feature 2
- Critical bug fix 1

### Upgrade Instructions
Step-by-step upgrade guide

### Breaking Changes
List of breaking changes and migration steps

### Known Issues
List of known issues and workarounds

### Contributors
List of contributors to this release
```

## Additional Resources

- [GitHub Releases](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/releases)
- [Release Planning](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/milestones)
- [Issue Tracker](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)

---

**Maintained By**: METS Programme Development Team  
**Last Updated**: May 2, 2026