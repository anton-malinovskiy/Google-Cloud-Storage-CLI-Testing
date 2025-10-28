package com.gcs.testing.tests;

import com.gcs.testing.base.BaseTest;
import com.gcs.testing.models.CommandResult;
import com.gcs.testing.utils.BucketHelper;
import com.gcs.testing.utils.GCloudCliExecutor;
import com.gcs.testing.utils.TestDataGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for the 'gcloud storage cp' command.
 */
public class CopyCommandTest extends BaseTest {

    @Test(priority = 1, description = "Test copying local file to GCS")
    public void testCopyLocalToGcs() {
        logger.info("=== Testing copy from local to GCS ===");

        // Create a local file
        String fileName = generateTestFileName("txt");
        String content = TestDataGenerator.generateRandomContent(5); // 5KB
        File localFile = BucketHelper.createLocalFile(fileName, content);

        try {
            // Copy to GCS
            String gsPath = getGsPath(fileName);
            CommandResult result = GCloudCliExecutor.copyFile(localFile.getAbsolutePath(), gsPath);

            assertCommandSuccess(result.getExitCode(), "Failed to copy file to GCS");
            logger.info("Successfully copied file to: {}", gsPath);

            // Track for cleanup
            createdFiles.add(gsPath);

            // Verify file exists in GCS
            Assert.assertTrue(BucketHelper.verifyFileExists(gsPath),
                    "File should exist in GCS after copy");

            // Verify content matches
            String gcsContent = BucketHelper.readFileContent(gsPath);
            Assert.assertEquals(gcsContent.trim(), content.trim(),
                    "Content should match after upload");

        } finally {
            // Clean up local file
            localFile.delete();
            if (localFile.getParentFile() != null) {
                localFile.getParentFile().delete();
            }
        }
    }

    @Test(priority = 2, description = "Test copying file from GCS to local")
    public void testCopyGcsToLocal() {
        logger.info("=== Testing copy from GCS to local ===");

        // First, upload a test file
        String fileName = generateTestFileName("json");
        String content = TestDataGenerator.generateJsonContent();
        String gsPath = createTestFile(fileName, content);

        Path downloadPath = null;
        try {
            // Copy from GCS to local
            downloadPath = Paths.get("target/downloads/" + fileName);
            Files.createDirectories(downloadPath.getParent());

            CommandResult result = GCloudCliExecutor.copyFile(gsPath, downloadPath.toString());

            assertCommandSuccess(result.getExitCode(), "Failed to copy file from GCS");
            logger.info("Successfully downloaded file to: {}", downloadPath);

            // Verify local file exists
            Assert.assertTrue(Files.exists(downloadPath),
                    "Downloaded file should exist locally");

            // Verify content matches
            String downloadedContent = Files.readString(downloadPath);
            Assert.assertEquals(downloadedContent.trim(), content.trim(),
                    "Downloaded content should match original");

        } catch (Exception e) {
            Assert.fail("Error during file download test: " + e.getMessage(), e);
        } finally {
            // Clean up downloaded file
            if (downloadPath != null) {
                try {
                    Files.deleteIfExists(downloadPath);
                } catch (Exception e) {
                    logger.warn("Failed to clean up downloaded file", e);
                }
            }
        }
    }

    @Test(priority = 3, description = "Test copying between GCS locations")
    public void testCopyGcsToGcs() {
        logger.info("=== Testing copy between GCS locations ===");

        // Upload source file
        String sourceFileName = generateTestFileName("csv");
        String content = TestDataGenerator.generateCsvContent(100);
        String sourcePath = createTestFile(sourceFileName, content);

        // Define destination
        String destFileName = "copy-" + sourceFileName;
        String destPath = getGsPath(destFileName);

        // Copy within GCS
        CommandResult result = GCloudCliExecutor.copyFile(sourcePath, destPath);

        assertCommandSuccess(result.getExitCode(), "Failed to copy between GCS locations");
        logger.info("Successfully copied from {} to {}", sourcePath, destPath);

        // Track destination for cleanup
        createdFiles.add(destPath);

        // Verify both files exist
        Assert.assertTrue(BucketHelper.verifyFileExists(sourcePath),
                "Source file should still exist");
        Assert.assertTrue(BucketHelper.verifyFileExists(destPath),
                "Destination file should exist");

        // Verify content matches
        String sourceContent = BucketHelper.readFileContent(sourcePath);
        String destContent = BucketHelper.readFileContent(destPath);
        Assert.assertEquals(destContent, sourceContent,
                "Content should be identical after GCS to GCS copy");
    }

