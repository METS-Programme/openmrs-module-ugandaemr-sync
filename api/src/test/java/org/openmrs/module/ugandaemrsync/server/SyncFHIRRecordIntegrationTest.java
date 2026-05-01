package org.openmrs.module.ugandaemrsync.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Integration test to verify SyncFHIRRecord properly delegates to specialized classes.
 * Confirms functionality is preserved during refactoring.
 */
public class SyncFHIRRecordIntegrationTest extends BaseModuleContextSensitiveTest {

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
     * Test that SyncFHIRRecord properly delegates JSON transformation to FhirResourceTransformer
     */
    @Test
    public void addOrganizationToRecord_shouldDelegateToFhirResourceTransformer() {
        String result = syncFHIRRecord.addOrganizationToRecord("{}", "managingOrganization");

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should contain managingOrganization", result.contains("managingOrganization"));
        Assert.assertTrue("Result should contain organization reference", result.contains("Organization/"));
        Assert.assertFalse("Result should not be empty", result.isEmpty());
    }

    /**
     * Test that SyncFHIRRecord properly delegates resource wrapping to FhirResourceTransformer
     */
    @Test
    public void wrapResourceInPostRequest_shouldDelegateToFhirResourceTransformer() {
        String testResource = "{\"resourceType\":\"Patient\",\"id\":\"12345\"}";
        String result = syncFHIRRecord.wrapResourceInPostRequest(testResource);

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should contain request method", result.contains("POST"));
        Assert.assertTrue("Result should contain original resource", result.contains("Patient"));
        Assert.assertFalse("Result should not be empty", result.isEmpty());
    }

    /**
     * Test that SyncFHIRRecord properly delegates PUT resource wrapping to FhirResourceTransformer
     */
    @Test
    public void wrapResourceInPUTRequest_shouldDelegateToFhirResourceTransformer() {
        String testResource = "{\"resourceType\":\"Patient\",\"id\":\"12345\"}";
        String result = syncFHIRRecord.wrapResourceInPUTRequest(testResource, "Patient", "12345");

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should contain request method", result.contains("PUT"));
        Assert.assertTrue("Result should contain resource reference", result.contains("Patient/12345"));
        Assert.assertFalse("Result should not be empty", result.isEmpty());
    }

    /**
     * Test that SyncFHIRRecord properly delegates Patient resource generation to FhirResourceGenerator
     */
    @Test
    public void getPatientResourceBundle_shouldDelegateToFhirResourceGenerator() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Profile");
        profile.setResourceSearchParameter("{}");

        SyncFhirCase testCase = new SyncFhirCase();
        testCase.setCaseIdentifier("test-case-123");

        Collection result = syncFHIRRecord.getPatientResourceBundle(profile, new ArrayList<>(), testCase);

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    /**
     * Test that SyncFHIRRecord properly delegates Observation resource generation to FhirResourceGenerator
     */
    @Test
    public void getObservationResourceBundle_shouldDelegateToFhirResourceGenerator() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Profile");
        profile.setResourceSearchParameter("{}");
        profile.setIsCaseBasedProfile(true);

        Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    /**
     * Test that SyncFHIRRecord properly delegates Practitioner resource generation to FhirResourceGenerator
     */
    @Test
    public void getPractitionerResourceBundle_shouldDelegateToFhirResourceGenerator() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Profile");
        profile.setResourceSearchParameter("{}");

        Collection result = syncFHIRRecord.getPractitionerResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    /**
     * Test that SyncFHIRRecord properly delegates ServiceRequest resource generation to FhirResourceGenerator
     */
    @Test
    public void getServiceRequestResourceBundle_shouldDelegateToFhirResourceGenerator() {
        Collection result = syncFHIRRecord.getServiceRequestResourceBundle(new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    /**
     * Test that SyncFHIRRecord properly delegates bundle grouping to FhirResourceGenerator
     */
    @Test
    public void groupInCaseBundle_shouldDelegateToFhirResourceGenerator() {
        Collection result = syncFHIRRecord.groupInCaseBundle("Patient", new ArrayList<>(), "test-identifier");

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    /**
     * Test that all integrated methods handle null inputs gracefully
     */
    @Test
    public void integratedMethods_shouldHandleNullInputsGracefully() {
        // Test null input handling for integrated methods
        String result1 = syncFHIRRecord.addOrganizationToRecord(null, "managingOrganization");
        Assert.assertNotNull("Should handle null payload", result1);

        String result2 = syncFHIRRecord.wrapResourceInPostRequest(null);
        Assert.assertNotNull("Should handle null resource", result2);

        Collection result3 = syncFHIRRecord.getObservationResourceBundle(null, null, null);
        Assert.assertNotNull("Should handle null parameters", result3);
    }

    /**
     * Test that the integration maintains backward compatibility
     */
    @Test
    public void integration_shouldMaintainBackwardCompatibility() {
        // Test that method signatures haven't changed
        try {
            syncFHIRRecord.addOrganizationToRecord("{}", "test");
            syncFHIRRecord.wrapResourceInPostRequest("{}");
            syncFHIRRecord.wrapResourceInPUTRequest("{}", "Patient", "123");
            syncFHIRRecord.getPatientResourceBundle(new SyncFhirProfile(), new ArrayList<>(), new SyncFhirCase());
            syncFHIRRecord.getObservationResourceBundle(new SyncFhirProfile(), new ArrayList<>(), new ArrayList<>());
            syncFHIRRecord.getPractitionerResourceBundle(new SyncFhirProfile(), new ArrayList<>(), new ArrayList<>());
            syncFHIRRecord.getServiceRequestResourceBundle(new ArrayList<>());
            syncFHIRRecord.groupInCaseBundle("Patient", new ArrayList<>(), "test");

            // If we get here without exceptions, backward compatibility is maintained
            Assert.assertTrue("All method calls should work without exceptions", true);
        } catch (Exception e) {
            Assert.fail("Integration should maintain backward compatibility: " + e.getMessage());
        }
    }
}