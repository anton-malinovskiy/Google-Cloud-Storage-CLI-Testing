package com.gcs.testing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration class for GCS CLI tests.
 * Reads configuration from environment variables and system properties.
 */
public class TestConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

    // Configuration keys
    private static final String PROJECT_ID_KEY = "GCS_PROJECT_ID";
    private static final String BUCKET_NAME_KEY = "GCS_BUCKET_NAME";
    private static final String TEST_FILE_PREFIX_KEY = "GCS_TEST_FILE_PREFIX";

    // Default values
    private static final String DEFAULT_TEST_FILE_PREFIX = "test-";
    private static final int DEFAULT_SIGNED_URL_DURATION_MINUTES = 60;
    private static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;

    // Cached values
    private static String projectId;
    private static String bucketName;
    private static String testFilePrefix;

    static {
        loadConfiguration();
    }

    /**
     * Loads configuration from environment variables and system properties.
     * System properties take precedence over environment variables.
     */
    private static void loadConfiguration() {
        // Project ID
        projectId = getConfigValue(PROJECT_ID_KEY, "gcs.project.id");
        if (projectId == null || projectId.isEmpty()) {
            logger.warn("GCS_PROJECT_ID not set. Tests requiring project ID will fail.");
        }

        // Bucket name
        bucketName = getConfigValue(BUCKET_NAME_KEY, "gcs.bucket.name");
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalStateException("GCS_BUCKET_NAME must be set");
        }

        // Test file prefix
        testFilePrefix = getConfigValue(TEST_FILE_PREFIX_KEY, "gcs.test.file.prefix");
        if (testFilePrefix == null || testFilePrefix.isEmpty()) {
            testFilePrefix = DEFAULT_TEST_FILE_PREFIX;
        }

        logger.info("Configuration loaded - Project: {}, Bucket: {}, Prefix: {}",
                    projectId, bucketName, testFilePrefix);
    }

    /**
     * Gets configuration value from system property or environment variable.
     * System property takes precedence.
     */
    private static String getConfigValue(String envKey, String propKey) {
        String value = System.getProperty(propKey);
        if (value == null || value.isEmpty()) {
            value = System.getenv(envKey);
        }
        return value;
    }

    /**
     * Gets the Google Cloud Project ID.
     */
    public static String getProjectId() {
        return projectId;
    }

    /**
     * Gets the GCS bucket name for testing.
     */
    public static String getBucketName() {
        return bucketName;
    }

    /**
     * Gets the test file prefix.
     */
    public static String getTestFilePrefix() {
        return testFilePrefix;
    }

    /**
     * Gets the full GCS path for a file.
     * @param fileName the file name
     * @return the full gs:// path
     */
    public static String getGsPath(String fileName) {
        return String.format("gs://%s/%s", bucketName, fileName);
    }

    /**
     * Gets the default duration for signed URLs in minutes.
     */
    public static int getSignedUrlDurationMinutes() {
        return DEFAULT_SIGNED_URL_DURATION_MINUTES;
    }

    /**
     * Gets the command execution timeout in seconds.
     */
    public static int getCommandTimeoutSeconds() {
        return DEFAULT_COMMAND_TIMEOUT_SECONDS;
    }

    /**
     * Gets the number of retry attempts for flaky operations.
     */
    public static int getRetryAttempts() {
        return DEFAULT_RETRY_ATTEMPTS;
    }

    /**
     * Gets the delay between retry attempts in milliseconds.
     */
    public static long getRetryDelayMs() {
        return DEFAULT_RETRY_DELAY_MS;
    }

    /**
     * Validates that all required configuration is present.
     * @throws IllegalStateException if required configuration is missing
     */
    public static void validateConfiguration() {
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalStateException("GCS_BUCKET_NAME environment variable must be set");
        }
        logger.info("Configuration validated successfully");
    }

    /**
     * Reloads configuration from environment variables and system properties.
     * Useful for testing.
     */
    public static void reloadConfiguration() {
        loadConfiguration();
    }
}