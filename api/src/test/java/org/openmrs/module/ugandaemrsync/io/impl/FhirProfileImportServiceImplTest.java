package org.openmrs.module.ugandaemrsync.io.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test suite for FhirProfileImportServiceImpl
 * Focuses on recursive directory discovery functionality for content pack support
 */
public class FhirProfileImportServiceImplTest {

    private FhirProfileImportServiceImpl importService;
    private Path tempTestDir;
    private Path baseConfigDir;

    @Before
    public void setUp() throws IOException {
        importService = new FhirProfileImportServiceImpl();

        // Create temporary directory structure for testing
        tempTestDir = Files.createTempDirectory("ugandaemrsync-test");
        baseConfigDir = tempTestDir.resolve("configuration").resolve("hie");
        Files.createDirectories(baseConfigDir);
    }

    @After
    public void tearDown() throws IOException {
        // Clean up temporary directory
        if (tempTestDir != null && Files.exists(tempTestDir)) {
            deleteDirectory(tempTestDir.toFile());
        }
    }

    /**
     * Test: findDirectoriesByName finds directories at root level
     */
    @Test
    public void testFindDirectoriesByName_RootLevel() throws IOException {
        // Arrange: Create syncprofile at root level
        Path syncprofileDir = baseConfigDir.resolve("syncprofile");
        Files.createDirectories(syncprofileDir);

        // Act: Use reflection to call private method
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).endsWith("syncprofile"));
    }

    /**
     * Test: findDirectoriesByName finds directories at multiple nesting levels
     */
    @Test
    public void testFindDirectoriesByName_MultipleNestingLevels() throws IOException {
        // Arrange: Create nested structure
        // configuration/hie/syncprofile/ (root)
        Path rootSyncprofile = baseConfigDir.resolve("syncprofile");
        Files.createDirectories(rootSyncprofile);

        // configuration/hie/contentpack1/syncprofile/ (level 1)
        Path contentpack1 = baseConfigDir.resolve("contentpack1");
        Path syncprofile1 = contentpack1.resolve("syncprofile");
        Files.createDirectories(syncprofile1);

        // configuration/hie/contentpack2/nested/syncprofile/ (level 2)
        Path contentpack2 = baseConfigDir.resolve("contentpack2");
        Path nested = contentpack2.resolve("nested");
        Path syncprofile2 = nested.resolve("syncprofile");
        Files.createDirectories(syncprofile2);

        // Act
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert: Should find all 3 directories
        assertEquals(3, result.size());
        assertTrue(result.contains(rootSyncprofile));
        assertTrue(result.contains(syncprofile1));
        assertTrue(result.contains(syncprofile2));
    }

    /**
     * Test: findDirectoriesByName respects maxDepth limit
     */
    @Test
    public void testFindDirectoriesByName_RespectsMaxDepth() throws IOException {
        // Arrange: Create nested structure at depth 4
        Path level1 = baseConfigDir.resolve("level1");
        Path level2 = level1.resolve("level2");
        Path level3 = level2.resolve("level3");
        Path level4 = level3.resolve("level4");
        Path syncprofileDir = level4.resolve("syncprofile");
        Files.createDirectories(syncprofileDir);

        // Act: Search with maxDepth=3
        List<Path> resultLimited = callFindDirectoriesByName(baseConfigDir, "syncprofile", 3);

        // Assert: Should not find the directory at depth 4
        assertEquals(0, resultLimited.size());

        // Act: Search with maxDepth=5
        List<Path> resultUnlimited = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert: Should find the directory at depth 4
        assertEquals(1, resultUnlimited.size());
    }

    /**
     * Test: findDirectoriesByName finds both syncprofile and synctasktype
     */
    @Test
    public void testFindDirectoriesByName_FindsBothFolderTypes() throws IOException {
        // Arrange: Create both folder types
        Path syncprofileDir = baseConfigDir.resolve("syncprofile");
        Files.createDirectories(syncprofileDir);

        Path synctasktypeDir = baseConfigDir.resolve("synctasktype");
        Files.createDirectories(synctasktypeDir);

        // Act & Assert
        List<Path> profileResults = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);
        List<Path> taskResults = callFindDirectoriesByName(baseConfigDir, "synctasktype", 5);

        assertEquals(1, profileResults.size());
        assertEquals(1, taskResults.size());
        assertTrue(profileResults.get(0).endsWith("syncprofile"));
        assertTrue(taskResults.get(0).endsWith("synctasktype"));
    }

    /**
     * Test: findDirectoriesByName returns empty list when no matches
     */
    @Test
    public void testFindDirectoriesByName_NoMatches() throws IOException {
        // Arrange: Create directory that doesn't match
        Path otherDir = baseConfigDir.resolve("otherfolder");
        Files.createDirectories(otherDir);

        // Act
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert
        assertTrue(result.isEmpty());
    }

    /**
     * Test: findDirectoriesByName finds multiple matches at same level
     */
    @Test
    public void testFindDirectoriesByName_MultipleMatchesSameLevel() throws IOException {
        // Arrange: Create multiple content packs with syncprofile
        Path pack1 = baseConfigDir.resolve("contentpack1").resolve("syncprofile");
        Files.createDirectories(pack1);

        Path pack2 = baseConfigDir.resolve("contentpack2").resolve("syncprofile");
        Files.createDirectories(pack2);

        Path pack3 = baseConfigDir.resolve("contentpack3").resolve("syncprofile");
        Files.createDirectories(pack3);

        // Act
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert: Should find all 3
        assertEquals(3, result.size());
    }

    /**
     * Test: findDirectoriesByName handles non-existent base directory
     */
    @Test
    public void testFindDirectoriesByName_NonExistentBaseDir() throws IOException {
        // Arrange: Use a non-existent path
        Path nonExistent = Paths.get("/tmp/non-existent-path-" + System.currentTimeMillis());

        // Act
        List<Path> result = callFindDirectoriesByName(nonExistent, "syncprofile", 5);

        // Assert: Should return empty list without throwing exception
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test: findDirectoriesByName handles directory name that appears at multiple levels
     */
    @Test
    public void testFindDirectoriesByName_NameAtMultipleLevels() throws IOException {
        // Arrange: Create structure where folder name appears at multiple levels
        // configuration/hie/syncprofile/
        // configuration/hie/syncprofile/syncprofile/ (nested with same name)
        Path rootSyncprofile = baseConfigDir.resolve("syncprofile");
        Files.createDirectories(rootSyncprofile);

        Path nestedSyncprofile = rootSyncprofile.resolve("syncprofile");
        Files.createDirectories(nestedSyncprofile);

        // Act
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert: Should find both (the parent and the nested one)
        assertEquals(2, result.size());
        assertTrue(result.contains(rootSyncprofile));
        assertTrue(result.contains(nestedSyncprofile));
    }

    /**
     * Test: findDirectoriesByName handles complex nested content pack structure
     */
    @Test
    public void testFindDirectoriesByName_ComplexContentPackStructure() throws IOException {
        // Arrange: Simulate real content pack structure
        // configuration/hie/
        // ├── syncprofile/                    (default profiles)
        // ├── synctasktype/                   (default task types)
        // ├── contentpack-africa/
        // │   ├── syncprofile/
        // │   └── synctasktype/
        // ├── contentpack-asia/
        // │   ├── country-specific/
        // │   │   ├── syncprofile/
        // │   │   └── synctasktype/
        // └── legacy/
        //     └── old-version/
        //         └── syncprofile/

        Path defaultSyncprofile = baseConfigDir.resolve("syncprofile");
        Files.createDirectories(defaultSyncprofile);

        Path defaultSynctasktype = baseConfigDir.resolve("synctasktype");
        Files.createDirectories(defaultSynctasktype);

        Path africaPack = baseConfigDir.resolve("contentpack-africa");
        Files.createDirectories(africaPack.resolve("syncprofile"));
        Files.createDirectories(africaPack.resolve("synctasktype"));

        Path asiaPack = baseConfigDir.resolve("contentpack-asia").resolve("country-specific");
        Files.createDirectories(asiaPack.resolve("syncprofile"));
        Files.createDirectories(asiaPack.resolve("synctasktype"));

        Path legacy = baseConfigDir.resolve("legacy").resolve("old-version");
        Files.createDirectories(legacy.resolve("syncprofile"));

        // Act
        List<Path> profileResults = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);
        List<Path> taskResults = callFindDirectoriesByName(baseConfigDir, "synctasktype", 5);

        // Assert
        assertEquals(4, profileResults.size());  // default + africa + asia + legacy
        assertEquals(3, taskResults.size());     // default + africa + asia
    }

    /**
     * Test: findDirectoriesByName doesn't exceed recursion limit
     */
    @Test
    public void testFindDirectoriesByName_DeepNestingWithLimit() throws IOException {
        // Arrange: Create very deep nesting (10 levels)
        Path current = baseConfigDir;
        for (int i = 0; i < 10; i++) {
            current = current.resolve("level" + i);
            Files.createDirectories(current);
        }
        Path syncprofileDir = current.resolve("syncprofile");
        Files.createDirectories(syncprofileDir);

        // Act: Search with maxDepth=5
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert: Should not find directory at depth 10
        assertEquals(0, result.size());

        // Act: Search with maxDepth=15
        List<Path> resultUnlimited = callFindDirectoriesByName(baseConfigDir, "syncprofile", 15);

        // Assert: Should find directory at depth 10
        assertEquals(1, resultUnlimited.size());
    }

    /**
     * Test: findDirectoriesByName doesn't match partial names
     */
    @Test
    public void testFindDirectoriesByName_NoPartialNameMatch() throws IOException {
        // Arrange: Create directories with similar but different names
        Files.createDirectories(baseConfigDir.resolve("syncprofiles"));  // plural
        Files.createDirectories(baseConfigDir.resolve("sync-profile"));  // with hyphen
        Files.createDirectories(baseConfigDir.resolve("syncprofile_old")); // with suffix
        Files.createDirectories(baseConfigDir.resolve("my_syncprofile")); // with prefix

        // Also create the exact match
        Files.createDirectories(baseConfigDir.resolve("syncprofile"));

        // Act
        List<Path> result = callFindDirectoriesByName(baseConfigDir, "syncprofile", 5);

        // Assert: Should only find the exact match
        assertEquals(1, result.size());
        assertTrue(result.get(0).endsWith("syncprofile"));
    }

    /**
     * Helper method to call private findDirectoriesByName method using reflection
     */
    private List<Path> callFindDirectoriesByName(Path baseDir, String folderName, int maxDepth) {
        try {
            java.lang.reflect.Method method = FhirProfileImportServiceImpl.class
                    .getDeclaredMethod("findDirectoriesByName", Path.class, String.class, int.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Path> result = (List<Path>) method.invoke(importService, baseDir, folderName, maxDepth);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call findDirectoriesByName via reflection", e);
        }
    }

    /**
     * Helper method to recursively delete a directory
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
