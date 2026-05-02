# Hybrid FHIR Profile Scheduler Architecture

## **Overview**

The UgandaEMR Sync module now supports a **hybrid scheduler architecture** that provides maximum flexibility for FHIR profile execution. This design allows both simple and complex profiles to coexist seamlessly.

## **Architecture Design**

### **Two Execution Paths:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    HYBRID SCHEDULER ARCHITECTURE                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  FHIR Profile Database Table                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ sync_fhir_profile                                      │   │
│  │ • profile_enabled: true/false                          │   │
│  │ • schedule_enabled: true/false                         │   │
│  │ • custom_task_class: NULL or "com.example.CustomTask" │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│                            │                                    │
│              ┌─────────────┴─────────────┐                    │
│              │                           │                    │
│              ▼                           ▼                    │
│    ┌──────────────────┐        ┌──────────────────┐          │
│    │  custom_task_class│        │  custom_task_class│          │
│    │      = NULL       │        │   = "CustomTask" │          │
│    └──────────────────┘        └──────────────────┘          │
│              │                           │                    │
│              ▼                           ▼                    │
│    ┌──────────────────┐        ┌──────────────────┐          │
│    │ Generic Scheduler │        │ Custom Scheduler  │          │
│    │  Task            │        │  Task             │          │
│    │                  │        │                  │          │
│    │ • Handles all    │        │ • Handles one     │          │
│    │   standard       │        │   specific        │          │
│    │   profiles       │        │   profile         │          │
│    │ • Runs every     │        │ • Custom schedule │          │
│    │   5 minutes      │        │ • Custom logic    │          │
│    └──────────────────┘        └──────────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## **When to Use Each Approach**

### **Use Generic Scheduler (custom_task_class = NULL) when:**

✅ **Simple profiles** with standard execution requirements
✅ **Resource-based profiles** sending data to external systems
✅ **Case-based profiles** with standard sync patterns
✅ **Profiles that don't need specialized logic**
✅ **Most common use cases**

**Examples:**
- Send lab results to CPHL
- Sync patient demographics to central server
- Daily HMIS reports
- Standard FHIR resource synchronization

### **Use Custom Scheduler (custom_task_class = "com.example.YourTask") when:**

✅ **Complex profiles** requiring specialized execution logic
✅ **Profiles with unique scheduling requirements**
✅ **Profiles needing custom error handling**
✅ **Profiles with complex data transformation logic**
✅ **Profiles requiring external system integration**

**Examples:**
- Multi-step data validation and transformation
- Profiles requiring interaction with multiple external systems
- Complex retry logic with exponential backoff
- Profiles requiring database transactions across multiple tables

## **Configuration Examples**

### **Example 1: Standard Profile (Generic Scheduler)**

```sql
-- Standard profile managed by generic scheduler
UPDATE sync_fhir_profile SET
  name = 'Daily Lab Results Sync',
  profile_enabled = 1,
  schedule_enabled = 1,
  schedule_type = 'FIXED_RATE',
  fixed_rate_interval = 3600000, -- 1 hour
  custom_task_class = NULL,  -- Uses generic scheduler
  execution_priority = 5
WHERE sync_fhir_profile_id = 1;
```

**Result:** Profile runs every hour via GenericFhirProfileSchedulerTask

### **Example 2: Custom Profile (Dedicated Scheduler)**

```sql
-- Complex profile with custom scheduler
UPDATE sync_fhir_profile SET
  name = 'Complex Multi-System Sync',
  profile_enabled = 1,
  schedule_enabled = 1,
  schedule_type = 'CRON',
  cron_expression = '0 0 2 * * ?', -- 2 AM daily
  custom_task_class = 'org.openmrs.module.ugandaemrsync.tasks.ComplexSyncTask',
  execution_priority = 1
WHERE sync_fhir_profile_id = 2;
```

**Result:** Profile runs at 2 AM daily via ComplexSyncTask

### **Example 3: Manual Profile (No Scheduler)**

```sql
-- Manual execution only
UPDATE sync_fhir_profile SET
  name = 'Emergency Data Export',
  profile_enabled = 1,
  schedule_enabled = 0,  -- No automatic scheduling
  schedule_type = 'MANUAL',
  custom_task_class = NULL
WHERE sync_fhir_profile_id = 3;
```

**Result:** Profile only runs when triggered manually via REST API

## **Creating Custom Scheduler Tasks**

### **Step 1: Create Custom Task Class**

```java
package org.openmrs.module.ugandaemrsync.tasks;

import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;

public class ComplexSyncTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(ComplexSyncTask.class);

    @Override
    public void execute() {
        log.info("Starting Complex Multi-System Sync");

        try {
            UgandaEMRSyncService service = Context.getService(UgandaEMRSyncService.class);

            // Get the profile this task is responsible for
            SyncFhirProfile profile = service.getSyncFhirProfileByName("Complex Multi-System Sync");

            if (profile != null && profile.getProfileEnabled()) {
                // Your custom execution logic here
                executeComplexSync(profile);
            }

        } catch (Exception e) {
            log.error("Error in Complex Multi-System Sync", e);
        }
    }

    private void executeComplexSync(SyncFhirProfile profile) {
        // Custom implementation
        // - Multi-step validation
        // - Complex data transformation
        // - External system integration
        // - Custom error handling
    }
}
```

### **Step 2: Register Custom Task**

**Option A: Via Liquibase (Automatic)**

