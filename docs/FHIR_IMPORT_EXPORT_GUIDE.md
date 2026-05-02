# FHIR Profile Import/Export System - User Guide

## Quick Start

### Export a Profile

**Using REST API:**
```bash
# Export single profile
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profile/123

# Export all profiles
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/all

# Export by category
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/category/DISEASE_SURVEILLANCE

# Export to file
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/file \
  -d "ids=123,456,789" \
  -d "format=JSON" \
  -o profiles_export.json
```

**Example Export Output:**
```json
{
  "version": "1.0",
  "exportedAt": "2024-05-01T10:30:00Z",
  "metadata": {
    "exportedBy": "admin",
    "sourceInstance": "openmrs-2.7.4",
    "version": "1.0",
    "description": "Production profile export"
  },
  "profiles": [
    {
      "metadata": {
        "id": "profile-123",
        "name": "HIV Case Based Surveillance",
        "category": "DISEASE_SURVEILLANCE",
        "description": "FHIR sync profile for HIV case surveillance",
        "version": "1.0"
      },
      "configuration": {
        "resourceTypes": "Patient,Condition,Observation",
        "isCaseBasedProfile": true,
        "caseBasedPrimaryResourceType": "Condition",
        "generateBundle": true,
        "numberOfResourcesInBundle": 50,
        "profileEnabled": true,
        "endpoint": {
          "url": "https://moh.gov.ug/fhir",
          "authentication": "BASIC",
          "username": "facility_user",
          "password": "{{CONFIGURE_PASSWORD}}"
        }
      }
    }
  ],
  "taskTypes": []
}
```

### Import a Profile

**Using REST API:**
```bash
# Validate before import
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/validate \
  -H "Content-Type: application/json" \
  -d @profiles_export.json

# Import from file
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@profiles_export.json"
```

## Real-World Usage Scenarios

### Scenario 1: Export from Development, Import to Production

**Step 1: Export from Development**
```bash
# Export HIV surveillance profile
curl -X GET http://dev-emr:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profile/5 \
  -o hiv_surveillance_profile.json

# Review the export
cat hiv_surveillance_profile.json | jq '.profiles[0].metadata'
```

**Step 2: Customize for Production**
```bash
# Edit the configuration for production environment
vi hiv_surveillance_profile.json

# Update endpoint URL
# Update authentication credentials
# Adjust resource search parameters
```

**Step 3: Validate Before Import**
```bash
# Validate the configuration
curl -X POST http://prod-emr:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/validate \
  -H "Content-Type: application/json" \
  -d @hiv_surveillance_profile.json
```

**Step 4: Import to Production**
```bash
# Import the profile
curl -X POST http://prod-emr:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@hiv_surveillance_profile.json"
```

### Scenario 2: Backup All Profiles Before Changes

```bash
# Export all profiles as backup
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/all \
  -o backup_$(date +%Y%m%d_%H%M%S)_all_profiles.json

# Verify backup
ls -lh backup_*.json
```

### Scenario 3: Share Profile with Team

**Step 1: Export with Metadata**
```bash
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/file \
  -d "ids=123" \
  -d "format=JSON" \
  -o covid_lab_results_profile.json
```

**Step 2: Share with Team**
```bash
# Upload to team shared drive
# Or email the file
# Or commit to version control
git add covid_lab_results_profile.json
git commit -m "Add COVID-19 lab results profile"
git push
```

**Step 3: Team Member Imports**
```bash
# Download from shared location
# Import to their instance
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@covid_lab_results_profile.json"
```

### Scenario 4: Mass Profile Deployment

**Export Multiple Related Profiles:**
```bash
# Export all disease surveillance profiles
curl -X GET "http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/category/DISEASE_SURVEILLANCE" \
  -o disease_surveillance_profiles.json

# Export all lab exchange profiles
curl -X GET "http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/category/LAB_EXCHANGE" \
  -o lab_exchange_profiles.json
```

**Deploy to Multiple Facilities:**
```bash
# Define list of facilities
facilities=("facility1.emr.org" "facility2.emr.org" "facility3.emr.org")

# Deploy to each facility
for facility in "${facilities[@]}"; do
  echo "Deploying to $facility..."

  # Import profiles
  curl -X POST "http://$facility/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file" \
    -F "file=@disease_surveillance_profiles.json"

  echo "Deployed to $facility successfully"
done
```

## Advanced Features

### Export Statistics

**Check what's available for export:**
```bash
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/stats
```

