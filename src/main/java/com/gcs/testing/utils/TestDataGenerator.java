package com.gcs.testing.utils;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates test data for GCS CLI tests.
 */
public class TestDataGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Generates random content of specified size.
     *
     * @param sizeInKB the size in kilobytes
     * @return the generated content
     */
    public static String generateRandomContent(int sizeInKB) {
        int totalChars = sizeInKB * 1024;
        StringBuilder sb = new StringBuilder(totalChars);

        // Generate content in chunks for better performance
        int chunkSize = 1024;
        String chunk = generateRandomString(chunkSize);

        for (int i = 0; i < sizeInKB; i++) {
            sb.append(chunk);
        }

        // Add some variation to make each content unique
        sb.append("\n").append("Generated at: ").append(System.currentTimeMillis());
        sb.append("\n").append("Unique ID: ").append(generateUniqueId());

        return sb.toString();
    }

    /**
     * Generates a unique identifier.
     *
     * @return a unique ID
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a unique test file name.
     *
     * @param prefix the file name prefix
     * @param extension the file extension
     * @return a unique file name
     */
    public static String generateUniqueFileName(String prefix, String extension) {
        String uniqueId = generateUniqueId().substring(0, 8); // Use first 8 chars for brevity
        long timestamp = System.currentTimeMillis();
        return String.format("%s%d-%s.%s", prefix, timestamp, uniqueId, extension);
    }

    /**
     * Generates test JSON content.
     *
     * @return JSON content as string
     */
    public static String generateJsonContent() {
        String id = generateUniqueId();
        return String.format("{\n" +
                           "  \"id\": \"%s\",\n" +
                           "  \"timestamp\": %d,\n" +
                           "  \"data\": {\n" +
                           "    \"message\": \"Test data for GCS CLI testing\",\n" +
                           "    \"random\": \"%s\",\n" +
                           "    \"size\": \"small\"\n" +
                           "  }\n" +
                           "}", id, System.currentTimeMillis(), generateRandomString(20));
    }

    /**
     * Generates test CSV content.
     *
     * @param rows the number of rows to generate
     * @return CSV content as string
     */
    public static String generateCsvContent(int rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,value,timestamp\n");

        for (int i = 0; i < rows; i++) {
            sb.append(String.format("%s,%s,%d,%d\n",
                    generateUniqueId().substring(0, 8),
                    "test-" + generateRandomString(5),
                    random.nextInt(1000),
                    System.currentTimeMillis()));
        }

        return sb.toString();
    }

    /**
     * Generates a random string of specified length.
     *
     * @param length the length of the string
     * @return the random string
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates test HTML content.
     *
     * @return HTML content as string
     */
    public static String generateHtmlContent() {
        String id = generateUniqueId();
        return String.format("<!DOCTYPE html>\n" +
                           "<html>\n" +
                           "<head>\n" +
                           "    <title>Test File %s</title>\n" +
                           "</head>\n" +
                           "<body>\n" +
                           "    <h1>GCS CLI Test File</h1>\n" +
                           "    <p>This is a test file generated for GCS CLI testing.</p>\n" +
                           "    <p>ID: %s</p>\n" +
                           "    <p>Timestamp: %d</p>\n" +
                           "</body>\n" +
                           "</html>", id, id, System.currentTimeMillis());
    }

    /**
     * Generates binary content of specified size.
     *
     * @param sizeInKB the size in kilobytes
     * @return the binary content as byte array
     */
    public static byte[] generateBinaryContent(int sizeInKB) {
        byte[] content = new byte[sizeInKB * 1024];
        random.nextBytes(content);
        return content;
    }
}