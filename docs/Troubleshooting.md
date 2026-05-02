# Troubleshooting

Common issues and solutions for the UgandaEMR Sync Module.

## Table of Contents

- [Installation Issues](#installation-issues)
- [Profile Issues](#profile-issues)
- [Connection Issues](#connection-issues)
- [Performance Issues](#performance-issues)
- [Data Issues](#data-issues)
- [Security Issues](#security-issues)
- [Logging and Debugging](#logging-and-debugging)

## Installation Issues

### Module Not Starting

**Symptoms**: Module doesn't appear in module list or shows "Stopped" status

**Solutions**:

1. **Check OpenMRS Version**
   ```bash
   # Verify OpenMRS version compatibility
   # Minimum required: OpenMRS 2.7.0
   ```

2. **Check Module Logs**
   ```bash
   tail -f /openmrs/logs/openmrs.log | grep ugandaemrsync
   ```

3. **Verify Dependencies**
   ```bash
   # Check if all required modules are installed
   # Navigate to: Administration → Manage Modules
   # Verify FHIR2 module is installed and started
   ```

4. **Check Database Permissions**
   ```sql
   -- Verify database user has required permissions
   SHOW GRANTS FOR 'openmrs'@'localhost';
   ```

### Database Tables Not Created

**Symptoms**: Tables starting with `sync_` are missing

**Solutions**:

1. **Check Liquibase Logs**
   ```bash
   grep -i liquibase /openmrs/logs/openmrs.log
   ```

2. **Manually Run Migrations**
   ```bash
   # Restart OpenMRS to trigger migrations
   # Check if migrations run successfully
   ```

3. **Verify Table Creation**
   ```sql
   -- Check if sync tables exist
   SHOW TABLES LIKE 'sync_%';
   
   -- Expected tables:
   -- sync_fhir_profile
   -- sync_fhir_resource
   -- sync_fhir_case
   -- sync_task
   ```

## Profile Issues

### Profile Not Executing

**Symptoms**: Profile shows "WAITING" but never executes

**Solutions**:

1. **Check Profile Configuration**
   ```sql
   -- Verify profile is enabled
   SELECT name, profile_enabled, schedule_enabled, next_execution_date
   FROM sync_fhir_profile 
   WHERE name = 'Your Profile Name';
   ```

2. **Enable Profile**
   ```sql
   -- Enable profile and scheduling
   UPDATE sync_fhir_profile 
   SET profile_enabled = TRUE, schedule_enabled = TRUE 
   WHERE name = 'Your Profile Name';
   ```

3. **Set Next Execution Date**
   ```sql
   -- Set next execution to now
   UPDATE sync_fhir_profile 
   SET next_execution_date = NOW() 
   WHERE name = 'Your Profile Name';
   ```

4. **Check Scheduler Task**
   ```bash
   # Via Admin UI:
   # Navigate to: Administration → Scheduler → Manage Tasks
   # Verify Generic FHIR Profile Scheduler is running
   ```

### Profile Executes But Generates No Resources

**Symptoms**: Profile runs but generates 0 resources

**Solutions**:

1. **Check Profile Configuration**
   ```sql
   -- Verify resource types and search parameters
   SELECT name, resource_types, generate_bundle, 
          resource_search_parameter, case_based_primary_resource_type
   FROM sync_fhir_profile 
   WHERE name = 'Your Profile Name';
   ```

2. **Verify Data Exists**
   ```sql
   -- Check if data exists in system
   SELECT COUNT(*) FROM patient WHERE voided = 0;
   SELECT COUNT(*) FROM encounter WHERE voided = 0;
   SELECT COUNT(*) FROM obs WHERE voided = 0;
   ```

3. **Enable Debug Logging**
   ```xml
   <!-- Add to log4j.xml -->
   <logger name="org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord">
     <level value="DEBUG"/>
   </logger>
   ```

4. **Check Resource Queries**
   ```bash
   # Review logs for SQL queries
   tail -f /openmrs/logs/openmrs.log | grep -i "executing query"
   ```

## Connection Issues

### Connection Failures to External Systems

**Symptoms**: "FAILED" status with connection errors

**Solutions**:

1. **Test External System Connectivity**
   ```bash
   # Test basic connectivity
   curl -v https://external-system.example.com/fhir
   
   # Test with credentials
   curl -u username:password https://external-system.example.com/fhir/Patient
   ```

2. **Verify Connection Details**
   ```sql
   -- Check profile connection configuration
   SELECT name, url, url_user_name 
   FROM sync_fhir_profile 
   WHERE name = 'Your Profile Name';
   ```

3. **Update Credentials**
   ```sql
   -- Update connection details
   UPDATE sync_fhir_profile 
   SET url = 'https://correct-url.example.com/fhir',
       url_user_name = 'correct-username',
       url_password = 'correct-password'
   WHERE name = 'Your Profile Name';
   ```

4. **Check SSL/TLS Configuration**
   ```bash
   # Test SSL connection
   openssl s_client -connect external-system.example.com:443
   
   # Verify certificate chain
   ```

5. **Review Firewall Settings**
   ```bash
   # Check if outbound connections are allowed
   telnet external-system.example.com 443
   ```

### Timeout Errors

**Symptoms**: Connections timeout after some time

**Solutions**:

1. **Increase Timeout Settings**
   ```sql
   -- Update global properties
   UPDATE global_property 
   SET property_value = '60000'  -- 60 seconds
   WHERE property = 'ugandaemrsync.read.timeout';
   ```

2. **Optimize Batch Size**
   ```sql
   -- Reduce batch size to reduce processing time
   UPDATE sync_fhir_profile 
   SET number_of_resources_in_bundle = 25
   WHERE name = 'Your Profile Name';
   ```

3. **Check Network Latency**
   ```bash
   # Test network latency
   ping external-system.example.com
   ```

## Performance Issues

### Slow Performance

**Symptoms**: Sync operations take too long

**Solutions**:

1. **Optimize Batch Size**
   ```sql
   -- Reduce batch size for faster processing
   UPDATE sync_fhir_profile 
   SET number_of_resources_in_bundle = 25
   WHERE name = 'Your Profile Name';
   ```

2. **Add Sync Limit**
   ```sql
   -- Limit resources processed per run
   UPDATE sync_fhir_profile 
   SET sync_limit = 500
   WHERE name = 'Your Profile Name';
   ```

3. **Adjust Schedule Frequency**
   ```sql
   -- Run less frequently for large datasets
   UPDATE sync_fhir_profile 
   SET fixed_rate_interval = 3600000  -- 1 hour
   WHERE name = 'Your Profile Name';
   ```

4. **Check Database Performance**
   ```sql
   -- Check for slow queries
   SHOW PROCESSLIST;
   
   -- Analyze query performance
   EXPLAIN SELECT * FROM patient WHERE voided = 0;
   ```

### Memory Issues

**Symptoms**: OutOfMemoryError or system becomes unresponsive

**Solutions**:

1. **Reduce Memory Usage**
   ```sql
   -- Reduce batch size significantly
   UPDATE sync_fhir_profile 
   SET number_of_resources_in_bundle = 10,
       sync_limit = 100
   WHERE name = 'Your Profile Name';
   ```

2. **Increase JVM Memory**
   ```bash
   # Add to CATALINA_OPTS
   export CATALINA_OPTS="$CATALINA_OPTS -Xmx2g -Xms1g"
   ```

3. **Enable Memory Monitoring**
   ```bash
   # Monitor JVM memory usage
   jstat -gc <pid> 1000
   ```

## Data Issues

### Data Not Syncing

**Symptoms**: Resources created but not synced to external system

**Solutions**:

1. **Check Sync Status**
   ```sql
   -- Check status of resources
   SELECT sync_status, COUNT(*) 
   FROM sync_fhir_resource 
   GROUP BY sync_status;
   ```

2. **Manually Trigger Sync**
   ```bash
   # Trigger manual execution
   curl -X POST \
     https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile/PROFILE_UUID/execute \
     -u admin:Admin123
   ```

3. **Review Sync Logs**
   ```sql
   -- Check execution history for errors
   SELECT * FROM sync_fhir_profile_execution_history
   WHERE execution_status = 'FAILED'
   ORDER BY execution_date DESC
   LIMIT 10;
   ```

### Incorrect Data Transformation

**Symptoms**: FHIR resources contain incorrect data

**Solutions**:

1. **Review FHIR Mapping**
   ```bash
   # Enable debug logging to see transformation
   # Check logs for transformation details
   ```

2. **Validate FHIR Resources**
   ```bash
   # Get sample FHIR resource
   curl -u admin:Admin123 \
     https://your-openmrs/openmrs/ws/rest/v1/syncfhirresource/RESOURCE_UUID
   
   # Validate against FHIR schema
   ```

3. **Check Source Data**
   ```sql
   -- Verify source data in OpenMRS
   SELECT * FROM patient WHERE patient_id = <id>;
   SELECT * FROM encounter WHERE encounter_id = <id>;
   ```

## Security Issues

### Authentication Failures

**Symptoms**: 401 Unauthorized errors

**Solutions**:

1. **Verify Credentials**
   ```bash
   # Test API credentials
   curl -u username:password \
     https://your-openmrs/openmrs/ws/rest/v1/syncfhirprofile
   ```

2. **Check User Privileges**
   ```sql
   -- Verify user has required privileges
   SELECT p.privilege 
   FROM user_role ur 
   JOIN role_privilege rp ON ur.role_id = rp.role_id
   JOIN privilege p ON rp.privilege_id = p.privilege_id
   WHERE ur.user_id = <user_id>
   AND p.privilege LIKE 'UgandaemrSync:%';
   ```

3. **Grant Required Privileges**
   ```bash
   # Via Admin UI:
   # Administration → Users → Roles → Manage Roles
   # Add UgandaemrSync privileges
   ```

### SSL/TLS Certificate Errors

**Symptoms**: SSL handshake failures

**Solutions**:

1. **Import Certificates**
   ```bash
   # Import certificate into truststore
   keytool -import -alias external-system \
     -file certificate.crt \
     -keystore /path/to/truststore.jks
   ```

2. **Configure SSL Context**
   ```bash
   # Set SSL system properties
   export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=/path/to/truststore.jks"
   ```

## Logging and Debugging

### Enable Debug Logging

```xml
<!-- Add to OpenMRS log4j.xml -->
<logger name="org.openmrs.module.ugandaemrsync">
  <level value="DEBUG"/>
</logger>

<logger name="org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord">
  <level value="TRACE"/>
</logger>

<logger name="org.openmrs.module.ugandaemrsync.api.impl.UgandaEMRSyncServiceImpl">
  <level value="DEBUG"/>
</logger>
```

### Monitor Profile Execution

```sql
-- Real-time monitoring
SELECT 
  name,
  last_execution_status,
  last_execution_date,
  next_execution_date,
  CASE 
    WHEN next_execution_date <= NOW() THEN 'DUE NOW'
    WHEN last_execution_status = 'RUNNING' THEN 'RUNNING'
    WHEN last_execution_status = 'FAILED' THEN 'FAILED'
    ELSE 'WAITING'
  END as status
FROM sync_fhir_profile
WHERE profile_enabled = TRUE
ORDER BY execution_priority, next_execution_date;
```

### View Recent Activity

```sql
-- Last 20 sync operations
SELECT 
  sp.name as profile,
  eph.execution_date,
  eph.execution_status,
  eph.resources_processed,
  eph.execution_duration_ms,
  eph.error_message
FROM sync_fhir_profile_execution_history eph
JOIN sync_fhir_profile sp ON eph.profile_id = sp.sync_fhir_profile_id
ORDER BY eph.execution_date DESC
LIMIT 20;
```

### Check Resource Status

```sql
-- Resource status by profile
SELECT 
  sp.name as profile,
  sfr.sync_status,
  COUNT(*) as count
FROM sync_fhir_resource sfr
JOIN sync_fhir_profile sp ON sfr.profile_id = sp.sync_fhir_profile_id
GROUP BY sp.name, sfr.sync_status
ORDER BY sp.name, sfr.sync_status;
```

### Common Log Patterns

```bash
# Search for errors
grep -i "error" /openmrs/logs/openmrs.log | grep ugandaemrsync

# Search for profile execution
grep "Executing profile" /openmrs/logs/openmrs.log

# Search for connection issues
grep "connection" /openmrs/logs/openmrs.log | grep ugandaemrsync

# Search for FHIR generation
grep "Generating FHIR" /openmrs/logs/openmrs.log
```

## Getting Help

### Diagnostic Information Collection

Before requesting help, collect:

1. **Module Version**
   ```bash
   # Via Admin UI: Administration → Manage Modules
   # Find "UgandaEMR Sync Module" and note version
   ```

2. **OpenMRS Version**
   ```bash
   # Via Admin UI: Administration → System Information
   ```

3. **Relevant Logs**
   ```bash
   # Collect recent logs
   tail -n 1000 /openmrs/logs/openmrs.log > debug-logs.txt
   ```

4. **Profile Configuration**
   ```sql
   -- Export profile configuration
   SELECT * FROM sync_fhir_profile WHERE name = 'Problem Profile';
   ```

5. **Execution History**
   ```sql
   -- Get recent execution history
   SELECT * FROM sync_fhir_profile_execution_history
   WHERE profile_id = (SELECT sync_fhir_profile_id FROM sync_fhir_profile WHERE name = 'Problem Profile')
   ORDER BY execution_date DESC LIMIT 20;
   ```

### Support Channels

- **GitHub Issues**: [Report bugs or request features](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- **OpenMRS Talk**: [Community discussions](https://talk.openmrs.org/)
- **Documentation**: [Full documentation](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/blob/master/DOCUMENTATION_INDEX.md)

### Emergency Procedures

If sync is completely broken:

1. **Stop All Scheduled Tasks**
   ```bash
   # Via Admin UI:
   # Administration → Scheduler → Manage Tasks
   # Stop all sync tasks
   ```

2. **Check Module Status**
   ```bash
   curl -u admin:Admin123 \
     https://your-openmrs/openmrs/ws/rest/v1/module?name=ugandaemrsync
   ```

3. **Review Error Logs**
   ```bash
   tail -f /openmrs/logs/openmrs.log | grep ugandaemrsync
   ```

4. **Restart Module**
   ```bash
   # Via Admin UI:
   # Administration → Manage Modules
   # Stop and Start UgandaEMR Sync module
   ```

5. **Test with Simple Profile**
   ```bash
   # Create a basic test profile and verify it works
   # Use minimal configuration for testing
   ```

---

**Last Updated**: May 2, 2026  
**Version**: 2.0.6-SNAPSHOT