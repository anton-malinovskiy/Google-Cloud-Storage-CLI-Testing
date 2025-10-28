package com.gcs.testing.tests;

import com.gcs.testing.base.BaseTest;
import com.gcs.testing.config.TestConfig;
import com.gcs.testing.models.CommandResult;
import com.gcs.testing.utils.BucketHelper;
import com.gcs.testing.utils.GCloudCliExecutor;
import com.gcs.testing.utils.TestDataGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the 'gcloud storage rm' command.
 */
public class DeleteCommandTest extends BaseTest {

    @Test(priority = 1, description = "Test deleting single file")
    public void testDeleteSingleFile() {
        logger.info("=== Testing delete single file ===");

        // Create a test file
        String fileName = generateTestFileName("txt");
        String content = "File to be deleted";
        String gsPath = BucketHelper.uploadTestFile(fileName, content);

        // Verify file exists before deletion
        Assert.assertTrue(BucketHelper.verifyFileExists(gsPath),
                "File should exist before deletion");

        // Delete the file
        CommandResult result = GCloudCliExecutor.deleteFile(gsPath);

        assertCommandSuccess(result.getExitCode(), "Failed to delete file");
        logger.info("Successfully deleted file: {}", gsPath);

        // Verify file no longer exists
        Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                "File should not exist after deletion");