    @Test(priority = 4, description = "Test copying with recursive flag")
    public void testCopyRecursive() {
        logger.info("=== Testing recursive copy ===");

        // Create multiple files with a common prefix
        String prefix = "recursive-test/";
        String[] fileNames = new String[3];

        for (int i = 0; i < 3; i++) {
            fileNames[i] = prefix + generateTestFileName("txt");
            String content = "Recursive test file " + i + ": " + TestDataGenerator.generateRandomContent(1);
            createTestFile(fileNames[i], content);
        }

        // Create local directory for download
        Path localDir = Paths.get("target/recursive-test");
        try {
            Files.createDirectories(localDir);

            // Copy recursively from GCS to local
            String sourcePath = getGsPath(prefix);
            CommandResult result = GCloudCliExecutor.executeCommand(
                    String.format("gcloud storage cp -r %s %s", sourcePath, localDir));

            assertCommandSuccess(result.getExitCode(), "Failed to copy recursively");

            // Verify all files were copied
            for (String fileName : fileNames) {
                Path expectedFile = localDir.resolve(fileName);
                Assert.assertTrue(Files.exists(expectedFile),
                        "File should exist after recursive copy: " + fileName);
            }

        } catch (Exception e) {
            Assert.fail("Error during recursive copy test: " + e.getMessage(), e);
        } finally {
            // Clean up local directory
            try {
                if (Files.exists(localDir)) {
                    Files.walk(localDir)
                         .sorted((a, b) -> -a.compareTo(b))
                         .forEach(path -> {
                             try {
                                 Files.delete(path);
                             } catch (Exception e) {
                                 logger.warn("Failed to delete: {}", path);
                             }
                         });
                }
            } catch (Exception e) {
                logger.warn("Failed to clean up recursive test directory", e);
            }
        }
    }

    @Test(priority = 5, description = "Test copying large file")
    public void testCopyLargeFile() {
        logger.info("=== Testing copy of large file ===");

        // Create a larger file (1MB)
        String fileName = generateTestFileName("bin");
        byte[] content = TestDataGenerator.generateBinaryContent(1024); // 1MB

        File localFile = null;
        Path downloadPath = null;
        try {
            // Write to local file first
            localFile = File.createTempFile("large-", ".bin");
            Files.write(localFile.toPath(), content);

            // Upload to GCS
            String gsPath = getGsPath(fileName);
            CommandResult uploadResult = GCloudCliExecutor.copyFile(localFile.getAbsolutePath(), gsPath);

            assertCommandSuccess(uploadResult.getExitCode(), "Failed to upload large file");
            createdFiles.add(gsPath);

            // Download back
            downloadPath = Paths.get("target/large-download.bin");
            CommandResult downloadResult = GCloudCliExecutor.copyFile(gsPath, downloadPath.toString());

            assertCommandSuccess(downloadResult.getExitCode(), "Failed to download large file");

            // Verify sizes match
            long uploadedSize = localFile.length();
            long downloadedSize = Files.size(downloadPath);
            Assert.assertEquals(downloadedSize, uploadedSize,
                    "Downloaded file size should match uploaded size");

        } catch (Exception e) {
            Assert.fail("Error during large file copy test: " + e.getMessage(), e);
        } finally {
            // Clean up
            if (localFile != null) {
                localFile.delete();
            }
            if (downloadPath != null) {
                try {
                    Files.deleteIfExists(downloadPath);
                } catch (Exception e) {
                    logger.warn("Failed to delete downloaded large file", e);
                }
            }
        }
    }

    @Test(priority = 6, description = "Test copying with content type preservation")
    public void testCopyWithContentType() {
        logger.info("=== Testing copy with content type ===");

        // Test with different file types
        String[][] testCases = {
                {"html", TestDataGenerator.generateHtmlContent()},
                {"json", TestDataGenerator.generateJsonContent()},
                {"csv", TestDataGenerator.generateCsvContent(50)}
        };

        for (String[] testCase : testCases) {
            String extension = testCase[0];
            String content = testCase[1];
            String fileName = generateTestFileName(extension);

            logger.info("Testing content type for .{} file", extension);

            // Upload file
            String gsPath = createTestFile(fileName, content);

            // Use gcloud storage to check metadata
            CommandResult metadataResult = GCloudCliExecutor.executeCommand(
                    String.format("gcloud storage objects describe %s --format=json", gsPath));

            if (metadataResult.isSuccess()) {
                String output = metadataResult.getStdout();
                logger.info("File metadata for {}: {}", extension,
                           output.contains("contentType") ? "has content type" : "no content type");
            }
        }
    }

    @Test(priority = 7, description = "Test copy error handling")
    public void testCopyErrorHandling() {
        logger.info("=== Testing copy error handling ===");

        // Test 1: Copy non-existent file
        String nonExistentPath = getGsPath("non-existent-file.txt");
        CommandResult result1 = GCloudCliExecutor.copyFile(nonExistentPath, "target/download.txt");

        Assert.assertFalse(result1.isSuccess(),
                "Copy of non-existent file should fail");
        Assert.assertTrue(result1.getStderr().length() > 0,
                "Error message should be provided for failed copy");

        // Test 2: Copy to invalid destination
        String fileName = generateTestFileName("txt");
        String gsPath = createTestFile(fileName, "test content");

        CommandResult result2 = GCloudCliExecutor.copyFile(gsPath, "/invalid/path/that/does/not/exist/file.txt");

        Assert.assertFalse(result2.isSuccess(),
                "Copy to invalid destination should fail");
    }
}