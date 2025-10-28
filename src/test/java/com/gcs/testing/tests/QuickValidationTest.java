package com.gcs.testing.tests;

import com.gcs.testing.base.BaseTest;
import com.gcs.testing.models.CommandResult;
import com.gcs.testing.utils.GCloudCliExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Quick validation tests to verify framework setup without hitting GCS.
 * These tests run in seconds to validate the fixes work.
 */
public class QuickValidationTest extends BaseTest {

    @Test(priority = 1, description = "Verify gcloud CLI is available")
    public void testGcloudAvailable() {
        logger.info("=== Testing gcloud CLI availability ===");

        boolean available = GCloudCliExecutor.isGcloudAvailable();
        Assert.assertTrue(available, "gcloud CLI should be available");

        logger.info("gcloud CLI is available: {}", available);
    }

    @Test(priority = 2, description = "Verify gcloud authentication")
    public void testGcloudAuthentication() {
        logger.info("=== Testing gcloud authentication ===");

        boolean authenticated = GCloudCliExecutor.isAuthenticated();
        Assert.assertTrue(authenticated, "gcloud should be authenticated");

        logger.info("gcloud is authenticated: {}", authenticated);
    }

    @Test(priority = 3, description = "Test command execution framework")
    public void testCommandExecution() {
        logger.info("=== Testing command execution framework ===");

        // Test a simple command that doesn't hit GCS
        CommandResult result = GCloudCliExecutor.executeCommand("gcloud --version");

        Assert.assertNotNull(result, "Command result should not be null");
        Assert.assertEquals(result.getExitCode(), 0, "Command should succeed");
        Assert.assertTrue(result.getStdout().contains("Google Cloud SDK"),
                "Output should contain SDK version info");

        logger.info("Command execution framework works correctly");
    }

    @Test(priority = 4, description = "Test unique file name generation")
    public void testUniqueFileNameGeneration() {
        logger.info("=== Testing unique file name generation ===");

        String fileName1 = generateTestFileName("txt");
        String fileName2 = generateTestFileName("txt");

        Assert.assertNotNull(fileName1, "File name should not be null");
        Assert.assertNotNull(fileName2, "File name should not be null");
        Assert.assertNotEquals(fileName1, fileName2, "File names should be unique");
        Assert.assertTrue(fileName1.endsWith(".txt"), "File name should have correct extension");

        logger.info("Generated unique file names: {} and {}", fileName1, fileName2);
    }

    @Test(priority = 5, description = "Test GCS path generation")
    public void testGcsPathGeneration() {
        logger.info("=== Testing GCS path generation ===");

        String testFile = "test-file.txt";
        String gsPath = getGsPath(testFile);

        Assert.assertNotNull(gsPath, "GCS path should not be null");
        Assert.assertTrue(gsPath.startsWith("gs://"), "Path should start with gs://");
        Assert.assertTrue(gsPath.endsWith(testFile), "Path should end with file name");

        logger.info("GCS path generated: {}", gsPath);
    }
}
