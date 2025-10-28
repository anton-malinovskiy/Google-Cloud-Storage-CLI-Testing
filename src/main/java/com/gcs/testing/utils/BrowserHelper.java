package com.gcs.testing.utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class for browser-based testing using Playwright.
 * Primarily used for testing signed URLs and detecting phishing warnings.
 */
public class BrowserHelper implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(BrowserHelper.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    // Singleton instances to avoid repeated browser launches
    private static Playwright sharedPlaywright;
    private static Browser sharedBrowser;

    // Phishing warning indicators
    private static final List<String> PHISHING_INDICATORS = Arrays.asList(
            "Deceptive site ahead",
            "Dangerous site",
            "Phishing",
            "This site ahead contains harmful programs",
            "Attack site ahead",
            "Suspicious site",
            "Your connection is not private",
            "Security Warning"
    );

    public BrowserHelper() {
        this(false);
    }

    public BrowserHelper(boolean headless) {
        logger.info("Initializing Playwright browser (headless: {})", headless);

        try {
            // Use shared Playwright instance if available
            synchronized (BrowserHelper.class) {
                if (sharedPlaywright == null) {
                    logger.info("Creating new Playwright instance");
                    sharedPlaywright = Playwright.create();
                }
                this.playwright = sharedPlaywright;

                if (sharedBrowser == null || !sharedBrowser.isConnected()) {
                    logger.info("Launching new browser instance");
                    // Use Firefox instead of Chromium - more stable in headless mode on macOS
                    sharedBrowser = playwright.firefox().launch(
                            new BrowserType.LaunchOptions()
                                    .setHeadless(headless)
                                    .setTimeout(120000) // 2 minutes timeout
                    );
                }
                this.browser = sharedBrowser;
            }

            // Create a new context for each test (contexts are lightweight)
            this.context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1920, 1080)
                            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
            );

            logger.info("Browser context initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize browser", e);
            // Clean up any partially initialized resources
            try {
                if (context != null) {
                    context.close();
                }
            } catch (Exception cleanupEx) {
                logger.error("Error during cleanup after initialization failure", cleanupEx);
            }
            throw new RuntimeException("Failed to initialize browser: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a signed URL by accessing it and checking for phishing warnings.
     *
     * @param url the URL to validate
     * @return validation result
     */
    public SignedUrlValidationResult validateSignedUrl(String url) {
        logger.info("Validating signed URL: {}", url);
        Page page = null;

        try {
            page = context.newPage();

            // Set up response listener to capture status code
            final int[] statusCode = {0};
            final boolean[] downloadStarted = {false};

            page.onResponse(response -> {
                if (response.url().equals(url)) {
                    statusCode[0] = response.status();
                    logger.info("Captured response status: {}", response.status());
                }
            });

            // Set up download listener to detect if a download starts
            page.onDownload(download -> {
                downloadStarted[0] = true;
                logger.info("Download detected for URL: {}", download.url());
            });

            Response response = null;
            try {
                // Navigate to the URL - this may fail if download starts
                response = page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(30000));
            } catch (com.microsoft.playwright.PlaywrightException e) {
                // Check if the exception is due to download starting
                if (e.getMessage().contains("Download is starting")) {
                    logger.info("Navigation aborted because download started (this is expected behavior)");
                    // Wait a bit for response listener to capture status
                    Thread.sleep(1000);
                } else {
                    throw e; // Re-throw if it's a different error
                }
            }

            // Get the status code
            int httpStatus = response != null ? response.status() : statusCode[0];

            // If we have a download but no status code, assume 200 (download wouldn't start on error)
            if (downloadStarted[0] && httpStatus == 0) {
                httpStatus = 200;
                logger.info("Download started, assuming HTTP 200");
            }

            logger.info("Final HTTP status code: {}", httpStatus);

            // Check for phishing warnings only if we can access the page
            boolean phishingDetected = false;
            String pageContent = "";
            String pageTitle = "";

            if (!downloadStarted[0]) {
                phishingDetected = checkForPhishingWarning(page);
                pageContent = page.content();
                pageTitle = page.title();
            }

            // Take screenshot for debugging
            String screenshotPath = null;
            if (phishingDetected || (httpStatus != 200 && httpStatus != 302)) {
                screenshotPath = captureScreenshot(page, "validation-" + System.currentTimeMillis());
            }

            return new SignedUrlValidationResult(
                    url,
                    httpStatus,
                    phishingDetected,
                    pageTitle,
                    pageContent,
                    screenshotPath
            );

        } catch (Exception e) {
            logger.error("Error validating signed URL", e);
            return new SignedUrlValidationResult(url, -1, false, null,
                    "Error: " + e.getMessage(), null);
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }

    /**
     * Checks if the page contains phishing warning indicators.
     *
     * @param page the page to check
     * @return true if phishing warning detected, false otherwise
     */
    private boolean checkForPhishingWarning(Page page) {
        try {
            // Check page title
            String title = page.title().toLowerCase();
            for (String indicator : PHISHING_INDICATORS) {
                if (title.contains(indicator.toLowerCase())) {
                    logger.warn("Phishing indicator found in title: {}", indicator);
                    return true;
                }
            }

            // Check visible text content
            String visibleText = page.innerText("body").toLowerCase();
            for (String indicator : PHISHING_INDICATORS) {
                if (visibleText.contains(indicator.toLowerCase())) {
                    logger.warn("Phishing indicator found in page content: {}", indicator);
                    return true;
                }
            }

            // Check for common phishing warning elements
            if (page.isVisible("#warning") ||
                page.isVisible(".warning-message") ||
                page.isVisible("[class*='phishing']") ||
                page.isVisible("[class*='warning']") ||
                page.isVisible("[class*='dangerous']")) {
                logger.warn("Phishing warning element detected");
                return true;
            }

            // Check meta tags
            ElementHandle metaRefresh = page.querySelector("meta[http-equiv='refresh']");
            if (metaRefresh != null) {
                String content = metaRefresh.getAttribute("content");
                if (content != null && content.toLowerCase().contains("phishing")) {
                    logger.warn("Phishing indicator found in meta refresh tag");
                    return true;
                }
            }

            logger.info("No phishing indicators detected");
            return false;

        } catch (Exception e) {
            logger.error("Error checking for phishing warning", e);
            return false;
        }
    }

    /**
     * Captures a screenshot of the current page.
     *
     * @param page the page to capture
     * @param name the screenshot name (without extension)
     * @return the path to the saved screenshot
     */
    private String captureScreenshot(Page page, String name) {
        try {
            String screenshotPath = "target/screenshots/" + name + ".png";
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(screenshotPath))
                    .setFullPage(true));
            logger.info("Screenshot saved: {}", screenshotPath);
            return screenshotPath;
        } catch (Exception e) {
            logger.error("Failed to capture screenshot", e);
            return null;
        }
    }

    /**
     * Downloads a file from a signed URL.
     *
     * @param url the signed URL
     * @param downloadPath the path to save the downloaded file
     * @return true if download successful, false otherwise
     */
    public boolean downloadFile(String url, String downloadPath) {
        Page page = null;
        try {
            page = context.newPage();
            final Page finalPage = page;

            // Set up download handling
            Download download = finalPage.waitForDownload(() -> {
                finalPage.navigate(url);
            });

            // Save the downloaded file
            download.saveAs(Paths.get(downloadPath));

            logger.info("File downloaded successfully to: {}", downloadPath);
            return true;

        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return false;
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }

    @Override
    public void close() {
        try {
            // Only close the context, not the shared browser/playwright
            if (context != null) {
                try {
                    context.close();
                    logger.info("Browser context closed");
                } catch (Exception e) {
                    logger.warn("Error closing browser context", e);
                }
            }
            // Note: Don't close shared browser/playwright as they're reused
        } catch (Exception e) {
            logger.error("Unexpected error during browser cleanup", e);
        }
    }

    /**
     * Shutdown method to clean up shared browser resources.
     * Call this once at the end of all tests.
     */
    public static void shutdownSharedBrowser() {
        synchronized (BrowserHelper.class) {
            try {
                if (sharedBrowser != null) {
                    sharedBrowser.close();
                    sharedBrowser = null;
                    logger.info("Shared browser closed");
                }
                if (sharedPlaywright != null) {
                    sharedPlaywright.close();
                    sharedPlaywright = null;
                    logger.info("Shared Playwright closed");
                }
            } catch (Exception e) {
                logger.error("Error shutting down shared browser", e);
            }
        }
    }

    /**
     * Result of signed URL validation.
     */
    public static class SignedUrlValidationResult {
        private final String url;
        private final int httpStatusCode;
        private final boolean phishingDetected;
        private final String pageTitle;
        private final String pageContent;
        private final String screenshotPath;

        public SignedUrlValidationResult(String url, int httpStatusCode, boolean phishingDetected,
                                        String pageTitle, String pageContent, String screenshotPath) {
            this.url = url;
            this.httpStatusCode = httpStatusCode;
            this.phishingDetected = phishingDetected;
            this.pageTitle = pageTitle;
            this.pageContent = pageContent;
            this.screenshotPath = screenshotPath;
        }

        public String getUrl() {
            return url;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        public boolean isPhishingDetected() {
            return phishingDetected;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public String getPageContent() {
            return pageContent;
        }

        public String getScreenshotPath() {
            return screenshotPath;
        }

        public boolean isSuccess() {
            return httpStatusCode == 200 && !phishingDetected;
        }

        @Override
        public String toString() {
            return String.format("SignedUrlValidationResult{url='%s', httpStatus=%d, phishing=%s, title='%s'}",
                    url, httpStatusCode, phishingDetected, pageTitle);
        }
    }
}