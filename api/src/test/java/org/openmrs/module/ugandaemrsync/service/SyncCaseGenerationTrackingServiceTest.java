package org.openmrs.module.ugandaemrsync.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncErrorType;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.Patient;

import java.util.Arrays;
import java.util.List;
import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * Test class for SyncCaseGenerationTrackingService.
 * Tests comprehensive error tracking during case-based FHIR resource generation.
 */
public class SyncCaseGenerationTrackingServiceTest extends BaseModuleContextSensitiveTest {

    protected static final String UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/globalPropertiesDataSet.xml";
    protected static final String UGANDAEMR_STANDARD_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/standardTestDataset.xml";

    private SyncCaseGenerationTrackingService caseTrackingService;
    private UgandaEMRSyncService mockSyncService;
    private SyncFhirCase testCase;
    private SyncFhirProfile testProfile;
    private Patient testPatient;

    @Before
    public void setup() throws Exception {
        executeDataSet(UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML);
        executeDataSet(UGANDAEMR_STANDARD_DATASET_XML);

        caseTrackingService = new SyncCaseGenerationTrackingService();
        mockSyncService = mock(UgandaEMRSyncService.class);

        // Create test profile
        testProfile = new SyncFhirProfile();
        testProfile.setName("Test Profile");
        testProfile.setCaseBasedPrimaryResourceType("EpisodeOfCare");

        // Create test patient
        testPatient = new Patient();
        testPatient.setId(123);

        // Create test case
        testCase = new SyncFhirCase();
        testCase.setCaseIdentifier("test-case-123");
        testCase.setProfile(testProfile);
        testCase.setPatient(testPatient);
    }

    /**
     * Test recording successful generation updates all status fields correctly
     */
    @Test
    public void recordSuccessfulGeneration_shouldUpdateAllStatusFields() {
        // Arrange
        testCase.setResourceGenerationStatus("FAILED");
        testCase.setConsecutiveGenerationFailures(3);
        testCase.setLastGenerationErrorType("RESOURCE_GENERATION_FAILED");

        SyncFhirResource mockResource = new SyncFhirResource();
        mockResource.setId(456);

        // Act
        caseTrackingService.recordSuccessfulGeneration(testCase, mockResource, mockSyncService);

        // Assert
        verify(mockSyncService, times(1)).saveSyncFHIRCase(testCase);
        Assert.assertEquals("Should mark as SUCCESS", "SUCCESS", testCase.getResourceGenerationStatus());
        Assert.assertEquals("Should reset consecutive failures", Integer.valueOf(0), testCase.getConsecutiveGenerationFailures());
        Assert.assertNull("Should clear error type", testCase.getLastGenerationErrorType());
        Assert.assertNotNull("Should set last generation attempt", testCase.getLastGenerationAttempt());
        Assert.assertNotNull("Should set last successful generation date", testCase.getLastSuccessfulGenerationDate());
    }

    /**
     * Test recording failed generation increments failure counters
     */
    @Test
    public void recordFailedGeneration_shouldIncrementFailureCounters() {
        // Arrange
        testCase.setConsecutiveGenerationFailures(2);

        // Act
        caseTrackingService.recordFailedGeneration(testCase, mockSyncService,
                SyncErrorType.RESOURCE_GENERATION_FAILED, "Failed to generate EpisodeOfCare", null);

        // Assert
        verify(mockSyncService, times(1)).saveSyncFHIRCase(testCase);
        Assert.assertEquals("Should mark as FAILED", "FAILED", testCase.getResourceGenerationStatus());
        Assert.assertEquals("Should increment consecutive failures", Integer.valueOf(3), testCase.getConsecutiveGenerationFailures());
        Assert.assertEquals("Should set error type", "RESOURCE_GENERATION_FAILED", testCase.getLastGenerationErrorType());
        Assert.assertTrue("Should contain error message", testCase.getLastGenerationErrorMessage().contains("Failed to generate EpisodeOfCare"));
    }

    /**
     * Test recording FHIR generation failure captures exception details
     */
    @Test
    public void recordFhirGenerationFailure_shouldCaptureExceptionDetails() {
        // Arrange
        Exception testException = new NullPointerException("Patient program is null");

        // Act
        caseTrackingService.recordFhirGenerationFailure(testCase, mockSyncService,
                testException, "EpisodeOfCare generation");

        // Assert
        Assert.assertEquals("Should set error type",
                "RESOURCE_GENERATION_FAILED", testCase.getLastGenerationErrorType());
        Assert.assertTrue("Should contain context info",
                testCase.getLastGenerationErrorMessage().contains("EpisodeOfCare generation"));
        Assert.assertTrue("Should contain exception info",
                testCase.getLastGenerationErrorMessage().contains("NullPointerException"));
    }