**Sample Response:**
```json
{
  "totalProfiles": 15,
  "totalTaskTypes": 8,
  "enabledProfiles": 12,
  "caseBasedProfiles": 10,
  "profilesByCategory": {
    "DISEASE_SURVEILLANCE": 5,
    "LAB_EXCHANGE": 4,
    "REPORTING": 3,
    "DEFAULT": 3
  }
}
```

### Custom Export with Metadata

**Export with custom metadata:**
```bash
# This would be done programmatically
# Example using a script that calls the export service with custom metadata
```

### Encrypted Export

**Export sensitive profiles with encryption:**
```bash
# This feature is implemented but needs proper encryption keys
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/file \
  -d "ids=123,456" \
  -d "format=ENCRYPTED_JSON" \
  -d "encryptionKey=your-secret-key" \
  -o encrypted_profiles.json
```

## Central Hub Integration (Future)

### Push Profile to Hub

```bash
# Push a profile to the central hub
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/hub/push/123 \
  -H "Content-Type: application/json" \
  -d '{
    "hubUrl": "https://hub.mets.or.ug",
    "apiKey": "your-api-key"
  }'
```

### Pull Profile from Hub

```bash
# Pull a profile from the central hub
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/hub/pull/hiv-surveillance-001 \
  -H "Content-Type: application/json" \
  -d '{
    "hubUrl": "https://hub.mets.or.ug",
    "apiKey": "your-api-key"
  }'
```

### List Available Hub Profiles

```bash
# List all profiles available in the hub
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/hub/list \
  -H "Content-Type: application/json" \
  -d '{
    "hubUrl": "https://hub.mets.or.ug",
    "apiKey": "your-api-key"
  }'
```

## Best Practices

### 1. Always Validate Before Import
```bash
# Step 1: Validate
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/validate \
  -d @profile.json

# Step 2: Review validation results
# Step 3: Only import if validation passes
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@profile.json"
```

### 2. Backup Before Bulk Operations
```bash
# Create backup
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/all \
  -o backup_before_changes.json

# Perform your operations
# ...

# Restore if needed
curl -X POST http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file \
  -F "file=@backup_before_changes.json"
```

### 3. Use Version Control for Profiles
```bash
# Initialize a profile repository
mkdir fhir-profiles
cd fhir-profiles
git init

# Export and save profiles
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profiles/all \
  -o profiles.json

# Commit to version control
git add profiles.json
git commit -m "Initial profile set"
git push origin main
```

### 4. Environment-Specific Configuration

**Development:**
```json
{
  "endpoint": {
    "url": "http://dev-server.example/fhir",
    "username": "dev_user",
    "password": "dev_password"
  },
  "profileEnabled": false
}
```

**Production:**
```json
{
  "endpoint": {
    "url": "https://prod-server.example/fhir",
    "username": "prod_user",
    "password": "{{CONFIGURE_PASSWORD}}"
  },
  "profileEnabled": true
}
```

### 5. Profile Documentation

**Always include clear descriptions:**
```json
{
  "metadata": {
    "name": "HIV Case Surveillance",
    "description": "Exchanges HIV case data with Ministry of Health. Includes Patient, Condition, and Observation resources. Runs every hour.",
    "tags": ["hiv", "surveillance", "ministry", "production"],
    "author": "John Doe",
    "version": "1.2"
  }
}
```

## Troubleshooting

### Export Errors

**Problem:** Profile not found
```bash
# Check profile exists first
curl -X GET http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/stats
```

**Problem:** Large export fails
```bash
# Export individual profiles instead of all
for id in 123 456 789; do
  curl -X GET "http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/export/profile/$id" \
    -o "profile_$id.json"
done
```

### Import Errors

**Problem:** Invalid JSON format
```bash
# Validate JSON before importing
cat profile.json | jq '.'
```

**Problem:** Profile name conflict
```bash
# This requires conflict resolution (to be implemented)
# For now, rename in JSON before importing
cat profile.json | jq '.profiles[0].metadata.name = "New Name"' > profile_new.json
```

## File Format Reference

### Export Structure

