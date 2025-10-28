package com.gcs.testing.utils;

import com.gcs.testing.config.TestConfig;
import com.gcs.testing.models.CommandResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Helper class for GCS bucket operations.
 */
public class BucketHelper {
    private static final Logger logger = LoggerFactory.getLogger(BucketHelper.class);

    /**
     * Uploads a test file to GCS bucket.
     *
     * @param fileName the file name (without gs:// prefix)
     * @param content the file content
     * @return the full GCS path (gs://bucket/file)
     * @throws RuntimeException if upload fails
     */
    public static String uploadTestFile(String fileName, String content) {
        Path tempFile = null;
        try {
            // Create a temporary file
            tempFile = Files.createTempFile("gcs-test-", ".tmp");
            Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));

            // Upload to GCS
            String gsPath = TestConfig.getGsPath(fileName);
            CommandResult result = GCloudCliExecutor.copyFile(tempFile.toString(), gsPath);

            if (!result.isSuccess()) {
                throw new RuntimeException("Failed to upload file: " + result.getStderr());
            }

            logger.info("Successfully uploaded file to: {}", gsPath);
            return gsPath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file", e);
        } finally {
            // Clean up temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Uploads a binary file to GCS bucket.
     *
     * @param fileName the file name
     * @param content the binary content
     * @return the full GCS path
     */
    public static String uploadTestFile(String fileName, byte[] content) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("gcs-test-", ".tmp");
            Files.write(tempFile, content);

            String gsPath = TestConfig.getGsPath(fileName);
            CommandResult result = GCloudCliExecutor.copyFile(tempFile.toString(), gsPath);

            if (!result.isSuccess()) {
                throw new RuntimeException("Failed to upload binary file: " + result.getStderr());
            }

            logger.info("Successfully uploaded binary file to: {}", gsPath);
            return gsPath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Deletes a test file from GCS bucket.
     *
     * @param gsPath the full GCS path (gs://bucket/file)
     */
    public static void deleteTestFile(String gsPath) {
        CommandResult result = GCloudCliExecutor.deleteFile(gsPath);
        if (result.isSuccess()) {
            logger.info("Successfully deleted file: {}", gsPath);
        } else {
            logger.warn("Failed to delete file {}: {}", gsPath, result.getStderr());
        }
    }

    /**
     * Verifies if a file exists in GCS.
     *
     * @param gsPath the full GCS path
     * @return true if file exists, false otherwise
     */
    public static boolean verifyFileExists(String gsPath) {
        try {
            List<String> files = GCloudCliExecutor.listBucket(gsPath);
            boolean exists = files.stream().anyMatch(f -> f.equals(gsPath));
            logger.debug("File {} exists: {}", gsPath, exists);
            return exists;
        } catch (Exception e) {
            logger.debug("File {} does not exist or error occurred: {}", gsPath, e.getMessage());
            return false;
        }
    }

    /**
     * Generates a unique file name with the configured prefix.
     *
     * @param extension the file extension (without dot)
     * @return a unique file name
     */
    public static String generateUniqueFileName(String extension) {
        return TestDataGenerator.generateUniqueFileName(TestConfig.getTestFilePrefix(), extension);
    }

    /**
     * Downloads a file from GCS to a local temporary file.
     *
     * @param gsPath the GCS path
     * @return the path to the downloaded file
     * @throws RuntimeException if download fails
     */
    public static Path downloadFile(String gsPath) {
        try {
            Path tempFile = Files.createTempFile("gcs-download-", ".tmp");
            CommandResult result = GCloudCliExecutor.copyFile(gsPath, tempFile.toString());

            if (!result.isSuccess()) {
                Files.deleteIfExists(tempFile);
                throw new RuntimeException("Failed to download file: " + result.getStderr());
            }

            logger.info("Successfully downloaded file from {} to {}", gsPath, tempFile);
            return tempFile;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file for download", e);
        }
    }

    /**
     * Reads the content of a file from GCS.
     *
     * @param gsPath the GCS path
     * @return the file content as string
     */
    public static String readFileContent(String gsPath) {
        Path downloadedFile = null;
        try {
            downloadedFile = downloadFile(gsPath);
            return Files.readString(downloadedFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read downloaded file", e);
        } finally {
            if (downloadedFile != null) {
                try {
                    Files.deleteIfExists(downloadedFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary downloaded file", e);
                }
            }
        }
    }

    /**
     * Cleans up all test files with the configured prefix from the bucket.
     * Use with caution!
     */
    public static void cleanupTestFiles() {
        String bucketPath = "gs://" + TestConfig.getBucketName() + "/" + TestConfig.getTestFilePrefix();
        try {
            List<String> files = GCloudCliExecutor.listBucket(bucketPath);
            logger.info("Found {} test files to clean up", files.size());

            for (String file : files) {
                if (file.contains(TestConfig.getTestFilePrefix())) {
                    deleteTestFile(file);
                }
            }
        } catch (Exception e) {
            logger.error("Error during test file cleanup", e);
        }
    }

    /**
     * Creates a local file for testing.
     *
     * @param fileName the file name
     * @param content the file content
     * @return the created file
     */
    public static File createLocalFile(String fileName, String content) {
        try {
            File tempDir = Files.createTempDirectory("gcs-test").toFile();
            File file = new File(tempDir, fileName);
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create local file", e);
        }
    }
}