# Security

Security considerations and best practices for the UgandaEMR Sync Module.

## Overview

The UgandaEMR Sync Module handles sensitive healthcare data and implements multiple layers of security to protect patient information and ensure compliance with healthcare data protection standards.

## Security Features

### Authentication & Authorization

#### Role-Based Access Control (RBAC)

The module implements fine-grained access control through OpenMRS privilege system:

**Available Privileges**:
- **UgandaemrSync: Manage FHIR Profiles**: Create, edit, delete profiles
- **UgandaemrSync: Manage FHIR Resources**: Manage FHIR resources
- **UgandaemrSync: Manage FHIR Cases**: Manage case-based surveillance
- **UgandaemrSync: View Execution History**: View execution history and logs
- **UgandaemrSync: Run Manual Execution**: Trigger manual sync operations

#### Creating Secure Roles

```sql
-- Create sync administrator role
INSERT INTO role (name, description, uuid)
VALUES ('Sync Administrator', 'Full access to sync functions', UUID());

-- Create sync operator role
INSERT INTO role (name, description, uuid)
VALUES ('Sync Operator', 'Limited sync operations', UUID());

-- Grant privileges via Admin UI:
-- Administration → Users → Roles → Manage Roles
-- Assign appropriate privileges to each role
```

### API Security

#### Authentication

All REST API endpoints require authentication using Basic Authentication:

```bash
# All API calls must include authentication
curl -u username:password https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
```

#### Method-Level Security

Key service methods are protected with `@Secured` annotations:

```java
@Secured("UgandaemrSync: Manage FHIR Profiles")
public SyncFhirProfile saveSyncFhirProfile(SyncFhirProfile profile) {
    // Implementation
}

@Secured("UgandaemrSync: View Execution History")
public List<SyncFhirProfileExecutionHistory> getExecutionHistory(String profileUuid) {
    // Implementation
}
```

#### Resource-Level Security

REST resources implement privilege checking:

```java
@Resource(name = RestConstants.VERSION_1 + "/syncfhirprofile")
public class SyncFhirProfileResource extends DelegatingCrudResource<SyncFhirProfile> {
    
    @Override
    public NeedsPaging<SyncFhirProfile> doGetAll(RequestContext context) {
        // Privilege checking happens automatically
        // based on @Secured annotations in service layer
    }
}
```

### Data Encryption

#### Transmission Security

- **HTTPS/TLS**: All external communications use HTTPS
- **Certificate Validation**: SSL certificates are validated by default
- **Secure Protocols**: Only secure protocols (TLSv1.2+) are used

#### Credential Storage

```bash
# Credentials are stored securely in database
# Passwords should be encrypted in production
# Never store passwords in plain text

# Example: Profile credentials
UPDATE sync_fhir_profile 
SET url_password = AES_ENCRYPT('password', 'encryption-key')
WHERE uuid = 'profile-uuid';
```

### Audit Logging

#### Comprehensive Audit Trail

All module operations are logged with contextual information:

```java
// Audit log includes:
- User who performed the action
- Timestamp of the action
- Type of action (CREATE, UPDATE, DELETE, EXECUTE)
- Resources affected
- Execution results
- Error messages (if any)
```

#### Viewing Audit Logs

```sql
-- View execution history
SELECT 
  sp.name as profile,
  eph.execution_date,
  eph.execution_status,
  eph.resources_processed,
  eph.error_message
FROM sync_fhir_profile_execution_history eph
JOIN sync_fhir_profile sp ON eph.profile_id = sp.sync_fhir_profile_id
ORDER BY eph.execution_date DESC;
```

## Security Best Practices

### 1. Credential Management

#### Use Strong Passwords

```bash
# Generate strong passwords for external systems
# Use password manager or generator
# Minimum 16 characters with mixed case, numbers, symbols

# Example: Secure password generation
openssl rand -base64 24
```

#### Rotate Credentials Regularly

```sql
-- Update credentials periodically
-- Example: Rotate API keys every 90 days
UPDATE sync_fhir_profile 
SET url_password = 'new-secure-password'
WHERE url_password = 'old-password';
```

#### Use Service Accounts

```bash
# Create dedicated service accounts for external integrations
# Never use personal accounts for automation
# Grant minimum required privileges
```

### 2. Network Security

#### Firewall Configuration

```bash
# Restrict outbound connections
# Only allow necessary external systems
# Use IP whitelisting when possible

# Example: Allow specific external systems
iptables -A OUTPUT -d external-system.example.com -j ACCEPT
iptables -A OUTPUT -p tcp --dport 443 -j DROP  # Block others
```

#### VPN/Private Networks

```bash
# Use VPN for connections to external systems
# Prefer private network connections over public internet
# Implement network segmentation for healthcare data
```

### 3. SSL/TLS Configuration

#### Certificate Validation

```java
// Enable certificate validation
System.setProperty("javax.net.ssl.trustStore", "/path/to/truststore.jks");
System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

// Disable certificate validation ONLY in development
// NEVER in production
```

#### Protocol Configuration

```bash
# Use only secure protocols
export JAVA_OPTS="$JAVA_OPTS -Dhttps.protocols=TLSv1.2,TLSv1.3"

# Disable insecure protocols
export JAVA_OPTS="$JAVA_OPTS -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3"
```

### 4. Input Validation

#### SQL Injection Prevention

