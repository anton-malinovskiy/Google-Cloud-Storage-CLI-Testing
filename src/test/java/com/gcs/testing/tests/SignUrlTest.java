package com.gcs.testing.tests;

import com.gcs.testing.base.BaseTest;
import com.gcs.testing.config.TestConfig;
import com.gcs.testing.utils.BrowserHelper;
import com.gcs.testing.utils.GCloudCliExecutor;
import com.gcs.testing.utils.TestDataGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tests for the 'gcloud storage sign-url' command.
 * Primary focus: Detecting phishing warnings when accessing signed URLs.
 */
public class SignUrlTest extends BaseTest {

    @Test(priority = 1, description = "Test generating and validating a signed URL for text file")
    public void testSignUrlWithTextFile() {
        logger.info("=== Testing sign-url command with text file ===");

        // Generate test data
        String fileName = generateTestFileName("txt");
        String content = TestDataGenerator.generateRandomContent(1); // 1KB file

        // Upload test file
        String gsPath = createTestFile(fileName, content);

        // Generate signed URL
        String signedUrl = GCloudCliExecutor.generateSignedUrl(gsPath,
                TestConfig.getSignedUrlDurationMinutes());

        Assert.assertNotNull(signedUrl, "Signed URL should not be null");
        Assert.assertTrue(signedUrl.startsWith("http"), "Signed URL should start with http");

        // Validate the signed URL using browser
        try (BrowserHelper browser = new BrowserHelper(true)) { // headless mode
            BrowserHelper.SignedUrlValidationResult result = browser.validateSignedUrl(signedUrl);

            // Log validation results
            logger.info("Validation result: {}", result);

            // Assert no phishing warning
            Assert.assertFalse(result.isPhishingDetected(),
                    "CRITICAL: Phishing warning detected! URL flagged as deceptive. Screenshot: " +
                    result.getScreenshotPath());

            // Assert HTTP status is 200
            Assert.assertEquals(result.getHttpStatusCode(), 200,
                    "Expected HTTP 200 status code for signed URL");

            // Verify we can actually download the file
            String downloadPath = "target/test-downloads/" + fileName;
            Files.createDirectories(Paths.get("target/test-downloads"));
            boolean downloaded = browser.downloadFile(signedUrl, downloadPath);

            Assert.assertTrue(downloaded, "File should be downloadable via signed URL");

            // Verify downloaded content matches
            String downloadedContent = Files.readString(Paths.get(downloadPath));
            Assert.assertEquals(downloadedContent.trim(), content.trim(),
                    "Downloaded content should match uploaded content");

            // Clean up downloaded file
            Files.deleteIfExists(Paths.get(downloadPath));
        } catch (Exception e) {
            Assert.fail("Error during browser validation: " + e.getMessage(), e);
        }
    }

    @Test(priority = 2, description = "Test signed URL with JSON file")
    public void testSignUrlWithJsonFile() {
        logger.info("=== Testing sign-url command with JSON file ===");

        String fileName = generateTestFileName("json");
        String content = TestDataGenerator.generateJsonContent();

        String gsPath = createTestFile(fileName, content);
        String signedUrl = GCloudCliExecutor.generateSignedUrl(gsPath,
                TestConfig.getSignedUrlDurationMinutes());

        Assert.assertNotNull(signedUrl, "Signed URL should not be null");

        try (BrowserHelper browser = new BrowserHelper(true)) {
            BrowserHelper.SignedUrlValidationResult result = browser.validateSignedUrl(signedUrl);

            Assert.assertFalse(result.isPhishingDetected(),
                    "Phishing warning detected for JSON file! Screenshot: " + result.getScreenshotPath());
            Assert.assertEquals(result.getHttpStatusCode(), 200,
                    "Expected HTTP 200 for JSON file signed URL");
            Assert.assertTrue(result.isSuccess(), "Signed URL validation should be successful");
        }
    }

    @Test(priority = 3, description = "Test signed URL with HTML file")
    public void testSignUrlWithHtmlFile() {
        logger.info("=== Testing sign-url command with HTML file ===");

        String fileName = generateTestFileName("html");
        String content = TestDataGenerator.generateHtmlContent();

        String gsPath = createTestFile(fileName, content);
        String signedUrl = GCloudCliExecutor.generateSignedUrl(gsPath,
                TestConfig.getSignedUrlDurationMinutes());

        Assert.assertNotNull(signedUrl, "Signed URL should not be null");

        try (BrowserHelper browser = new BrowserHelper(true)) {
            BrowserHelper.SignedUrlValidationResult result = browser.validateSignedUrl(signedUrl);

            // HTML files are more likely to trigger phishing warnings
            if (result.isPhishingDetected()) {
                logger.error("CRITICAL: Phishing warning detected for HTML file!");
                logger.error("Screenshot saved at: {}", result.getScreenshotPath());
                logger.error("Page title: {}", result.getPageTitle());
            }

            Assert.assertFalse(result.isPhishingDetected(),
                    "Phishing warning detected for HTML file! This is a critical issue.");
            Assert.assertEquals(result.getHttpStatusCode(), 200,
                    "Expected HTTP 200 for HTML file signed URL");
        }
    }