    /**
     * Test recording null or empty resource failure
     */
    @Test
    public void recordNullOrEmptyResourceFailure_shouldTrackNullOrEmptyResources() {
        // Act
        caseTrackingService.recordNullOrEmptyResourceFailure(testCase, mockSyncService,
                "EpisodeOfCare", "test-case-123");

        // Assert
        Assert.assertEquals("Should set error type",
                "RESOURCE_GENERATION_FAILED", testCase.getLastGenerationErrorType());
        Assert.assertTrue("Should contain resource type",
                testCase.getLastGenerationErrorMessage().contains("EpisodeOfCare"));
        Assert.assertTrue("Should indicate null or empty",
                testCase.getLastGenerationErrorMessage().contains("null or empty"));
    }

    /**
     * Test recording JSON transformation failure
     */
    @Test
    public void recordJsonTransformationFailure_shouldTrackTransformationErrors() {
        // Arrange
        Exception testException = new org.json.JSONException("Invalid JSON structure");

        // Act
        caseTrackingService.recordJsonTransformationFailure(testCase, mockSyncService,
                testException, "EpisodeOfCare JSON transformation");

        // Assert
        Assert.assertEquals("Should set error type",
                "JSON_TRANSFORMATION_FAILED", testCase.getLastGenerationErrorType());
        Assert.assertTrue("Should contain transformation context",
                testCase.getLastGenerationErrorMessage().contains("JSON transformation"));
    }

    /**
     * Test recording data validation error
     */
    @Test
    public void recordDataValidationError_shouldTrackValidationIssues() {
        // Act
        caseTrackingService.recordDataValidationError(testCase, mockSyncService,
                "Missing required field: startDate");

        // Assert
        Assert.assertEquals("Should set error type",
                "DATA_VALIDATION_FAILED", testCase.getLastGenerationErrorType());
        Assert.assertTrue("Should contain validation error",
                testCase.getLastGenerationErrorMessage().contains("Missing required field"));
    }

    /**
     * Test recording configuration error
     */
    @Test
    public void recordConfigurationError_shouldTrackConfigIssues() {
        // Act
        caseTrackingService.recordConfigurationError(testCase, mockSyncService,
                "Invalid program UUID in profile configuration");

        // Assert
        Assert.assertEquals("Should set error type",
                "CONFIGURATION_ERROR", testCase.getLastGenerationErrorType());
        Assert.assertTrue("Should contain config issue",
                testCase.getLastGenerationErrorMessage().contains("Invalid program UUID"));
    }

    /**
     * Test checking if case exceeded max failures
     */
    @Test
    public void hasExceededMaxGenerationFailures_shouldIdentifyProblematicCases() {
        // Arrange
        testCase.setConsecutiveGenerationFailures(5);

        // Act & Assert
        Assert.assertTrue("Should identify case exceeded max failures",
                testCase.hasExceededMaxGenerationFailures(3));

        testCase.setConsecutiveGenerationFailures(2);
        Assert.assertFalse("Should not exceed with 2 failures",
                testCase.hasExceededMaxGenerationFailures(3));
    }

    /**
     * Test getting case generation status summary
     */
    @Test
    public void getCaseGenerationReport_shouldProvideComprehensiveInformation() {
        // Arrange
        testCase.setResourceGenerationStatus("FAILED");
        testCase.setConsecutiveGenerationFailures(4);
        testCase.setLastGenerationErrorType("RESOURCE_GENERATION_FAILED");
        testCase.setLastGenerationErrorMessage("Patient program not found");

        // Act
        String report = caseTrackingService.getCaseGenerationReport(testCase);

        // Assert
        Assert.assertTrue("Should contain case ID", report.contains("test-case-123"));
        Assert.assertTrue("Should contain patient ID", report.contains("123"));
        Assert.assertTrue("Should contain status", report.contains("FAILED"));
        Assert.assertTrue("Should contain failure count", report.contains("Consecutive Failures: 4"));
        Assert.assertTrue("Should contain error type", report.contains("RESOURCE_GENERATION_FAILED"));
        Assert.assertTrue("Should contain error message", report.contains("Patient program not found"));
    }

