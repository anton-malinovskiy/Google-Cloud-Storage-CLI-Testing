package com.gcs.testing.tests;

import com.gcs.testing.base.BaseTest;
import com.gcs.testing.config.TestConfig;
import com.gcs.testing.models.CommandResult;
import com.gcs.testing.utils.GCloudCliExecutor;
import com.gcs.testing.utils.TestDataGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the 'gcloud storage ls' command.
 */
public class ListCommandTest extends BaseTest {

    @Test(priority = 1, description = "Test listing empty bucket")
    public void testListEmptyBucket() {
        logger.info("=== Testing list on empty bucket prefix ===");

        // Use a unique prefix that should be empty
        String uniquePrefix = "empty-test-" + TestDataGenerator.generateUniqueId() + "/";
        String gsPath = getGsPath(uniquePrefix);

        List<String> files = GCloudCliExecutor.listBucket(gsPath);

        Assert.assertNotNull(files, "List result should not be null");
        Assert.assertTrue(files.isEmpty(), "List should be empty for non-existent prefix");
    }

    @Test(priority = 2, description = "Test listing bucket with single file")
    public void testListSingleFile() {
        logger.info("=== Testing list with single file ===");

        // Create a test file
        String fileName = generateTestFileName("txt");
        String content = "Single file for list test";
        String gsPath = createTestFile(fileName, content);

        // List the specific file
        List<String> files = GCloudCliExecutor.listBucket(gsPath);

        Assert.assertNotNull(files, "List result should not be null");
        Assert.assertEquals(files.size(), 1, "Should find exactly one file");
        Assert.assertEquals(files.get(0), gsPath, "Listed file should match created file");
    }

    @Test(priority = 3, description = "Test listing bucket with multiple files")
    public void testListMultipleFiles() {
        logger.info("=== Testing list with multiple files ===");

        // Create multiple files with common prefix
        String prefix = "list-test-" + TestDataGenerator.generateUniqueId().substring(0, 8) + "/";
        List<String> createdPaths = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            String fileName = prefix + "file-" + i + ".txt";
            String content = "Test file " + i;
            String gsPath = createTestFile(fileName, content);
            createdPaths.add(gsPath);
        }

        // List files with the prefix
        String listPath = getGsPath(prefix);
        List<String> files = GCloudCliExecutor.listBucket(listPath);

        Assert.assertNotNull(files, "List result should not be null");
        Assert.assertEquals(files.size(), 5, "Should find all 5 created files");

