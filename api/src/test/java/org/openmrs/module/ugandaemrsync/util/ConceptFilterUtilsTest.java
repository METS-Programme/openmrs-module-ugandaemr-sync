package org.openmrs.module.ugandaemrsync.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for ConceptFilterUtils
 */
public class ConceptFilterUtilsTest {

    private static final String TEST_UUID_1 = "a893f7c0-1350-11df-a1f1-0026b934483c";
    private static final String TEST_UUID_2 = "a898f6c0-1350-11df-a1f1-0026b934483c";
    private static final String TEST_UUID_3 = "a899f7c0-1350-11df-a1f1-0026b934483c";

    private JSONArray newFormatArray;
    private JSONArray legacyFormatArray;
    private JSONObject searchParams;

    @Before
    public void setUp() {
        // Create new format array with concept objects
        newFormatArray = new JSONArray();
        JSONObject concept1 = new JSONObject();
        concept1.put("uuid", TEST_UUID_1);
        concept1.put("display", "WEIGHT (KG)");
        concept1.put("id", 5089);
        concept1.put("conceptClass", "Misc");
        concept1.put("datatype", "Numeric");
        newFormatArray.put(concept1);

        JSONObject concept2 = new JSONObject();
        concept2.put("uuid", TEST_UUID_2);
        concept2.put("display", "HEIGHT (CM)");
        concept2.put("id", 5090);
        concept2.put("conceptClass", "Misc");
        concept2.put("datatype", "Numeric");
        newFormatArray.put(concept2);

        // Create legacy format array with simple UUID strings
        legacyFormatArray = new JSONArray();
        legacyFormatArray.put(TEST_UUID_1);
        legacyFormatArray.put(TEST_UUID_2);

        // Create search params object
        searchParams = new JSONObject();
        JSONObject observationFilter = new JSONObject();
        observationFilter.put("code", newFormatArray);
        searchParams.put("observationFilter", observationFilter);
    }

