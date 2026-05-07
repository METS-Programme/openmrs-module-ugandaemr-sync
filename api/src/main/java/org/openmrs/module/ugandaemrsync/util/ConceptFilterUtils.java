package org.openmrs.module.ugandaemrsync.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for handling concept filters in FHIR sync profiles.
 * Supports both legacy format (array of UUID strings) and new format (array of concept objects).
 *
 * Legacy format:
 * <code>
 * {
 *   "observationFilter": {
 *     "code": ["uuid1", "uuid2"]
 *   }
 * }
 * </code>
 *
 * New format:
 * <code>
 * {
 *   "observationFilter": {
 *     "code": [
 *       {
 *         "uuid": "a893f7c0-1350-11df-a1f1-0026b934483c",
 *         "display": "WEIGHT (KG)",
 *         "id": 5089,
 *         "conceptClass": "Misc",
 *         "datatype": "Numeric"
 *       }
 *     ]
 *   }
 * }
 * </code>
 */
public class ConceptFilterUtils {

    /**
     * Extracts concept UUIDs from a code array that may be in either legacy or new format.
     *
     * @param codeValue the code array value (JSONArray, List<String>, or String[])
     * @return list of concept UUIDs
     */
    public static List<String> extractConceptUuids(Object codeValue) {
        List<String> uuids = new ArrayList<>();

        if (codeValue == null) {
            return uuids;
        }

        if (codeValue instanceof JSONArray) {
            JSONArray codeArray = (JSONArray) codeValue;
            for (int i = 0; i < codeArray.length(); i++) {
                Object item = codeArray.opt(i);
                if (item == null) {
                    continue;
                }
                if (item instanceof String) {
                    String uuid = (String) item;
                    if (!uuid.isEmpty()) {
                        uuids.add(uuid);
                    }
                } else if (item instanceof JSONObject) {
                    String uuid = ((JSONObject) item).optString("uuid");
                    if (uuid != null && !uuid.isEmpty()) {
                        uuids.add(uuid);
                    }
                }
            }
        } else if (codeValue instanceof List) {
            List<?> codeList = (List<?>) codeValue;
            if (!codeList.isEmpty()) {
                Object firstItem = codeList.get(0);
                if (firstItem instanceof String) {
                    for (Object item : codeList) {
                        if (item instanceof String) {
                            String uuid = (String) item;
                            if (!uuid.isEmpty()) {
                                uuids.add(uuid);
                            }
                        }
                    }
                } else if (firstItem instanceof JSONObject) {
                    for (Object item : codeList) {
                        if (item instanceof JSONObject) {
                            String uuid = ((JSONObject) item).optString("uuid");
                            if (uuid != null && !uuid.isEmpty()) {
                                uuids.add(uuid);
                            }
                        }
                    }
                } else if (firstItem instanceof Map) {
                    for (Object item : codeList) {
                        if (item instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) item;
                            Object uuid = map.get("uuid");
                            if (uuid instanceof String && !((String) uuid).isEmpty()) {
                                uuids.add((String) uuid);
                            }
                        }
                    }
                }
            }
        } else if (codeValue.getClass().isArray()) {
            Object[] codeArray = (Object[]) codeValue;
            for (Object item : codeArray) {
                if (item instanceof String) {
                    String uuid = (String) item;
                    if (!uuid.isEmpty()) {
                        uuids.add(uuid);
                    }
                }
            }
        }

        return uuids;
    }

    /**
     * Extracts concept UUIDs from a JSONObject containing a code array.
     *
     * @param searchParams the search parameters JSONObject
     * @param codeKey the key for the code array (e.g., "code")
     * @return list of concept UUIDs
     */
    public static List<String> extractConceptUuidsFromJson(JSONObject searchParams, String codeKey) {
        if (searchParams == null || !searchParams.has(codeKey)) {
            return new ArrayList<>();
        }
        Object codeValue = searchParams.get(codeKey);
        return extractConceptUuids(codeValue);
    }

    /**
     * Validates that a code array has the required structure.
     *
     * @param codeValue the code array value to validate
     * @return true if valid, false otherwise
     */
    public static boolean validateCodeArray(Object codeValue) {
        if (codeValue == null) {
            return false;
        }

        if (codeValue instanceof JSONArray) {
            JSONArray codeList = (JSONArray) codeValue;
            for (int i = 0; i < codeList.length(); i++) {
                Object item = codeList.opt(i);
                if (item == null) {
                    return false;
                }
                if (item instanceof JSONObject) {
                    JSONObject conceptRef = (JSONObject) item;
                    if (conceptRef.opt("uuid") == null || conceptRef.opt("display") == null) {
                        return false;
                    }
                } else if (!(item instanceof String)) {
                    return false;
                }
            }
            return true;
        } else if (codeValue instanceof List) {
            List<?> codeList = (List<?>) codeValue;
            for (Object item : codeList) {
                if (item == null) {
                    return false;
                }
                if (item instanceof JSONObject) {
                    JSONObject conceptRef = (JSONObject) item;
                    if (conceptRef.opt("uuid") == null || conceptRef.opt("display") == null) {
                        return false;
                    }
                } else if (!(item instanceof String)) {
                    return false;
                }
            }
            return !codeList.isEmpty();
        }

        return false;
    }

    /**
     * Loads Concept objects from a list of UUIDs.
     *
     * @param uuids list of concept UUIDs
     * @return list of Concept objects (skips invalid UUIDs)
     */
    public static List<Concept> loadConceptsFromUuids(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return new ArrayList<>();
        }

        return uuids.stream()
                .map(uuid -> {
                    try {
                        return Context.getConceptService().getConcept(uuid);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(concept -> concept != null)
                .collect(Collectors.toList());
    }

    /**
     * Extracts display names from a code array in the new format.
     *
     * @param codeValue the code array value
     * @return list of display names (empty for legacy format or missing displays)
     */
    public static List<String> extractDisplayNames(Object codeValue) {
        List<String> displays = new ArrayList<>();

        if (codeValue == null) {
            return displays;
        }

        if (codeValue instanceof JSONArray) {
            JSONArray codeArray = (JSONArray) codeValue;
            for (int i = 0; i < codeArray.length(); i++) {
                Object item = codeArray.opt(i);
                if (item instanceof JSONObject) {
                    String display = ((JSONObject) item).optString("display");
                    if (display != null && !display.isEmpty()) {
                        displays.add(display);
                    }
                }
            }
        } else if (codeValue instanceof List) {
            List<?> codeList = (List<?>) codeValue;
            for (Object item : codeList) {
                if (item instanceof JSONObject) {
                    String display = ((JSONObject) item).optString("display");
                    if (display != null && !display.isEmpty()) {
                        displays.add(display);
                    }
                } else if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    Object display = map.get("display");
                    if (display instanceof String && !((String) display).isEmpty()) {
                        displays.add((String) display);
                    }
                }
            }
        }

        return displays;
    }

    /**
     * Builds a FHIR code query parameter from a list of UUIDs.
     * FHIR format: ?code=system|uuid1,system|uuid2
     *
     * @param uuids list of concept UUIDs
     * @param system the concept system URL
     * @return FHIR code parameter value
     */
    public static String buildFhirCodeParam(List<String> uuids, String system) {
        if (uuids == null || uuids.isEmpty() || system == null || system.isEmpty()) {
            return "";
        }

        return uuids.stream()
                .map(uuid -> system + "|" + uuid)
                .collect(Collectors.joining(","));
    }
}