    /**
     * Test skipping cases due to excessive failures
     */
    @Test
    public void shouldSkipCaseDueToFailures_shouldIdentifyCasesToSkip() {
        // Arrange
        testCase.setConsecutiveGenerationFailures(5);

        // Act & Assert
        Assert.assertTrue("Should skip case with excessive failures",
                caseTrackingService.shouldSkipCaseDueToFailures(testCase, 3));

        testCase.setConsecutiveGenerationFailures(2);
        Assert.assertFalse("Should not skip case with low failures",
                caseTrackingService.shouldSkipCaseDueToFailures(testCase, 3));
    }

    /**
     * Test getting cases generation summary
     */
    @Test
    public void getCasesGenerationSummary_shouldProvideStatistics() {
        // Arrange
        SyncFhirCase successCase = createTestCase("success-case", "SUCCESS", 0);
        SyncFhirCase failedCase = createTestCase("failed-case", "FAILED", 2);
        SyncFhirCase highFailureCase = createTestCase("high-failure-case", "FAILED", 5);
        SyncFhirCase pendingCase = createTestCase("pending-case", "PENDING", 0);

        List<SyncFhirCase> cases = Arrays.asList(successCase, failedCase, highFailureCase, pendingCase);

        // Act
        String summary = caseTrackingService.getCasesGenerationSummary(cases);

        // Assert
        Assert.assertTrue("Should contain total cases", summary.contains("Total Cases: 4"));
        Assert.assertTrue("Should contain success count", summary.contains("Successful: 1"));
        Assert.assertTrue("Should contain failed count", summary.contains("Failed: 2"));
        Assert.assertTrue("Should contain pending count", summary.contains("Pending/Unknown: 1"));
        Assert.assertTrue("Should contain high failure cases", summary.contains("High Failure Cases (>3 consecutive): 1"));
    }

    /**
     * Test first failure initializes counters properly
     */
    @Test
    public void recordFailedGeneration_shouldInitializeCountersOnFirstFailure() {
        // Arrange - Case with no previous failures
        testCase.setConsecutiveGenerationFailures(null);
        testCase.setTotalGenerationAttempts(null);

        // Act
        caseTrackingService.recordFailedGeneration(testCase, mockSyncService,
                SyncErrorType.NETWORK_TIMEOUT, "Connection timeout", null);

        // Assert
        Assert.assertEquals("Should initialize consecutive failures to 1",
                Integer.valueOf(1), testCase.getConsecutiveGenerationFailures());
        Assert.assertEquals("Should initialize total attempts to 1",
                Integer.valueOf(1), testCase.getTotalGenerationAttempts());
    }

    /**
     * Test that successful generation clears all error state
     */
    @Test
    public void recordSuccessfulGeneration_shouldClearAllErrorState() {
        // Arrange - Case with many failures and errors
        testCase.setResourceGenerationStatus("FAILED");
        testCase.setConsecutiveGenerationFailures(10);
        testCase.setTotalGenerationAttempts(15);
        testCase.setLastGenerationErrorType("RESOURCE_GENERATION_FAILED");
        testCase.setLastGenerationErrorMessage("Multiple generation failures");

        SyncFhirResource mockResource = new SyncFhirResource();
        mockResource.setId(789);

        // Act
        caseTrackingService.recordSuccessfulGeneration(testCase, mockResource, mockSyncService);

        // Assert
        Assert.assertEquals("Should mark as SUCCESS", "SUCCESS", testCase.getResourceGenerationStatus());
        Assert.assertEquals("Should reset consecutive failures to 0",
                Integer.valueOf(0), testCase.getConsecutiveGenerationFailures());
        Assert.assertNull("Should clear error type", testCase.getLastGenerationErrorType());
        Assert.assertNull("Should clear error message", testCase.getLastGenerationErrorMessage());
        Assert.assertNotNull("Should set last successful generation date", testCase.getLastSuccessfulGenerationDate());
    }

    /**
     * Test empty case list handling
     */
    @Test
    public void getCasesGenerationSummary_shouldHandleEmptyList() {
        // Act
        String summary = caseTrackingService.getCasesGenerationSummary(Arrays.asList());

        // Assert
        Assert.assertTrue("Should handle empty list", summary.contains("No cases to analyze"));
    }

    /**
     * Helper method to create test cases
     */
    private SyncFhirCase createTestCase(String caseId, String status, int failureCount) {
        SyncFhirCase testCase = new SyncFhirCase();
        testCase.setCaseIdentifier(caseId);
        testCase.setResourceGenerationStatus(status);
        testCase.setConsecutiveGenerationFailures(failureCount);
        testCase.setPatient(testPatient);
        return testCase;
    }
}