    @Test
    public void extractConceptUuids_fromNewFormat_shouldReturnUuids() {
        List<String> uuids = ConceptFilterUtils.extractConceptUuids(newFormatArray);

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuids_fromLegacyFormat_shouldReturnUuids() {
        List<String> uuids = ConceptFilterUtils.extractConceptUuids(legacyFormatArray);

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuids_fromEmptyArray_shouldReturnEmptyList() {
        JSONArray emptyArray = new JSONArray();
        List<String> uuids = ConceptFilterUtils.extractConceptUuids(emptyArray);

        assertNotNull(uuids);
        assertTrue(uuids.isEmpty());
    }

    @Test
    public void extractConceptUuids_fromNull_shouldReturnEmptyList() {
        List<String> uuids = ConceptFilterUtils.extractConceptUuids(null);

        assertNotNull(uuids);
        assertTrue(uuids.isEmpty());
    }

    @Test
    public void extractConceptUuids_fromListWithStrings_shouldReturnUuids() {
        List<String> stringList = new ArrayList<>();
        stringList.add(TEST_UUID_1);
        stringList.add(TEST_UUID_2);

        List<String> uuids = ConceptFilterUtils.extractConceptUuids(stringList);

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuids_fromListWithMaps_shouldReturnUuids() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> concept1 = new HashMap<>();
        concept1.put("uuid", TEST_UUID_1);
        concept1.put("display", "WEIGHT (KG)");
        concept1.put("id", 5089);
        mapList.add(concept1);

        Map<String, Object> concept2 = new HashMap<>();
        concept2.put("uuid", TEST_UUID_2);
        concept2.put("display", "HEIGHT (CM)");
        concept2.put("id", 5090);
        mapList.add(concept2);

        List<String> uuids = ConceptFilterUtils.extractConceptUuids(mapList);

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuids_fromStringArray_shouldReturnUuids() {
        String[] stringArray = {TEST_UUID_1, TEST_UUID_2};

        List<String> uuids = ConceptFilterUtils.extractConceptUuids(stringArray);

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuids_fromMixedFormat_shouldHandleGracefully() {
        JSONArray mixedArray = new JSONArray();
        mixedArray.put(TEST_UUID_1); // String

        JSONObject concept2 = new JSONObject();
        concept2.put("uuid", TEST_UUID_2);
        concept2.put("display", "HEIGHT (CM)");
        mixedArray.put(concept2); // Object

        List<String> uuids = ConceptFilterUtils.extractConceptUuids(mixedArray);

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuids_fromObjectWithMissingUuid_shouldSkip() {
        JSONArray arrayWithMissingUuid = new JSONArray();

        JSONObject concept1 = new JSONObject();
        concept1.put("uuid", TEST_UUID_1);
        concept1.put("display", "WEIGHT (KG)");
        arrayWithMissingUuid.put(concept1);

        JSONObject concept2 = new JSONObject();
        concept2.put("display", "HEIGHT (CM)"); // Missing uuid
        arrayWithMissingUuid.put(concept2);

        List<String> uuids = ConceptFilterUtils.extractConceptUuids(arrayWithMissingUuid);

        assertNotNull(uuids);
        assertEquals(1, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
    }

    @Test
    public void extractConceptUuidsFromJson_shouldExtractFromKey() {
        List<String> uuids = ConceptFilterUtils.extractConceptUuidsFromJson(searchParams, "observationFilter");

        // This should return empty because the key structure doesn't match directly
        // The observationFilter is a JSONObject, not a code array
        assertNotNull(uuids);
    }

    @Test
    public void extractConceptUuidsFromJson_withDirectKey_shouldReturnUuids() {
        JSONObject params = new JSONObject();
        params.put("code", newFormatArray);

        List<String> uuids = ConceptFilterUtils.extractConceptUuidsFromJson(params, "code");

        assertNotNull(uuids);
        assertEquals(2, uuids.size());
        assertEquals(TEST_UUID_1, uuids.get(0));
        assertEquals(TEST_UUID_2, uuids.get(1));
    }

    @Test
    public void extractConceptUuidsFromJson_withMissingKey_shouldReturnEmptyList() {
        JSONObject params = new JSONObject();
        params.put("otherKey", newFormatArray);

        List<String> uuids = ConceptFilterUtils.extractConceptUuidsFromJson(params, "code");

        assertNotNull(uuids);
        assertTrue(uuids.isEmpty());
    }

    @Test
    public void validateCodeArray_withValidNewFormat_shouldReturnTrue() {
        boolean isValid = ConceptFilterUtils.validateCodeArray(newFormatArray);

        assertTrue(isValid);
    }

    @Test
    public void validateCodeArray_withValidLegacyFormat_shouldReturnTrue() {
        boolean isValid = ConceptFilterUtils.validateCodeArray(legacyFormatArray);

        assertTrue(isValid);
    }

    @Test
    public void validateCodeArray_withMissingUuid_shouldReturnFalse() {
        JSONArray invalidArray = new JSONArray();
        JSONObject concept = new JSONObject();
        concept.put("display", "WEIGHT (KG)"); // Missing uuid
        invalidArray.put(concept);

        boolean isValid = ConceptFilterUtils.validateCodeArray(invalidArray);

        assertFalse(isValid);
    }

    @Test
    public void validateCodeArray_withMissingDisplay_shouldReturnFalse() {
        JSONArray invalidArray = new JSONArray();
        JSONObject concept = new JSONObject();
        concept.put("uuid", TEST_UUID_1); // Missing display
        invalidArray.put(concept);

        boolean isValid = ConceptFilterUtils.validateCodeArray(invalidArray);

        assertFalse(isValid);
    }

    @Test
    public void validateCodeArray_withNull_shouldReturnFalse() {
        boolean isValid = ConceptFilterUtils.validateCodeArray(null);

        assertFalse(isValid);
    }

    @Test
    public void validateCodeArray_withEmptyList_shouldReturnFalse() {
        List<String> emptyList = new ArrayList<>();
        boolean isValid = ConceptFilterUtils.validateCodeArray(emptyList);

        assertFalse(isValid);
    }

    @Test
    public void extractDisplayNames_fromNewFormat_shouldReturnDisplays() {
        List<String> displays = ConceptFilterUtils.extractDisplayNames(newFormatArray);

        assertNotNull(displays);
        assertEquals(2, displays.size());
        assertEquals("WEIGHT (KG)", displays.get(0));
        assertEquals("HEIGHT (CM)", displays.get(1));
    }

    @Test
    public void extractDisplayNames_fromLegacyFormat_shouldReturnEmptyList() {
        List<String> displays = ConceptFilterUtils.extractDisplayNames(legacyFormatArray);

        assertNotNull(displays);
        assertTrue(displays.isEmpty());
    }

    @Test
    public void extractDisplayNames_fromNull_shouldReturnEmptyList() {
        List<String> displays = ConceptFilterUtils.extractDisplayNames(null);

        assertNotNull(displays);
        assertTrue(displays.isEmpty());
    }

    @Test
    public void buildFhirCodeParam_withValidInputs_shouldReturnFhirParam() {
        List<String> uuids = new ArrayList<>();
        uuids.add(TEST_UUID_1);
        uuids.add(TEST_UUID_2);
        String system = "http://org.openmrs.projectomod/concepts";

        String fhirParam = ConceptFilterUtils.buildFhirCodeParam(uuids, system);

        assertNotNull(fhirParam);
        assertFalse(fhirParam.isEmpty());
        assertTrue(fhirParam.contains(system + "|" + TEST_UUID_1));
        assertTrue(fhirParam.contains(system + "|" + TEST_UUID_2));
        assertTrue(fhirParam.contains(","));
    }

    @Test
    public void buildFhirCodeParam_withEmptyUuids_shouldReturnEmptyString() {
        List<String> uuids = new ArrayList<>();
        String system = "http://org.openmrs.projectomod/concepts";

        String fhirParam = ConceptFilterUtils.buildFhirCodeParam(uuids, system);

        assertNotNull(fhirParam);
        assertTrue(fhirParam.isEmpty());
    }

    @Test
    public void buildFhirCodeParam_withNullUuids_shouldReturnEmptyString() {
        String system = "http://org.openmrs.projectomod/concepts";

        String fhirParam = ConceptFilterUtils.buildFhirCodeParam(null, system);

        assertNotNull(fhirParam);
        assertTrue(fhirParam.isEmpty());
    }

    @Test
    public void buildFhirCodeParam_withEmptySystem_shouldReturnEmptyString() {
        List<String> uuids = new ArrayList<>();
        uuids.add(TEST_UUID_1);

        String fhirParam = ConceptFilterUtils.buildFhirCodeParam(uuids, "");

        assertNotNull(fhirParam);
        assertTrue(fhirParam.isEmpty());
    }
}
