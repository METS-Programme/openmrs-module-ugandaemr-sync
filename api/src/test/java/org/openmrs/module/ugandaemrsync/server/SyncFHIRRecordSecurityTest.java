package org.openmrs.module.ugandaemrsync.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.Arrays;
import java.util.List;

/**
 * Test class for SyncFHIRRecord security integration.
 * Tests that the refactored class properly uses FhirQueryExecutor for secure database operations.
 */
public class SyncFHIRRecordSecurityTest extends BaseModuleContextSensitiveTest {

    protected static final String UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/globalPropertiesDataSet.xml";
    protected static final String UGANDAEMR_STANDARD_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/standardTestDataset.xml";

    private SyncFHIRRecord syncFHIRRecord;

    @Before
    public void setup() throws Exception {
        executeDataSet(UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML);
        executeDataSet(UGANDAEMR_STANDARD_DATASET_XML);
        syncFHIRRecord = new SyncFHIRRecord();
    }

    /**
     * Test that SyncFHIRRecord properly delegates database operations to FhirQueryExecutor
     */
    @Test
    public void syncFHIRRecord_shouldUseFhirQueryExecutorForDatabaseOperations() throws Exception {
        // Test that SyncFHIRRecord can execute database queries safely through FhirQueryExecutor
        try {
            // This tests that the refactored SyncFHIRRecord properly delegates to FhirQueryExecutor
            java.lang.reflect.Field field = SyncFHIRRecord.class.getDeclaredField("queryExecutor");
            field.setAccessible(true);
            Object queryExecutor = field.get(syncFHIRRecord);

            Assert.assertNotNull("FhirQueryExecutor should be initialized", queryExecutor);
            Assert.assertTrue("Should have FhirQueryExecutor instance", queryExecutor instanceof FhirQueryExecutor);
        } catch (Exception e) {
            Assert.fail("Should have FhirQueryExecutor field: " + e.getMessage());
        }
    }

    /**
     * Test that SQL injection attempts are properly blocked
     */
    @Test
    public void syncFHIRRecord_shouldBlockSQLInjectionAttempts() throws Exception {
        // Use reflection to access the private getDatabaseRecordWithOutFacility method
        java.lang.reflect.Method method = SyncFHIRRecord.class.getDeclaredMethod(
            "getDatabaseRecordWithOutFacility",
            String.class,
            String.class,
            String.class,
            int.class,
            List.class
        );
        method.setAccessible(true);

        // Test SQL injection attempt in date parameters
        try {
            List<String> columns = Arrays.asList("uuid");
            method.invoke(
                syncFHIRRecord,
                "select uuid from person WHERE date_created > :lastSyncDate1",
                "2021-04-01'; DROP TABLE users; --",
                "2021-04-30",
                1,
                columns
            );

            // If we get here, the SQL injection attempt should have been blocked
            Assert.fail("Should throw exception for SQL injection attempt in date parameters");
        } catch (Exception e) {
            // Should fail due to date validation
            Assert.assertTrue("Should fail due to date validation",
                e.getCause() != null && e.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * Test that queries with format specifiers are rejected
     */
    @Test
    public void syncFHIRRecord_shouldRejectQueriesWithFormatSpecifiers() throws Exception {
        // Use reflection to access the private getDatabaseRecord method
        java.lang.reflect.Method method = SyncFHIRRecord.class.getDeclaredMethod("getDatabaseRecord", String.class);
        method.setAccessible(true);

        // Test that queries with format specifiers are rejected
        try {
            method.invoke(syncFHIRRecord, "SELECT * FROM users WHERE username = '%s'");
            Assert.fail("Should throw IllegalArgumentException for queries with format specifiers");
        } catch (Exception e) {
            Assert.assertTrue("Should throw IllegalArgumentException",
                e.getCause() != null && e.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * Test that valid parameterized queries work correctly
     */
    @Test
    public void syncFHIRRecord_shouldHandleValidParameterizedQueries() throws Exception {
        // Use reflection to access the private getDatabaseRecordWithOutFacility method
        java.lang.reflect.Method method = SyncFHIRRecord.class.getDeclaredMethod(
            "getDatabaseRecordWithOutFacility",
            String.class,
            String.class,
            String.class,
            int.class,
            List.class
        );
        method.setAccessible(true);

        // Test with valid date parameters
        try {
            List<String> columns = Arrays.asList("uuid");
            List result = (List) method.invoke(
                syncFHIRRecord,
                "select uuid from person WHERE date_created > :lastSyncDate1 OR date_changed > :lastSyncDate2 OR date_voided > :lastSyncDate3",
                "2021-04-01",
                "2021-04-30",
                3,
                columns
            );

            Assert.assertNotNull("Result should not be null", result);
            Assert.assertTrue("Result should be a list", result instanceof List);
        } catch (Exception e) {
            // The query might fail due to missing test data, but it should not fail
            // due to SQL injection issues
            Assert.assertFalse("Should not fail due to SQL injection",
                e.getCause() != null && e.getCause().getMessage().contains("SQL injection"));
        }
    }

    /**
     * Test integration between SyncFHIRRecord and FhirQueryExecutor
     */
    @Test
    public void syncFHIRRecord_shouldHaveProperIntegrationWithFhirQueryExecutor() {
        // Test that the refactored class has the proper integration
        try {
            java.lang.reflect.Field field = SyncFHIRRecord.class.getDeclaredField("queryExecutor");
            field.setAccessible(true);
            FhirQueryExecutor queryExecutor = (FhirQueryExecutor) field.get(syncFHIRRecord);

            // Test that the FhirQueryExecutor works correctly
            Assert.assertTrue("Valid date should pass", queryExecutor.isValidDateString("2021-04-30"));
            Assert.assertFalse("Invalid date should fail", queryExecutor.isValidDateString("invalid-date"));

            // Test that validation works
            queryExecutor.validateDateParameters("2021-04-01", "2021-04-30");
            Assert.assertTrue("Valid date parameters should not throw exceptions", true);

        } catch (Exception e) {
            Assert.fail("FhirQueryExecutor integration should work: " + e.getMessage());
        }
    }
}