```xml
<!-- Add to liquibase changelog -->
<changeSet id="add-complex-sync-task" author="your-name">
    <insert tableName="scheduler_task_config">
        <column name="name" value="Complex Multi-System Sync"/>
        <column name="description" value="Handles complex multi-system data synchronization"/>
        <column name="task_class" value="org.openmrs.module.ugandaemrsync.tasks.ComplexSyncTask"/>
        <column name="start_time" valueDate="2024-01-01T02:00:00"/>
        <column name="repeat_interval" value="86400000"/> <!-- 24 hours -->
        <column name="started" valueBoolean="true"/>
        <column name="creator" value="1"/>
        <column name="uuid" value="your-uuid-here"/>
    </insert>
</changeSet>
```

**Option B: Via OpenMRS UI (Manual)**

1. Go to: Administration → Scheduler
2. Click "Add Task"
3. Fill in:
   - Name: Complex Multi-System Sync
   - Description: Handles complex multi-system data synchronization
   - Task Class: org.openmrs.module.ugandaemrsync.tasks.ComplexSyncTask
   - Schedule: Cron expression "0 0 2 * * ?"
4. Save and start the task

### **Step 3: Assign Custom Task to Profile**

```sql
UPDATE sync_fhir_profile SET
  custom_task_class = 'org.openmrs.module.ugandaemrsync.tasks.ComplexSyncTask'
WHERE name = 'Complex Multi-System Sync';
```

## **Benefits of Hybrid Architecture**

### **1. Simplicity for Common Cases**
- ✅ No need to create task classes for standard profiles
- ✅ Generic scheduler handles most use cases
- ✅ Easy configuration through database/UI

### **2. Flexibility for Complex Cases**
- ✅ Custom tasks for specialized requirements
- ✅ No limitations on execution logic
- ✅ Full control over scheduling and error handling

### **3. Backward Compatibility**
- ✅ Existing profiles continue to work unchanged
- ✅ No breaking changes to current implementation
- ✅ Gradual migration to hybrid model

### **4. Maintainability**
- ✅ Separation of concerns (generic vs. custom)
- ✅ Easier to debug and test
- ✅ Clear ownership of profile execution

## **Migration Guide**

### **For Existing Profiles**

**Current State:** All profiles managed by individual task classes

**Option 1: Migrate to Generic Scheduler (Recommended for simple profiles)**

```sql
-- Remove custom task assignment to use generic scheduler
UPDATE sync_fhir_profile SET
  custom_task_class = NULL,
  schedule_type = 'FIXED_RATE',
  fixed_rate_interval = 3600000  -- Set desired interval
WHERE name IN ('Simple Lab Results', 'Basic Patient Sync');
```

**Option 2: Keep Custom Task (For complex profiles)**

```sql
-- Keep existing custom task assignment
UPDATE sync_fhir_profile SET
  custom_task_class = 'org.openmrs.module.ugandaemrsync.tasks.YourExistingTask'
WHERE name = 'Complex Profile';
```

### **For New Profiles**

**Default Approach:** Start with generic scheduler

```sql
-- New profile using generic scheduler
INSERT INTO sync_fhir_profile (
  name, profile_enabled, schedule_enabled,
  schedule_type, fixed_rate_interval, custom_task_class
) VALUES (
  'New Standard Profile', 1, 1,
  'FIXED_RATE', 3600000, NULL
);
```

**Advanced Approach:** Use custom task when needed

```sql
-- New complex profile with custom scheduler
INSERT INTO sync_fhir_profile (
  name, profile_enabled, schedule_enabled,
  schedule_type, cron_expression, custom_task_class
) VALUES (
  'New Complex Profile', 1, 1,
  'CRON', '0 0 2 * * ?', 'org.openmrs.module.ugandaemrsync.tasks.NewComplexTask'
);
```

## **Troubleshooting**

### **Issue: Profile Not Running**

**Check 1: Verify profile configuration**
```sql
SELECT name, profile_enabled, schedule_enabled, custom_task_class
FROM sync_fhir_profile
WHERE name = 'Your Profile';
```

**Check 2: Generic scheduler logs**
```
Generic FHIR Profile Scheduler Task - Profile has custom task class, skipping
```

**Check 3: Custom task status**
```
Administration → Scheduler → Find your custom task → Check if running
```

### **Issue: Wrong Task Executing Profile**

**Symptom:** Profile executing with wrong logic

**Solution:** Verify custom_task_class assignment
```sql
UPDATE sync_fhir_profile SET
  custom_task_class = 'correct.task.ClassName'
WHERE name = 'Your Profile';
```

### **Issue: Performance Problems**

**Symptom:** Generic scheduler running slowly

**Solution:** Move complex profiles to custom tasks
```sql
-- Move resource-intensive profiles to custom tasks
UPDATE sync_fhir_profile SET
  custom_task_class = 'org.openmrs.module.ugandaemrsync.tasks.HighPerformanceTask'
WHERE resource_types LIKE '%large%';
```

## **Summary**

The hybrid scheduler architecture provides:

1. **Flexibility**: Choose the right approach for each profile
2. **Simplicity**: Most profiles need no custom code
3. **Power**: Complex profiles can have dedicated implementations
4. **Compatibility**: Works with existing implementations
5. **Maintainability**: Clear separation of concerns

**Best Practice:** Start with generic scheduler, move to custom tasks only when needed.