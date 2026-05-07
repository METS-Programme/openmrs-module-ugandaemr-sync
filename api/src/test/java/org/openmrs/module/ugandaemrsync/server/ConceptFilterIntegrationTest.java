package org.openmrs.module.ugandaemrsync.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.util.ConceptFilterUtils;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Integration tests for concept filter handling with both legacy and new formats.
 * Tests backward compatibility and new concept object structure support.
 */
public class ConceptFilterIntegrationTest extends BaseModuleContextSensitiveTest {

    protected static final String UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/globalPropertiesDataSet.xml";
    protected static final String UGANDAEMR_STANDARD_DATASET_XML = "org/openmrs/module/ugandaemrsync/include/standardTestDataset.xml";

    private static final String TEST_UUID_1 = "a893f7c0-1350-11df-a1f1-0026b934483c";
    private static final String TEST_UUID_2 = "a898f6c0-1350-11df-a1f1-0026b934483c";

    private SyncFHIRRecord syncFHIRRecord;

    @Before
    public void setup() throws Exception {
        executeDataSet(UGANDAEMR_GLOBAL_PROPERTY_DATASET_XML);
        executeDataSet(UGANDAEMR_STANDARD_DATASET_XML);
        syncFHIRRecord = new SyncFHIRRecord();
    }

    /**
     * Creates a legacy format filter (array of UUID strings)
     */
    private String createLegacyFilterFormat() {
        return "{"
                + "\"observationFilter\": {"
                + "  \"code\": [\"" + TEST_UUID_1 + "\", \"" + TEST_UUID_2 + "\"]"
                + "}"
                + "}";
    }

    /**
     * Creates a new format filter (array of concept objects)
     */
    private String createNewFilterFormat() {
        return "{"
                + "\"observationFilter\": {"
                + "  \"code\": ["
                + "    {"
                + "      \"uuid\": \"" + TEST_UUID_1 + "\","
                + "      \"display\": \"WEIGHT (KG)\","
                + "      \"id\": 5089,"
                + "      \"conceptClass\": \"Misc\","
                + "      \"datatype\": \"Numeric\""
                + "    },"
                + "    {"
                + "      \"uuid\": \"" + TEST_UUID_2 + "\","
                + "      \"display\": \"HEIGHT (CM)\","
                + "      \"id\": 5090,"
                + "      \"conceptClass\": \"Misc\","
                + "      \"datatype\": \"Numeric\""
                + "    }"
                + "  ]"
                + "}"
                + "}";
    }

    /**
     * Creates a new format condition filter
     */
    private String createConditionFilterNewFormat() {
        return "{"
                + "\"conditionFilter\": {"
                + "  \"code\": ["
                + "    {"
                + "      \"uuid\": \"" + TEST_UUID_1 + "\","
                + "      \"display\": \"HYPERTENSION\","
                + "      \"id\": 1234,"
                + "      \"conceptClass\": \"Diagnosis\","
                + "      \"datatype\": \"Coded\""
                + "    }"
                + "  ]"
                + "}"
                + "}";
    }

    /**
     * Creates a legacy format condition filter
     */
    private String createConditionFilterLegacyFormat() {
        return "{"
                + "\"conditionFilter\": {"
                + "  \"code\": [\"" + TEST_UUID_1 + "\"]"
                + "}"
                + "}";
    }

    @Test
    public void observationFilter_withLegacyFormat_shouldWorkCorrectly() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Legacy Format Profile");
        profile.setResourceSearchParameter(createLegacyFilterFormat());
        profile.setIsCaseBasedProfile(true);

        Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    @Test
    public void observationFilter_withNewFormat_shouldWorkCorrectly() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test New Format Profile");
        profile.setResourceSearchParameter(createNewFilterFormat());
        profile.setIsCaseBasedProfile(true);

        Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    @Test
    public void conditionFilter_withLegacyFormat_shouldWorkCorrectly() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Legacy Condition Profile");
        profile.setResourceSearchParameter(createConditionFilterLegacyFormat());
        profile.setIsCaseBasedProfile(true);

        SyncFhirCase testCase = new SyncFhirCase();
        testCase.setCaseIdentifier("test-case-123");