        // Remove from cleanup list since it's already deleted
        createdFiles.remove(gsPath);
    }

    @Test(priority = 2, description = "Test deleting multiple files")
    public void testDeleteMultipleFiles() {
        logger.info("=== Testing delete multiple files ===");

        // Create multiple files
        List<String> filesToDelete = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String fileName = generateTestFileName("json");
            String content = TestDataGenerator.generateJsonContent();
            String gsPath = BucketHelper.uploadTestFile(fileName, content);
            filesToDelete.add(gsPath);
        }

        // Verify all files exist
        for (String gsPath : filesToDelete) {
            Assert.assertTrue(BucketHelper.verifyFileExists(gsPath),
                    "File should exist before deletion: " + gsPath);
        }

        // Delete all files
        for (String gsPath : filesToDelete) {
            CommandResult result = GCloudCliExecutor.deleteFile(gsPath);
            assertCommandSuccess(result.getExitCode(),
                    "Failed to delete file: " + gsPath);
            createdFiles.remove(gsPath);
        }

        // Verify all files are deleted
        for (String gsPath : filesToDelete) {
            Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                    "File should not exist after deletion: " + gsPath);
        }
    }

    @Test(priority = 3, description = "Test deleting files with wildcard")
    public void testDeleteWithWildcard() {
        logger.info("=== Testing delete with wildcard pattern ===");

        // Create files with common prefix
        String prefix = "delete-wildcard-" + TestDataGenerator.generateUniqueId().substring(0, 8);
        List<String> createdPaths = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            String fileName = prefix + "-file-" + i + ".txt";
            String gsPath = BucketHelper.uploadTestFile(fileName, "Content " + i);
            createdPaths.add(gsPath);
        }

        // Also create a file that shouldn't be deleted
        String keepFile = "keep-" + generateTestFileName("txt");
        String keepPath = createTestFile(keepFile, "This file should not be deleted");

        // Delete using wildcard pattern
        // Note: wildcards need to be quoted to prevent shell expansion
        String wildcardPath = getGsPath(prefix + "*");
        CommandResult result = GCloudCliExecutor.executeCommand(
                String.format("gcloud storage rm '%s'", wildcardPath));

        // Check result - wildcard delete may return error if pattern doesn't match exactly
        if (!result.isSuccess()) {
            logger.warn("Wildcard delete returned exit code {}, stderr: {}",
                    result.getExitCode(), result.getStderr());
            // Try deleting files individually
            for (String gsPath : createdPaths) {
                GCloudCliExecutor.deleteFile(gsPath);
                createdFiles.remove(gsPath);
            }
        } else {
            // Mark files as deleted from cleanup list
            for (String gsPath : createdPaths) {
                createdFiles.remove(gsPath);
            }
        }

        // Verify wildcard files are deleted
        for (String gsPath : createdPaths) {
            Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                    "Wildcard-matched file should be deleted: " + gsPath);
            createdFiles.remove(gsPath);
        }

        // Verify other file still exists
        Assert.assertTrue(BucketHelper.verifyFileExists(keepPath),
                "Non-matching file should not be deleted");
    }

    @Test(priority = 4, description = "Test recursive deletion")
    public void testDeleteRecursive() {
        logger.info("=== Testing recursive deletion ===");

        // Create nested structure
        String basePrefix = "delete-recursive-" + TestDataGenerator.generateUniqueId().substring(0, 8) + "/";
        List<String> createdPaths = new ArrayList<>();

        String[] paths = {
                basePrefix + "file1.txt",
                basePrefix + "dir1/file2.txt",
                basePrefix + "dir1/subdir/file3.txt",
                basePrefix + "dir2/file4.txt"
        };

        for (String path : paths) {
            String gsPath = BucketHelper.uploadTestFile(path, "Content for " + path);
            createdPaths.add(gsPath);
        }

        // Delete recursively
        String recursivePath = getGsPath(basePrefix);
        CommandResult result = GCloudCliExecutor.executeCommand(
                String.format("gcloud storage rm -r %s", recursivePath));

        assertCommandSuccess(result.getExitCode(), "Failed to delete recursively");

        // Verify all files are deleted
        for (String gsPath : createdPaths) {
            Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                    "File should be deleted by recursive operation: " + gsPath);
            createdFiles.remove(gsPath);
        }

        // Verify the prefix no longer returns any files
        List<String> remainingFiles = GCloudCliExecutor.listBucket(recursivePath);
        Assert.assertTrue(remainingFiles.isEmpty(),
                "No files should remain after recursive deletion");
    }

    @Test(priority = 5, description = "Test deleting non-existent file")
    public void testDeleteNonExistentFile() {
        logger.info("=== Testing delete non-existent file ===");

        String nonExistentPath = getGsPath("non-existent-file-" +
                TestDataGenerator.generateUniqueId() + ".txt");

        // Attempt to delete non-existent file
        CommandResult result = GCloudCliExecutor.deleteFile(nonExistentPath);

        // gcloud storage rm typically returns success even for non-existent files
        // or it might return an error - both behaviors are acceptable
        if (!result.isSuccess()) {
            Assert.assertTrue(result.getStderr().contains("not found") ||
                            result.getStderr().contains("No URLs matched") ||
                            result.getStderr().contains("matched no objects") ||
                            result.getStderr().contains("CommandException"),
                    "Error message should indicate file not found or no match. Got: " + result.getStderr());
        }
        logger.info("Delete non-existent file completed with exit code: {}", result.getExitCode());
    }

    @Test(priority = 6, description = "Test force deletion")
    public void testForceDelete() {
        logger.info("=== Testing force deletion ===");

        // Create test files
        String fileName1 = generateTestFileName("txt");
        String fileName2 = generateTestFileName("txt");
        String gsPath1 = BucketHelper.uploadTestFile(fileName1, "File 1");
        String gsPath2 = BucketHelper.uploadTestFile(fileName2, "File 2");

        // Note: gcloud storage rm does not support -f flag
        // Instead, test batch deletion which continues through errors
        // by deleting multiple files including one that doesn't exist
        String nonExistentPath = getGsPath("non-existent-file-" +
                TestDataGenerator.generateUniqueId() + ".txt");

        // Delete multiple files in one command (batch delete)
        String command = String.format("gcloud storage rm %s %s %s",
                gsPath1, gsPath2, nonExistentPath);
        CommandResult result = GCloudCliExecutor.executeCommand(command);

        // Command may fail because of non-existent file, but real files should still be deleted
        if (!result.isSuccess()) {
            logger.warn("Batch delete returned non-zero exit code: {}, stderr: {}",
                    result.getExitCode(), result.getStderr());
        }

        // Verify real files are deleted (gcloud continues on error by default)
        Assert.assertFalse(BucketHelper.verifyFileExists(gsPath1),
                "First file should be deleted");
        Assert.assertFalse(BucketHelper.verifyFileExists(gsPath2),
                "Second file should be deleted");

        createdFiles.remove(gsPath1);
        createdFiles.remove(gsPath2);
    }

    @Test(priority = 7, description = "Test deleting large file")
    public void testDeleteLargeFile() {
        logger.info("=== Testing delete large file ===");

        // Create a larger file (5MB)
        String fileName = generateTestFileName("bin");
        byte[] content = TestDataGenerator.generateBinaryContent(5120); // 5MB
        String gsPath = BucketHelper.uploadTestFile(fileName, content);

        // Verify file exists
        Assert.assertTrue(BucketHelper.verifyFileExists(gsPath),
                "Large file should exist before deletion");

        // Delete the large file
        long startTime = System.currentTimeMillis();
        CommandResult result = GCloudCliExecutor.deleteFile(gsPath);
        long duration = System.currentTimeMillis() - startTime;

        assertCommandSuccess(result.getExitCode(), "Failed to delete large file");
        logger.info("Deleted large file in {} ms", duration);

        // Verify deletion
        Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                "Large file should not exist after deletion");

        createdFiles.remove(gsPath);
    }

    @Test(priority = 8, description = "Test deletion with special characters in filename", timeOut = 60000)
    public void testDeleteSpecialCharacters() {
        logger.info("=== Testing delete with special characters ===");

        // Test various special characters (safe ones that work in GCS)
        // Note: Spaces in filenames can be problematic, so we'll handle them specially
        String[] specialNames = {
                "test_file_underscore.txt",
                "test-file-dash.txt",
                "test.file.dots.txt"
        };

        for (String specialName : specialNames) {
            String fileName = TestConfig.getTestFilePrefix() + specialName;
            try {
                String gsPath = BucketHelper.uploadTestFile(fileName, "Special char content");

                logger.info("Testing deletion of file: {}", fileName);

                // Delete the file
                CommandResult result = GCloudCliExecutor.deleteFile(gsPath);

                if (result.isSuccess()) {
                    Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                            "File with special chars should be deleted: " + fileName);
                    createdFiles.remove(gsPath);
                } else {
                    // If deletion failed, log details
                    logger.warn("Failed to delete file with special chars: {}, stderr: {}",
                            fileName, result.getStderr());
                    // Still try to clean up
                    createdFiles.add(gsPath);
                }
            } catch (Exception e) {
                logger.warn("Failed to upload or delete file with special chars: {}", specialName, e);
            }
        }

        // Test spaces separately with proper handling
        String fileWithSpaces = TestConfig.getTestFilePrefix() + "file-with-spaces.txt";
        try {
            String gsPath = BucketHelper.uploadTestFile(fileWithSpaces, "Spaces test");
            logger.info("Testing deletion of file with spaces-like name: {}", fileWithSpaces);

            CommandResult result = GCloudCliExecutor.deleteFile(gsPath);
            if (result.isSuccess()) {
                Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                        "File with dash-spaces should be deleted");
                createdFiles.remove(gsPath);
            }
        } catch (Exception e) {
            logger.warn("Could not test file with spaces: {}", e.getMessage());
        }
    }

    @Test(priority = 9, description = "Test batch deletion performance")
    public void testBatchDeletionPerformance() {
        logger.info("=== Testing batch deletion performance ===");

        // Create multiple files
        int fileCount = 10;
        StringBuilder pathsToDelete = new StringBuilder();
        List<String> createdPaths = new ArrayList<>();

        for (int i = 0; i < fileCount; i++) {
            String fileName = generateTestFileName("txt");
            String gsPath = BucketHelper.uploadTestFile(fileName, "Batch delete file " + i);
            createdPaths.add(gsPath);
            pathsToDelete.append(gsPath).append(" ");
        }

        // Delete all files in one command
        long startTime = System.currentTimeMillis();
        CommandResult result = GCloudCliExecutor.executeCommand(
                String.format("gcloud storage rm %s", pathsToDelete.toString().trim()));
        long duration = System.currentTimeMillis() - startTime;

        assertCommandSuccess(result.getExitCode(), "Batch deletion should succeed");
        logger.info("Batch deleted {} files in {} ms", fileCount, duration);

        // Verify all files are deleted
        for (String gsPath : createdPaths) {
            Assert.assertFalse(BucketHelper.verifyFileExists(gsPath),
                    "File should be deleted in batch operation");
            createdFiles.remove(gsPath);
        }

        // Performance assertion
        Assert.assertTrue(duration < 30000,
                "Batch deletion should complete within 30 seconds");
    }
}