    @Test(priority = 4, description = "Test signed URL with short duration")
    public void testSignUrlWithShortDuration() {
        logger.info("=== Testing sign-url command with short duration ===");

        String fileName = generateTestFileName("txt");
        String content = "Short duration test file";

        String gsPath = createTestFile(fileName, content);

        // Generate URL with 1 minute duration
        String signedUrl = GCloudCliExecutor.generateSignedUrl(gsPath, 1);
        Assert.assertNotNull(signedUrl, "Signed URL should not be null");

        // Immediate validation should succeed
        try (BrowserHelper browser = new BrowserHelper(true)) {
            BrowserHelper.SignedUrlValidationResult result = browser.validateSignedUrl(signedUrl);

            Assert.assertFalse(result.isPhishingDetected(),
                    "Phishing warning should not appear for fresh signed URL");
            Assert.assertEquals(result.getHttpStatusCode(), 200,
                    "Fresh signed URL should return HTTP 200");
        }
    }

    @Test(priority = 5, description = "Test signed URL with binary file")
    public void testSignUrlWithBinaryFile() {
        logger.info("=== Testing sign-url command with binary file ===");

        String fileName = generateTestFileName("bin");
        byte[] content = TestDataGenerator.generateBinaryContent(5); // 5KB binary

        String gsPath = createTestFile(fileName, content);
        String signedUrl = GCloudCliExecutor.generateSignedUrl(gsPath,
                TestConfig.getSignedUrlDurationMinutes());

        Assert.assertNotNull(signedUrl, "Signed URL should not be null");

        try (BrowserHelper browser = new BrowserHelper(true)) {
            // For binary files, we mainly check that no phishing warning appears
            BrowserHelper.SignedUrlValidationResult result = browser.validateSignedUrl(signedUrl);

            Assert.assertFalse(result.isPhishingDetected(),
                    "Binary files should not trigger phishing warnings");

            // Binary files might redirect or prompt download, so accept 200 or 302
            Assert.assertTrue(result.getHttpStatusCode() == 200 || result.getHttpStatusCode() == 302,
                    "Expected HTTP 200 or 302 for binary file, got: " + result.getHttpStatusCode());
        }
    }

    @Test(priority = 6, description = "Test multiple signed URLs in parallel")
    public void testMultipleSignedUrls() {
        logger.info("=== Testing multiple signed URLs ===");

        // Create multiple files
        String[] fileNames = new String[3];
        String[] signedUrls = new String[3];

        for (int i = 0; i < 3; i++) {
            fileNames[i] = generateTestFileName("txt");
            String content = "Test file " + i + ": " + TestDataGenerator.generateRandomContent(1);
            String gsPath = createTestFile(fileNames[i], content);
            signedUrls[i] = GCloudCliExecutor.generateSignedUrl(gsPath,
                    TestConfig.getSignedUrlDurationMinutes());
        }

        // Validate all URLs
        try (BrowserHelper browser = new BrowserHelper(true)) {
            for (int i = 0; i < signedUrls.length; i++) {
                logger.info("Validating URL {} of {}", i + 1, signedUrls.length);

                BrowserHelper.SignedUrlValidationResult result = browser.validateSignedUrl(signedUrls[i]);

                Assert.assertFalse(result.isPhishingDetected(),
                        String.format("Phishing warning for file %d: %s", i, fileNames[i]));
                Assert.assertEquals(result.getHttpStatusCode(), 200,
                        String.format("Expected HTTP 200 for file %d", i));
            }
        }
    }

    @Test(priority = 7, description = "Test signed URL format and components")
    public void testSignedUrlFormat() {
        logger.info("=== Testing signed URL format ===");

        String fileName = generateTestFileName("txt");
        String content = "URL format test";

        String gsPath = createTestFile(fileName, content);
        String signedUrl = GCloudCliExecutor.generateSignedUrl(gsPath,
                TestConfig.getSignedUrlDurationMinutes());

        // Log the URL to see its format
        logger.info("Generated signed URL: {}", signedUrl);

        // Verify URL components - gcloud storage sign-url may use different domains
        Assert.assertTrue(signedUrl.startsWith("http"),
                "Signed URL should start with http");
        Assert.assertTrue(signedUrl.contains("Expires=") || signedUrl.contains("x-goog-expires"),
                "Signed URL should contain Expires parameter");
        Assert.assertTrue(signedUrl.contains("Signature=") || signedUrl.contains("x-goog-signature"),
                "Signed URL should contain Signature parameter");

        // URL should be properly encoded
        Assert.assertFalse(signedUrl.contains(" "), "URL should not contain spaces");
    }
}