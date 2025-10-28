package com.gcs.testing.models;

/**
 * Represents the result of a CLI command execution.
 */
public class CommandResult {
    private final String command;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final long executionTimeMs;

    public CommandResult(String command, int exitCode, String stdout, String stderr, long executionTimeMs) {
        this.command = command;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Gets the command that was executed.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets the exit code of the command.
     * @return 0 for success, non-zero for failure
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Gets the standard output of the command.
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Gets the standard error output of the command.
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Gets the execution time in milliseconds.
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Checks if the command execution was successful.
     * @return true if exit code is 0, false otherwise
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }

    /**
     * Gets the combined output (stdout + stderr).
     */
    public String getCombinedOutput() {
        StringBuilder sb = new StringBuilder();
        if (stdout != null && !stdout.isEmpty()) {
            sb.append(stdout);
        }
        if (stderr != null && !stderr.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(stderr);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("CommandResult{command='%s', exitCode=%d, executionTimeMs=%d, stdout='%s', stderr='%s'}",
                command, exitCode, executionTimeMs,
                stdout != null ? stdout.substring(0, Math.min(stdout.length(), 100)) + "..." : "",
                stderr != null ? stderr.substring(0, Math.min(stderr.length(), 100)) + "..." : "");
    }
}