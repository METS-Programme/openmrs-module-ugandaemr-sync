package org.openmrs.module.ugandaemrsync.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncErrorType;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * Test class for SyncErrorTrackingService.
 * Tests comprehensive sync error tracking and status management.
 */
public class SyncErrorTrackingServiceTest extends BaseModuleContextSensitiveTest {

    protected static final String UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/globalPropertiesDataSet.xml";
    protected static final String UGANDAEMR_STANDARD_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/standardTestDataset.xml";

    private SyncErrorTrackingService syncErrorTrackingService;
    private UgandaEMRSyncService mockSyncService;
    private SyncFhirResource testResource;

    @Before
    public void setup() throws Exception {
        executeDataSet(UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML);
        executeDataSet(UGANDAEMR_STANDARD_DATASET_XML);

        syncErrorTrackingService = new SyncErrorTrackingService();
        mockSyncService = mock(UgandaEMRSyncService.class);

        // Create test resource
        testResource = new SyncFhirResource();
        testResource.setResource("{\"resourceType\": \"Patient\"}");
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Profile");
        testResource.setGeneratorProfile(profile);
    }

    /**
     * Test recording successful sync updates all status fields correctly
     */
    @Test
    public void recordSuccessfulSync_shouldUpdateAllStatusFields() {
        // Arrange
        testResource.setSynced(false);
        testResource.setConsecutiveFailureCount(3);
        testResource.setLastSyncErrorType(SyncErrorType.HTTP_CONNECTION_FAILED.name());
        testResource.setLastSyncErrorMessage("Previous failure");

        // Act
        syncErrorTrackingService.recordSuccessfulSync(testResource, mockSyncService);

        // Assert
        verify(mockSyncService, times(1)).saveFHIRResource(testResource);
        Assert.assertTrue("Should be marked as synced", testResource.getSynced());
        Assert.assertNotNull("Should have date synced", testResource.getDateSynced());
        Assert.assertNotNull("Should have last sync attempt", testResource.getLastSyncAttempt());
        Assert.assertEquals("Should reset consecutive failures", Integer.valueOf(0), testResource.getConsecutiveFailureCount());
        Assert.assertNull("Should clear error type", testResource.getLastSyncErrorType());
        Assert.assertNull("Should clear error message", testResource.getLastSyncErrorMessage());
    }

    /**
     * Test recording failed sync increments failure counters
     */
    @Test
    public void recordFailedSync_shouldIncrementFailureCounters() {
        // Arrange
        testResource.setConsecutiveFailureCount(2);

        // Act
        syncErrorTrackingService.recordFailedSync(testResource, mockSyncService,
                SyncErrorType.HTTP_CONNECTION_FAILED, "Connection timeout", null);

        // Assert
        verify(mockSyncService, times(1)).saveFHIRResource(testResource);
        Assert.assertFalse("Should not be marked as synced", testResource.getSynced());
        Assert.assertNotNull("Should have last sync attempt", testResource.getLastSyncAttempt());
        Assert.assertEquals("Should increment consecutive failures", Integer.valueOf(3), testResource.getConsecutiveFailureCount());
        Assert.assertEquals("Should set error type", "HTTP_CONNECTION_FAILED", testResource.getLastSyncErrorType());
        Assert.assertTrue("Should contain error message", testResource.getLastSyncErrorMessage().contains("Connection timeout"));
    }

    /**
     * Test recording HTTP sync result handles success cases
     */
    @Test
    public void recordHttpSyncResult_shouldHandleSuccessCases() {
        // Act
        syncErrorTrackingService.recordHttpSyncResult(testResource, mockSyncService,
                200, "OK", true);

        // Assert
        verify(mockSyncService, times(1)).saveFHIRResource(testResource);
        Assert.assertTrue("Should be marked as synced", testResource.getSynced());
        Assert.assertEquals("Should set status code", Integer.valueOf(200), testResource.getStatusCode());
        Assert.assertEquals("Should set status detail", "OK", testResource.getStatusCodeDetail());
    }

    /**
     * Test recording HTTP sync result handles client errors correctly
     */
    @Test
    public void recordHttpSyncResult_shouldHandleClientErrors() {
        // Act
        syncErrorTrackingService.recordHttpSyncResult(testResource, mockSyncService,
                404, "Not Found", false);

        // Assert
        verify(mockSyncService, times(1)).saveFHIRResource(testResource);
        Assert.assertFalse("Should not be marked as synced", testResource.getSynced());
        Assert.assertEquals("Should set status code", Integer.valueOf(404), testResource.getStatusCode());
        Assert.assertEquals("Should categorize as client error", "HTTP_CLIENT_ERROR", testResource.getLastSyncErrorType());
    }

    /**
     * Test recording HTTP sync result handles authentication errors
     */
    @Test
    public void recordHttpSyncResult_shouldHandleAuthenticationErrors() {
        // Act
        syncErrorTrackingService.recordHttpSyncResult(testResource, mockSyncService,
                401, "Unauthorized", false);

        // Assert
        Assert.assertEquals("Should categorize as authentication error",
                "HTTP_AUTHENTICATION_FAILED", testResource.getLastSyncErrorType());
    }

    /**
     * Test recording resource generation failure captures error details
     */
    @Test
    public void recordResourceGenerationFailure_shouldCaptureErrorDetails() {
        // Arrange
        Exception testException = new NullPointerException("Patient UUID is null");

        // Act
        syncErrorTrackingService.recordResourceGenerationFailure(testResource, mockSyncService,
                testException, "Patient resource creation");

        // Assert
        Assert.assertEquals("Should set error type",
                "RESOURCE_GENERATION_FAILED", testResource.getLastSyncErrorType());
        Assert.assertTrue("Should contain context info",
                testResource.getLastSyncErrorMessage().contains("Patient resource creation"));
        Assert.assertTrue("Should contain exception info",
                testResource.getLastSyncErrorMessage().contains("NullPointerException"));
    }

