package com.github.sshailabh.antlr4mcp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * Manages resource limits for the MCP server to prevent resource exhaustion attacks.
 * Handles output size limits, process timeouts, and memory management.
 * Can be disabled via antlr.security.resource-limits.enabled property.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "antlr.security.resource-limits", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResourceManager {

    @Value("${antlr.security.resource-limits.enabled:true}")
    private boolean resourceLimitsEnabled;

    @Value("${antlr.max-response-size-kb:50}")
    private int maxResponseSizeKb = 50;

    @Value("${antlr.compilation-timeout-seconds:30}")
    private int compilationTimeoutSeconds = 30;

    @Value("${antlr.max-input-size-mb:1}")
    private int maxInputSizeMb = 1;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Limits output to configured maximum size.
     *
     * @param output The output string to limit
     * @return Limited output string with truncation message if needed
     */
    public String limitOutput(String output) {
        if (!resourceLimitsEnabled) {
            log.debug("Resource limits disabled, returning output without truncation");
            return output;
        }

        if (output == null) {
            return null;
        }

        int maxBytes = maxResponseSizeKb * 1024;
        if (output.length() > maxBytes) {
            log.info("Output truncated from {} to {} bytes", output.length(), maxBytes);
            return output.substring(0, maxBytes) + "\n... (output truncated after " + maxResponseSizeKb + "KB)";
        }
        return output;
    }

    /**
     * Validates input size is within limits.
     *
     * @param input The input to validate
     * @param inputType Description of the input type for error messages
     * @throws SecurityException if input exceeds size limit
     */
    public void validateInputSize(String input, String inputType) {
        if (!resourceLimitsEnabled) {
            log.debug("Resource limits disabled, skipping input size validation");
            return;
        }

        if (input == null) {
            return;
        }

        int maxBytes = maxInputSizeMb * 1024 * 1024;
        if (input.length() > maxBytes) {
            log.warn("{} exceeds size limit: {} bytes (max: {} bytes)", inputType, input.length(), maxBytes);
            throw new SecurityException(inputType + " exceeds maximum size limit of " + maxInputSizeMb + "MB");
        }
    }

    /**
     * Executes a process with timeout and output limiting.
     *
     * @param processBuilder The process builder configured with the command
     * @return Process output (limited to max size)
     * @throws IOException If process execution fails
     * @throws TimeoutException If process exceeds timeout
     */
    public ProcessResult executeWithTimeout(ProcessBuilder processBuilder) throws IOException, TimeoutException {
        Process process = processBuilder.start();

        Future<String> outputFuture = executorService.submit(() -> {
            try {
                return readLimitedOutput(process.getInputStream());
            } catch (IOException e) {
                log.error("Error reading process output", e);
                return "Error reading output: " + e.getMessage();
            }
        });

        Future<String> errorFuture = executorService.submit(() -> {
            try {
                return readLimitedOutput(process.getErrorStream());
            } catch (IOException e) {
                log.error("Error reading process error stream", e);
                return "Error reading error stream: " + e.getMessage();
            }
        });

        try {
            boolean finished = process.waitFor(compilationTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                errorFuture.cancel(true);
                log.warn("Process timeout after {} seconds", compilationTimeoutSeconds);
                throw new TimeoutException("Process exceeded timeout of " + compilationTimeoutSeconds + " seconds");
            }

            String output = outputFuture.get(1, TimeUnit.SECONDS);
            String error = errorFuture.get(1, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            return new ProcessResult(exitCode, output, error);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Process execution interrupted", e);
        } catch (ExecutionException e) {
            process.destroyForcibly();
            throw new IOException("Error executing process", e.getCause());
        } catch (TimeoutException e) {
            outputFuture.cancel(true);
            errorFuture.cancel(true);
            throw e;
        }
    }

    /**
     * Reads output from a stream with size limiting.
     */
    private String readLimitedOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        int maxBytes = maxResponseSizeKb * 1024;
        boolean truncated = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() + line.length() + 1 > maxBytes) {
                    truncated = true;
                    break;
                }
                output.append(line).append("\n");
            }
        }

        if (truncated) {
            output.append("\n... (output truncated after ").append(maxResponseSizeKb).append("KB)");
        }

        return output.toString();
    }

    /**
     * Gets the configured compilation timeout.
     */
    public int getCompilationTimeoutSeconds() {
        return compilationTimeoutSeconds;
    }

    /**
     * Gets the configured maximum response size in KB.
     */
    public int getMaxResponseSizeKb() {
        return maxResponseSizeKb;
    }

    /**
     * Result of process execution.
     */
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String errorOutput;

        public ProcessResult(int exitCode, String output, String errorOutput) {
            this.exitCode = exitCode;
            this.output = output;
            this.errorOutput = errorOutput;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public String getErrorOutput() {
            return errorOutput;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    /**
     * Cleanup method to shutdown executor service.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}