```java
// GOOD: Parameterized queries
public List<Map<String, Object>> executeQuery(String sql, Map<String, Object> params) {
    return jdbcTemplate.queryForList(sql, params);
}

// BAD: String concatenation (NEVER do this)
public List<Map<String, Object>> executeQueryBad(String sql, String param) {
    return jdbcTemplate.queryForList(sql + param);  // SQL injection risk
}
```

#### Input Sanitization

```java
// Validate and sanitize all user inputs
public void validateProfile(SyncFhirProfile profile) {
    if (profile.getName() == null || profile.getName().isEmpty()) {
        throw new IllegalArgumentException("Profile name is required");
    }
    
    // Sanitize input
    profile.setName(profile.getName().trim());
}
```

### 5. Error Handling

#### Secure Error Messages

```java
// GOOD: Generic error messages
try {
    processFhirData();
} catch (Exception e) {
    log.error("Error processing FHIR data", e);
    throw new UgandaEMRSyncException("Processing failed. Contact administrator.");
}

// BAD: Expose internal details
try {
    processFhirData();
} catch (Exception e) {
    throw new UgandaEMRSyncException("Error: " + e.getMessage());  // Information leak
}
```

#### Logging Sensitive Data

```java
// GOOD: Log only non-sensitive data
log.info("Processing profile: {}", profile.getName());

// BAD: Log sensitive data
log.info("Processing profile with password: {}", profile.getUrlPassword());  // Security risk
```

### 6. Session Management

#### API Session Timeout

```bash
# Configure session timeout
# Via OpenMRS: Administration → Advanced Settings → Global Properties
openmrs.session.timeout = 1800  # 30 minutes
```

#### Concurrent Session Control

```bash
# Limit concurrent sessions per user
# Prevent credential sharing
openmrs.concurrent.users.max = 1
```

## Compliance

### Healthcare Data Protection

#### Patient Data Privacy

- **HIPAA Compliance**: Module supports HIPAA requirements for protected health information (PHI)
- **Data Minimization**: Only necessary data is shared
- **Purpose Limitation**: Data is used only for intended purposes
- **Patient Consent**: Respects patient consent preferences

#### Data Sharing Agreements

- **Data Use Agreements**: Establish clear agreements before sharing data
- **Purpose Specification**: Clearly define purpose of data exchange
- **Retention Policies**: Implement data retention policies
- **Right to Access**: Support patient data access requests

### Audit Requirements

#### HIPAA Audit Log Requirements

```sql
-- Audit logs include:
- User identity
- Timestamp
- Activity performed
- Resources accessed
- Whether the activity was successful
- The clinical purpose of the activity
```

#### Regular Audit Reviews

```sql
-- Regular review of access logs
-- Identify unusual patterns
-- Review failed authentication attempts
-- Monitor data exports
```

## Security Monitoring

### Intrusion Detection

#### Monitor Failed Authentications

```sql
-- Track failed authentication attempts
SELECT 
  user_id,
  COUNT(*) as failed_attempts,
  MAX(timestamp) as last_attempt
FROM user_log
WHERE action = 'LOGIN_FAILED'
GROUP BY user_id
HAVING failed_attempts > 5
ORDER BY failed_attempts DESC;
```

#### Monitor Unusual Activity

```sql
-- Monitor for unusual data access patterns
SELECT 
  sph.name as profile,
  COUNT(*) as executions,
  eph.resources_processed
FROM sync_fhir_profile_execution_history eph
JOIN sync_fhir_profile sph ON eph.profile_id = sph.sync_fhir_profile_id
WHERE eph.execution_date > DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY sph.name
HAVING executions > 100;  # More than 100 executions per hour is unusual
```

### Security Alerts

#### Failed Sync Alerts

```bash
# Set up monitoring for failed syncs
# Alert if failure rate exceeds threshold
# Example: Email notification if 5 consecutive failures
```

#### Resource Usage Alerts

```bash
# Monitor for unusual resource usage
# Alert if sync volume suddenly increases
# Could indicate data breach
```

## Incident Response

### Security Incident Procedures

1. **Identify and Contain**
   - Stop affected sync operations
   - Disable compromised accounts
   - Isolate affected systems

2. **Assess Impact**
   - Review audit logs
   - Identify accessed data
   - Determine affected patients

3. **Notify Stakeholders**
   - Security team
   - Management
   - Regulatory bodies (if required)
   - Affected patients (if required)

4. **Remediate**
   - Patch vulnerabilities
   - Update credentials
   - Implement additional controls

5. **Document**
   - Incident timeline
   - Actions taken
   - Lessons learned
   - Prevention measures

### Reporting Security Issues

**For security vulnerabilities or issues**:
- **Private Disclosure**: security@mets.or.ug
- **GitHub Security**: Use GitHub Security Advisory features
- **Do NOT publicly disclose** sensitive security issues

## Security Checklist

### Before Deployment

- [ ] All default passwords changed
- [ ] SSL/TLS properly configured
- [ ] Firewall rules configured
- [ ] Role-based access control configured
- [ ] Audit logging enabled
- [ ] Error handling reviewed
- [ ] Input validation implemented
- [ ] Dependencies updated
- [ ] Security scan performed
- [ ] Data use agreements in place

### Regular Maintenance

- [ ] Review and rotate credentials (quarterly)
- [ ] Update dependencies (monthly)
- [ ] Review audit logs (weekly)
- [ ] Test backup and recovery (monthly)
- [ ] Security awareness training (annually)
- [ ] Compliance audit (annually)

---

**Last Updated**: May 2, 2026  
**Version**: 2.0.6-SNAPSHOT  
**Security Policy**: Healthcare Data Protection Compliant