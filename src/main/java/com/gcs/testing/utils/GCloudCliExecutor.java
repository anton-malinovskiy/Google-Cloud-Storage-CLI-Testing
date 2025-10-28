package com.gcs.testing.utils;

import com.gcs.testing.config.TestConfig;
import com.gcs.testing.models.CommandResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes gcloud CLI commands and parses their output.
 */
public class GCloudCliExecutor {
    private static final Logger logger = LoggerFactory.getLogger(GCloudCliExecutor.class);
    private static final Gson gson = new Gson();

    /**
     * Executes a command and returns the result.
     *
     * @param command the command to execute
     * @return the command result
     */
    public static CommandResult executeCommand(String command) {
        logger.info("Executing command: {}", command);
        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            // Use absolute path to bash to avoid PATH issues in Maven Surefire
            String bashPath = System.getenv("SHELL");
            if (bashPath == null || bashPath.isEmpty()) {
                bashPath = "/bin/bash"; // Default for macOS/Linux
            }

            // Check if bash exists, otherwise try sh
            java.io.File bashFile = new java.io.File(bashPath);
            if (!bashFile.exists()) {
                bashPath = "/bin/sh";
            }

            // Set up environment with extended PATH to include gcloud
            java.util.Map<String, String> env = processBuilder.environment();
            String currentPath = env.get("PATH");
            if (currentPath == null) {
                currentPath = "";
            }

            // Add common gcloud installation paths
            String homeDir = System.getProperty("user.home");
            String extendedPath = currentPath + ":" +
                    homeDir + "/google-cloud-sdk/bin:" +
                    "/usr/local/bin:" +
                    "/opt/homebrew/bin:" +
                    "/usr/bin:" +
                    "/bin";

            env.put("PATH", extendedPath);

            processBuilder.command(bashPath, "-c", command);
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            // Read stdout
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }

            // Read stderr
            StringBuilder stderr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            // Wait for process to complete
            boolean finished = process.waitFor(TestConfig.getCommandTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Command timed out after " + TestConfig.getCommandTimeoutSeconds() + " seconds");
            }

            int exitCode = process.exitValue();
            long executionTime = System.currentTimeMillis() - startTime;

            CommandResult result = new CommandResult(command, exitCode, stdout.toString().trim(),
                                                   stderr.toString().trim(), executionTime);

            if (result.isSuccess()) {
                logger.info("Command executed successfully in {} ms", executionTime);
            } else {
                logger.warn("Command failed with exit code: {}", exitCode);
                logger.warn("Stderr: {}", result.getStderr());
            }