    /**
     * Test recording JSON transformation failure
     */
    @Test
    public void recordJsonTransformationFailure_shouldTrackTransformationErrors() {
        // Arrange
        Exception testException = new org.json.JSONException("Invalid JSON structure");

        // Act
        syncErrorTrackingService.recordJsonTransformationFailure(testResource, mockSyncService,
                testException, "Patient JSON transformation");

        // Assert
        Assert.assertEquals("Should set error type",
                "JSON_TRANSFORMATION_FAILED", testResource.getLastSyncErrorType());
        Assert.assertTrue("Should contain transformation context",
                testResource.getLastSyncErrorMessage().contains("JSON transformation"));
    }

    /**
     * Test exceeding max consecutive failures triggers alert
     */
    @Test
    public void hasExceededMaxFailures_shouldIdentifyProblematicResources() {
        // Arrange & Act & Assert
        testResource.setConsecutiveFailureCount(6);
        Assert.assertTrue("Should identify resource exceeded max failures",
                testResource.hasExceededMaxFailures(5));

        testResource.setConsecutiveFailureCount(3);
        Assert.assertFalse("Should not exceed with 3 failures",
                testResource.hasExceededMaxFailures(5));
    }

    /**
     * Test sync status summary provides comprehensive information
     */
    @Test
    public void getSyncStatusSummary_shouldProvideComprehensiveInformation() {
        // Arrange
        testResource.setUuid("test-uuid-123");
        testResource.setSynced(false);
        testResource.setConsecutiveFailureCount(3);
        testResource.setLastSyncErrorType("HTTP_CONNECTION_FAILED");
        testResource.setLastSyncErrorMessage("Connection timeout");
        testResource.setStatusCode(500);

        // Act
        String summary = syncErrorTrackingService.getSyncStatusSummary(testResource);

        // Assert
        Assert.assertTrue("Should contain UUID", summary.contains("test-uuid-123"));
        Assert.assertTrue("Should contain failure count", summary.contains("Consecutive Failures: 3"));
        Assert.assertTrue("Should contain error type", summary.contains("HTTP_CONNECTION_FAILED"));
        Assert.assertTrue("Should contain error message", summary.contains("Connection timeout"));
        Assert.assertTrue("Should contain status code", summary.contains("HTTP Status Code: 500"));
    }

    /**
     * Test retry count increments properly
     */
    @Test
    public void recordFailedSync_shouldIncrementRetryCount() {
        // Arrange
        testResource.setSyncRetryCount(2);

        // Act
        syncErrorTrackingService.recordFailedSync(testResource, mockSyncService,
                SyncErrorType.HTTP_SERVER_ERROR, "Internal server error", null);

        // Assert
        Assert.assertEquals("Should increment retry count", Integer.valueOf(3), testResource.getSyncRetryCount());
    }

    /**
     * Test recording configuration error
     */
    @Test
    public void recordConfigurationError_shouldTrackConfigIssues() {
        // Act
        syncErrorTrackingService.recordConfigurationError(testResource, mockSyncService,
                "Invalid endpoint URL");

        // Assert
        Assert.assertEquals("Should set error type",
                "CONFIGURATION_ERROR", testResource.getLastSyncErrorType());
        Assert.assertTrue("Should contain config issue",
                testResource.getLastSyncErrorMessage().contains("Invalid endpoint URL"));
    }

    /**
     * Test that successful sync clears all error state
     */
    @Test
    public void recordSuccessfulSync_shouldClearAllErrorState() {
        // Arrange - Set up a resource with many failures and errors
        testResource.setSynced(false);
        testResource.setConsecutiveFailureCount(10);
        testResource.setSyncRetryCount(15);
        testResource.setLastSyncErrorType("HTTP_CONNECTION_FAILED");
        testResource.setLastSyncErrorMessage("Connection timeout");
        testResource.setStatusCode(500);

        // Act
        syncErrorTrackingService.recordSuccessfulSync(testResource, mockSyncService);

        // Assert
        Assert.assertTrue("Should be marked as synced", testResource.getSynced());
        Assert.assertEquals("Should reset consecutive failures to 0",
                Integer.valueOf(0), testResource.getConsecutiveFailureCount());
        Assert.assertNull("Should clear error type", testResource.getLastSyncErrorType());
        Assert.assertNull("Should clear error message", testResource.getLastSyncErrorMessage());
        Assert.assertNotNull("Should set last successful sync date", testResource.getLastSuccessfulSyncDate());
    }

    /**
     * Test first failure initializes counters properly
     */
    @Test
    public void recordFailedSync_shouldInitializeCountersOnFirstFailure() {
        // Arrange - Resource with no previous failures
        testResource.setConsecutiveFailureCount(null);
        testResource.setSyncRetryCount(null);

        // Act
        syncErrorTrackingService.recordFailedSync(testResource, mockSyncService,
                SyncErrorType.NETWORK_TIMEOUT, "Request timeout", null);

        // Assert
        Assert.assertEquals("Should initialize consecutive failures to 1",
                Integer.valueOf(1), testResource.getConsecutiveFailureCount());
        Assert.assertEquals("Should initialize retry count to 1",
                Integer.valueOf(1), testResource.getSyncRetryCount());
    }
}