package com.gcs.testing.base;

import com.gcs.testing.config.TestConfig;
import com.gcs.testing.utils.BucketHelper;
import com.gcs.testing.utils.GCloudCliExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Base test class providing common setup, teardown, and utility methods.
 */
public abstract class BaseTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // Track files created during test for cleanup
    protected final List<String> createdFiles = new ArrayList<>();

    @BeforeSuite
    public void beforeSuite() {
        logger.info("=== Starting GCS CLI Test Suite ===");

        // Validate configuration
        TestConfig.validateConfiguration();

        // Check gcloud availability
        if (!GCloudCliExecutor.isGcloudAvailable()) {
            throw new IllegalStateException("gcloud CLI is not available. Please install and configure gcloud CLI.");
        }

        // Check authentication
        if (!GCloudCliExecutor.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated with gcloud. Please run 'gcloud auth login'.");
        }

        logger.info("Configuration validated. Project: {}, Bucket: {}",
                   TestConfig.getProjectId(), TestConfig.getBucketName());
    }

    @BeforeClass
    public void beforeClass() {
        logger.info("Starting test class: {}", getClass().getSimpleName());
    }

    @BeforeMethod
    public void beforeMethod() {
        logger.info("Starting test method");
        // Clear the list of created files for this test
        createdFiles.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        logger.info("Cleaning up test resources");

        // Clean up all files created during the test
        for (String filePath : createdFiles) {
            try {
                BucketHelper.deleteTestFile(filePath);
                logger.debug("Deleted test file: {}", filePath);
            } catch (Exception e) {
                logger.warn("Failed to delete test file: {}", filePath, e);
            }
        }

        createdFiles.clear();
    }

    @AfterClass
    public void afterClass() {
        logger.info("Completed test class: {}", getClass().getSimpleName());
    }

    @AfterSuite
    public void afterSuite() {
        logger.info("=== Completed GCS CLI Test Suite ===");

        // Optional: Clean up all test files with the configured prefix
        // Uncomment if you want aggressive cleanup after suite
        // BucketHelper.cleanupTestFiles();
    }

    /**
     * Creates a test file and tracks it for cleanup.
     *
     * @param fileName the file name
     * @param content the file content
     * @return the GCS path of the created file
     */
    protected String createTestFile(String fileName, String content) {
        String gsPath = BucketHelper.uploadTestFile(fileName, content);
        createdFiles.add(gsPath);
        logger.info("Created test file: {}", gsPath);
        return gsPath;
    }

    /**
     * Creates a binary test file and tracks it for cleanup.
     *
     * @param fileName the file name
     * @param content the binary content
     * @return the GCS path of the created file
     */
    protected String createTestFile(String fileName, byte[] content) {
        String gsPath = BucketHelper.uploadTestFile(fileName, content);
        createdFiles.add(gsPath);
        logger.info("Created binary test file: {}", gsPath);
        return gsPath;
    }

    /**
     * Generates a unique test file name with extension.
     *
     * @param extension the file extension (e.g., "txt", "json")
     * @return a unique file name
     */
    protected String generateTestFileName(String extension) {
        return BucketHelper.generateUniqueFileName(extension);
    }

    /**
     * Asserts that a command was successful.
     *
     * @param exitCode the exit code
     * @param errorMessage the error message to display if assertion fails
     */
    protected void assertCommandSuccess(int exitCode, String errorMessage) {
        if (exitCode != 0) {
            throw new AssertionError(errorMessage + " (exit code: " + exitCode + ")");
        }
    }

    /**
     * Logs test information.
     *
     * @param message the message to log
     * @param args the message arguments
     */
    protected void logInfo(String message, Object... args) {
        logger.info("[{}] " + message, getClass().getSimpleName(), args);
    }

    /**
     * Waits for a specified time.
     *
     * @param milliseconds the time to wait in milliseconds
     */
    protected void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }

    /**
     * Gets the full GCS path for a file name.
     *
     * @param fileName the file name
     * @return the full gs:// path
     */
    protected String getGsPath(String fileName) {
        return TestConfig.getGsPath(fileName);
    }
}