```json
{
  "version": "1.0",
  "exportedAt": "ISO-8601 timestamp",
  "metadata": {
    "exportedBy": "username",
    "sourceInstance": "instance-name",
    "version": "export-version",
    "description": "export description",
    "tags": ["tag1", "tag2"]
  },
  "profiles": [
    {
      "metadata": {
        "id": "unique-id",
        "name": "Profile Name",
        "category": "CATEGORY",
        "description": "Description",
        "version": "1.0",
        "author": "Author Name",
        "createdAt": "timestamp",
        "updatedAt": "timestamp",
        "tags": ["tag1", "tag2"]
      },
      "configuration": {
        "resourceTypes": "Patient,Condition",
        "isCaseBasedProfile": true,
        "caseBasedPrimaryResourceType": "Condition",
        "resourceSearchParameter": "{}",
        "generateBundle": true,
        "numberOfResourcesInBundle": 50,
        "profileEnabled": true,
        "keepProfileIdentifierOnly": false,
        "durationToKeepSyncedResources": 30,
        "syncLimit": 100,
        "searchable": true,
        "searchURL": "http://example.com/search",
        "endpoint": {
          "url": "http://example.com/fhir",
          "authentication": "BASIC|TOKEN",
          "username": "username",
          "password": "{{PLACEHOLDER}}",
          "token": "token-value"
        }
      }
    }
  ],
  "taskTypes": [
    {
      "metadata": { ... },
      "configuration": {
        "name": "Task Name",
        "description": "Description",
        "taskClass": "com.example.Task",
        "taskType": "DATA_SEND"
      }
    }
  ]
}
```

## Integration Examples

### 1. Automated Backup Script

```bash
#!/bin/bash
# backup_fhir_profiles.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/fhir_profiles"
API_URL="http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Export all profiles
curl -X GET "$API_URL/export/profiles/all" \
  -o "$BACKUP_DIR/profiles_$DATE.json"

# Export task types
curl -X GET "$API_URL/export/tasktypes" \
  -o "$BACKUP_DIR/tasktypes_$DATE.json"

# Keep only last 30 days of backups
find "$BACKUP_DIR" -name "*.json" -mtime +30 -delete

echo "Backup completed: profiles_$DATE.json"
```

### 2. Profile Deployment Script

```bash
#!/bin/bash
# deploy_profile.sh

PROFILE_FILE=$1
TARGET_ENV=$2

if [ -z "$PROFILE_FILE" ] || [ -z "$TARGET_ENV" ]; then
  echo "Usage: $0 <profile-file> <target-environment>"
  exit 1
fi

# Validate profile first
echo "Validating profile..."
VALIDATION=$(curl -s -X POST \
  "http://localhost:8080/openmrs/ws/rest/v1/ugandaemrsync/io/import/validate" \
  -H "Content-Type: application/json" \
  -d @"$PROFILE_FILE")

VALID=$(echo $VALIDATION | jq '.valid')

if [ "$VALID" != "true" ]; then
  echo "Validation failed:"
  echo $VALIDATION | jq '.errors'
  exit 1
fi

echo "Validation passed, importing profile..."

# Import profile
RESULT=$(curl -s -X POST \
  "http://$TARGET_ENV/openmrs/ws/rest/v1/ugandaemrsync/io/import/profile/file" \
  -F "file=@$PROFILE_FILE")

SUCCESS=$(echo $RESULT | jq '.success')

if [ "$SUCCESS" == "true" ]; then
  echo "Profile imported successfully!"
else
  echo "Import failed:"
  echo $RESULT | jq '.error'
  exit 1
fi
```

### 3. Profile Comparison Tool

```python
#!/usr/bin/env python3
import json
import sys

def compare_profiles(file1, file2):
    with open(file1) as f1, open(file2) as f2:
        profile1 = json.load(f1)
        profile2 = json.load(f2)

    # Compare basic metadata
    if profile1['metadata']['name'] != profile2['metadata']['name']:
        print(f"Name differs: {profile1['metadata']['name']} vs {profile2['metadata']['name']}")

    # Compare configuration
    config1 = profile1['configuration']
    config2 = profile2['configuration']

    if config1['resourceTypes'] != config2['resourceTypes']:
        print(f"Resource types differ")

    if config1['endpoint']['url'] != config2['endpoint']['url']:
        print(f"Endpoint URL differs: {config1['endpoint']['url']} vs {config2['endpoint']['url']}")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python3 compare_profiles.py file1.json file2.json")
        sys.exit(1)

    compare_profiles(sys.argv[1], sys.argv[2])
```

## Summary

This import/export system provides:

1. ✅ **Easy backup and restore** of FHIR sync configurations
2. ✅ **Configuration sharing** between development, testing, and production
3. ✅ **Team collaboration** through profile sharing
4. ✅ **Version control** integration for profile management
5. ✅ **Rapid deployment** across multiple facilities
6. ✅ **Safety features** like validation and preview
7. ✅ **Flexibility** with multiple export formats
8. ✅ **Security** through encryption options

The system transforms profile management from a manual, error-prone process into a streamlined, automated workflow that saves time and reduces configuration errors.