            return result;

        } catch (IOException | InterruptedException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Error executing command", e);
            return new CommandResult(command, -1, "", e.getMessage(), executionTime);
        }
    }

    /**
     * Generates a signed URL for a GCS file.
     *
     * @param filePath the GCS file path (gs://bucket/file)
     * @param durationMinutes the duration in minutes
     * @return the signed URL
     */
    public static String generateSignedUrl(String filePath, int durationMinutes) {
        String command = String.format("gcloud storage sign-url %s --duration=%dm",
                                     filePath, durationMinutes);

        CommandResult result = executeCommand(command);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to generate signed URL: " + result.getStderr());
        }

        // Parse the output to extract the URL
        String output = result.getStdout();
        String[] lines = output.split("\n");

        // gcloud storage sign-url returns format like:
        // http_verb: GET
        // expiration: 2024-01-01T00:00:00Z
        // url: https://storage.googleapis.com/...

        // Look for the line that starts with "signed_url:"
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("signed_url:")) {
                return line.substring("signed_url:".length()).trim();
            }
            // Also check for "url:" format
            if (line.startsWith("url:")) {
                return line.substring("url:".length()).trim();
            }
            // Also check if line is just a URL
            if (line.startsWith("http://") || line.startsWith("https://")) {
                return line;
            }
        }

        throw new RuntimeException("Could not parse signed URL from output: " + output);
    }

    /**
     * Copies a file to/from GCS.
     *
     * @param source the source path
     * @param destination the destination path
     * @return the command result
     */
    public static CommandResult copyFile(String source, String destination) {
        // If source is a local file and destination is GCS, ensure proper quoting for spaces
        if (!source.startsWith("gs://") && destination.startsWith("gs://")) {
            // Local to GCS - quote source if it contains spaces
            if (source.contains(" ")) {
                source = "\"" + source + "\"";
            }
        }
        // If source is GCS and destination is local, quote destination if needed
        if (source.startsWith("gs://") && !destination.startsWith("gs://")) {
            if (destination.contains(" ")) {
                destination = "\"" + destination + "\"";
            }
        }

        String command = String.format("gcloud storage cp %s %s", source, destination);
        return executeCommand(command);
    }

    /**
     * Lists files in a GCS bucket.
     *
     * @param bucketPath the bucket path (gs://bucket or gs://bucket/prefix)
     * @return list of file paths
     */
    public static List<String> listBucket(String bucketPath) {
        return listBucket(bucketPath, false);
    }

    /**
     * Lists files in a GCS bucket with optional recursive flag.
     *
     * @param bucketPath the bucket path (gs://bucket or gs://bucket/prefix)
     * @param recursive if true, lists recursively
     * @return list of file paths
     */
    public static List<String> listBucket(String bucketPath, boolean recursive) {
        // gcloud storage ls with -r for recursive listing
        String command = recursive ?
            String.format("gcloud storage ls -r %s", bucketPath) :
            String.format("gcloud storage ls %s", bucketPath);
        CommandResult result = executeCommand(command);

        if (!result.isSuccess()) {
            // Check if it's just an empty result (which is not an error)
            if (result.getStderr().contains("No URLs matched") ||
                result.getStderr().contains("One or more URLs matched no objects") ||
                result.getStderr().contains("matched no objects")) {
                return new ArrayList<>();
            }
            throw new RuntimeException("Failed to list bucket: " + result.getStderr());
        }

        List<String> files = new ArrayList<>();

        // Parse the output line by line
        String[] lines = result.getStdout().split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.startsWith("gs://")) {
                // Skip directory markers (lines ending with :)
                if (!line.endsWith(":")) {
                    files.add(line);
                }
            }
        }

        return files;
    }

    /**
     * Deletes a file from GCS.
     *
     * @param filePath the file path to delete (gs://bucket/file)
     * @return the command result
     */
    public static CommandResult deleteFile(String filePath) {
        String command = String.format("gcloud storage rm %s", filePath);
        return executeCommand(command);
    }

    /**
     * Checks if gcloud CLI is available.
     *
     * @return true if gcloud is available, false otherwise
     */
    public static boolean isGcloudAvailable() {
        try {
            CommandResult result = executeCommand("gcloud --version");
            return result.isSuccess();
        } catch (Exception e) {
            logger.error("gcloud CLI not available", e);
            return false;
        }
    }

    /**
     * Checks if the user is authenticated with gcloud.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        try {
            CommandResult result = executeCommand("gcloud auth list --format=json");
            if (result.isSuccess()) {
                JsonArray accounts = gson.fromJson(result.getStdout(), JsonArray.class);
                return accounts != null && accounts.size() > 0;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to check authentication status", e);
            return false;
        }
    }

    /**
     * Executes a command with retry logic for flaky operations.
     *
     * @param command the command to execute
     * @param maxAttempts the maximum number of attempts
     * @return the command result
     */
    public static CommandResult executeCommandWithRetry(String command, int maxAttempts) {
        CommandResult result = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            result = executeCommand(command);

            if (result.isSuccess()) {
                return result;
            }

            if (attempt < maxAttempts) {
                logger.warn("Command failed on attempt {}/{}, retrying...", attempt, maxAttempts);
                try {
                    Thread.sleep(TestConfig.getRetryDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return result;
    }
}