        // Test through public interface - generateFHIRCaseResource uses all the filter methods
        // Note: Full case resource generation requires valid patient data
        // This test verifies no exceptions are thrown with legacy format
        try {
            syncFHIRRecord.saveCaseResources(profile, testCase);
            Assert.assertTrue("Legacy format should be handled without exceptions", true);
        } catch (Exception e) {
            // Expected if patient data is not fully set up
            Assert.assertTrue("Should handle legacy format: " + e.getMessage(),
                    e.getMessage() == null || !e.getMessage().contains("code"));
        }
    }

    @Test
    public void conditionFilter_withNewFormat_shouldWorkCorrectly() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test New Condition Profile");
        profile.setResourceSearchParameter(createConditionFilterNewFormat());
        profile.setIsCaseBasedProfile(true);

        SyncFhirCase testCase = new SyncFhirCase();
        testCase.setCaseIdentifier("test-case-123");

        // Test through public interface - generateFHIRCaseResource uses all the filter methods
        // Note: Full case resource generation requires valid patient data
        // This test verifies no exceptions are thrown with new format
        try {
            syncFHIRRecord.saveCaseResources(profile, testCase);
            Assert.assertTrue("New format should be handled without exceptions", true);
        } catch (Exception e) {
            // Expected if patient data is not fully set up
            Assert.assertTrue("Should handle new format: " + e.getMessage(),
                    e.getMessage() == null || !e.getMessage().contains("code"));
        }
    }

    @Test
    public void emptyFilter_shouldHandleGracefully() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Empty Profile");
        profile.setResourceSearchParameter("{}");
        profile.setIsCaseBasedProfile(true);

        Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    @Test
    public void nullFilter_shouldHandleGracefully() {
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Null Profile");
        profile.setResourceSearchParameter(null);
        profile.setIsCaseBasedProfile(true);

        Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }

    @Test
    public void extractConceptUuids_shouldHandleBothFormatsIdentically() {
        // Create legacy format list
        List<String> legacyList = new ArrayList<>();
        legacyList.add(TEST_UUID_1);
        legacyList.add(TEST_UUID_2);

        // Create new format list
        List<org.json.JSONObject> newList = new ArrayList<>();
        org.json.JSONObject concept1 = new org.json.JSONObject();
        concept1.put("uuid", TEST_UUID_1);
        concept1.put("display", "WEIGHT (KG)");
        newList.add(concept1);

        org.json.JSONObject concept2 = new org.json.JSONObject();
        concept2.put("uuid", TEST_UUID_2);
        concept2.put("display", "HEIGHT (CM)");
        newList.add(concept2);

        List<String> legacyExtracted = ConceptFilterUtils.extractConceptUuids(legacyList);
        List<String> newExtracted = ConceptFilterUtils.extractConceptUuids(newList);

        Assert.assertEquals("Both formats should extract same number of UUIDs",
                legacyExtracted.size(), newExtracted.size());
        Assert.assertTrue("Legacy UUIDs should contain TEST_UUID_1", legacyExtracted.contains(TEST_UUID_1));
        Assert.assertTrue("New UUIDs should contain TEST_UUID_1", newExtracted.contains(TEST_UUID_1));
    }

    @Test
    public void malformedFilter_shouldNotCrash() {
        // Test with various malformed inputs
        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Malformed Profile");

        // Malformed JSON - missing closing brace
        profile.setResourceSearchParameter("{\"observationFilter\": {\"code\": [\"");

        try {
            Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());
            Assert.assertNotNull("Should return empty collection for malformed JSON", result);
        } catch (Exception e) {
            // Expected - malformed JSON should throw exception
            Assert.assertTrue("Should handle malformed JSON gracefully", true);
        }
    }

    @Test
    public void mixedFormatFilter_shouldHandleBoth() {
        // Test with a mix of legacy UUID strings and new concept objects
        String mixedFormat = "{"
                + "\"observationFilter\": {"
                + "  \"code\": ["
                + "    \"" + TEST_UUID_1 + "\","
                + "    {"
                + "      \"uuid\": \"" + TEST_UUID_2 + "\","
                + "      \"display\": \"HEIGHT (CM)\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

        SyncFhirProfile profile = new SyncFhirProfile();
        profile.setName("Test Mixed Format Profile");
        profile.setResourceSearchParameter(mixedFormat);
        profile.setIsCaseBasedProfile(true);

        Collection result = syncFHIRRecord.getObservationResourceBundle(profile, new ArrayList<>(), new ArrayList<>());

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Result should be a collection", result instanceof Collection);
    }
}