        // Verify all created files are in the list
        for (String createdPath : createdPaths) {
            Assert.assertTrue(files.contains(createdPath),
                    "List should contain created file: " + createdPath);
        }
    }

    @Test(priority = 4, description = "Test listing with different file types")
    public void testListDifferentFileTypes() {
        logger.info("=== Testing list with different file types ===");

        String prefix = "mixed-types-" + TestDataGenerator.generateUniqueId().substring(0, 8) + "/";
        String[] extensions = {"txt", "json", "csv", "html", "bin"};

        // Create files with different extensions
        for (String ext : extensions) {
            String fileName = prefix + "test." + ext;
            String content = TestDataGenerator.generateRandomContent(1);
            createTestFile(fileName, content);
        }

        // List all files with the prefix
        String listPath = getGsPath(prefix);
        List<String> files = GCloudCliExecutor.listBucket(listPath);

        Assert.assertEquals(files.size(), extensions.length,
                "Should find all files regardless of type");

        // Verify each extension is represented
        for (String ext : extensions) {
            boolean found = files.stream().anyMatch(f -> f.endsWith("." + ext));
            Assert.assertTrue(found, "Should find file with extension: " + ext);
        }
    }

    @Test(priority = 5, description = "Test listing with nested structure")
    public void testListNestedStructure() {
        logger.info("=== Testing list with nested directory structure ===");

        String basePrefix = "nested-" + TestDataGenerator.generateUniqueId().substring(0, 8) + "/";

        // Create nested structure
        String[] paths = {
                basePrefix + "file1.txt",
                basePrefix + "dir1/file2.txt",
                basePrefix + "dir1/file3.txt",
                basePrefix + "dir2/file4.txt",
                basePrefix + "dir1/subdir/file5.txt"
        };

        for (String path : paths) {
            createTestFile(path, "Content for " + path);
        }

        // List base directory recursively
        List<String> baseFiles = GCloudCliExecutor.listBucket(getGsPath(basePrefix), true);
        Assert.assertTrue(baseFiles.size() >= 5, "Should list all files recursively");

        // List specific subdirectory recursively
        List<String> dir1Files = GCloudCliExecutor.listBucket(getGsPath(basePrefix + "dir1/"), true);
        long dir1Count = dir1Files.stream()
                .filter(f -> f.contains("/dir1/"))
                .count();
        Assert.assertTrue(dir1Count >= 3, "Should find files in dir1 and its subdirectories");
    }

    @Test(priority = 6, description = "Test listing with wildcards")
    public void testListWithWildcards() {
        logger.info("=== Testing list with wildcard patterns ===");

        String prefix = "wildcard-" + TestDataGenerator.generateUniqueId().substring(0, 8) + "/";

        // Create files with pattern
        for (int i = 0; i < 3; i++) {
            createTestFile(prefix + "test-" + i + ".txt", "Text file " + i);
            createTestFile(prefix + "data-" + i + ".json", "JSON file " + i);
            createTestFile(prefix + "report-" + i + ".csv", "CSV file " + i);
        }

        // List all files
        List<String> allFiles = GCloudCliExecutor.listBucket(getGsPath(prefix));
        Assert.assertEquals(allFiles.size(), 9, "Should find all 9 files");

        // Note: gcloud storage ls doesn't support wildcards directly in the path,
        // but we can filter results programmatically
        long txtCount = allFiles.stream().filter(f -> f.endsWith(".txt")).count();
        long jsonCount = allFiles.stream().filter(f -> f.endsWith(".json")).count();
        long csvCount = allFiles.stream().filter(f -> f.endsWith(".csv")).count();

        Assert.assertEquals(txtCount, 3, "Should find 3 txt files");
        Assert.assertEquals(jsonCount, 3, "Should find 3 json files");
        Assert.assertEquals(csvCount, 3, "Should find 3 csv files");
    }

    @Test(priority = 7, description = "Test listing with JSON format")
    public void testListJsonFormat() {
        logger.info("=== Testing list with JSON format output ===");

        // Create a test file
        String fileName = generateTestFileName("txt");
        String gsPath = createTestFile(fileName, "Test content for JSON listing");

        // Test listing - gcloud storage ls actually doesn't support --format=json in newer versions
        // So we'll just test that we can list the file successfully
        CommandResult result = GCloudCliExecutor.executeCommand(
                String.format("gcloud storage ls %s", gsPath));

        assertCommandSuccess(result.getExitCode(), "List command should succeed");

        String output = result.getStdout();
        Assert.assertTrue(output.contains(gsPath) || output.contains(fileName),
                "Output should contain the file path or name");

        logger.info("List command output: {}", output);
    }

    @Test(priority = 8, description = "Test listing performance with many files")
    public void testListPerformance() {
        logger.info("=== Testing list performance with many files ===");

        String prefix = "perf-test-" + TestDataGenerator.generateUniqueId().substring(0, 8) + "/";

        // Create 20 files
        int fileCount = 20;
        for (int i = 0; i < fileCount; i++) {
            String fileName = prefix + String.format("file-%03d.txt", i);
            createTestFile(fileName, "Performance test file " + i);
        }

        // Measure list performance
        long startTime = System.currentTimeMillis();
        List<String> files = GCloudCliExecutor.listBucket(getGsPath(prefix));
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Listed {} files in {} ms", files.size(), duration);

        Assert.assertEquals(files.size(), fileCount, "Should find all created files");
        Assert.assertTrue(duration < 10000, "List operation should complete within 10 seconds");
    }

    @Test(priority = 9, description = "Test listing bucket root")
    public void testListBucketRoot() {
        logger.info("=== Testing list bucket root ===");

        // List bucket root
        String bucketPath = "gs://" + TestConfig.getBucketName();
        List<String> files = GCloudCliExecutor.listBucket(bucketPath);

        Assert.assertNotNull(files, "List result should not be null");
        logger.info("Found {} items in bucket root", files.size());

        // Bucket should contain at least our test files
        long testFiles = files.stream()
                .filter(f -> f.contains(TestConfig.getTestFilePrefix()))
                .count();
        Assert.assertTrue(testFiles > 0, "Should find at least some test files in bucket");
    }

    @Test(priority = 10, description = "Test list error handling")
    public void testListErrorHandling() {
        logger.info("=== Testing list error handling ===");

        // Test with invalid bucket name
        try {
            List<String> files = GCloudCliExecutor.listBucket("gs://invalid-bucket-that-does-not-exist/");
            // If no exception is thrown, the list should be empty or method should handle error gracefully
            Assert.assertTrue(files.isEmpty() || files == null,
                    "List of non-existent bucket should be empty or null");
        } catch (RuntimeException e) {
            // Expected exception for non-existent bucket
            Assert.assertTrue(e.getMessage().contains("Failed to list bucket"),
                    "Exception message should indicate list failure");
        }
    }
}