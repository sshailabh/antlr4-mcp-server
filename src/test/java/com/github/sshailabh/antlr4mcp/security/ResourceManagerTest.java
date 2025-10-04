package com.github.sshailabh.antlr4mcp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Resource Manager Tests")
class ResourceManagerTest {

    private ResourceManager resourceManager;

    @BeforeEach
    void setUp() {
        resourceManager = new ResourceManager();
        // Set test-friendly limits via reflection
        ReflectionTestUtils.setField(resourceManager, "resourceLimitsEnabled", true); // Enable limits for testing
        ReflectionTestUtils.setField(resourceManager, "maxResponseSizeKb", 1); // 1KB for testing
        ReflectionTestUtils.setField(resourceManager, "compilationTimeoutSeconds", 2); // 2 seconds for testing
        ReflectionTestUtils.setField(resourceManager, "maxInputSizeMb", 1); // 1MB for testing
    }

    @Test
    @DisplayName("Should limit output to configured size")
    void testOutputLimiting() {
        String largeOutput = "A".repeat(2048); // 2KB
        String limited = resourceManager.limitOutput(largeOutput);

        assertTrue(limited.length() < largeOutput.length());
        assertTrue(limited.contains("output truncated"));
        assertTrue(limited.length() <= 1024 + 100); // 1KB + truncation message
    }

    @Test
    @DisplayName("Should not truncate output within limits")
    void testOutputWithinLimits() {
        String smallOutput = "This is a small output";
        String result = resourceManager.limitOutput(smallOutput);

        assertEquals(smallOutput, result);
        assertFalse(result.contains("truncated"));
    }

    @Test
    @DisplayName("Should handle null output gracefully")
    void testNullOutput() {
        assertNull(resourceManager.limitOutput(null));
    }

    @Test
    @DisplayName("Should validate input size limits")
    void testInputSizeValidation() {
        String smallInput = "Small input";
        assertDoesNotThrow(() ->
            resourceManager.validateInputSize(smallInput, "Test input"));

        String largeInput = "X".repeat(2 * 1024 * 1024); // 2MB
        SecurityException exception = assertThrows(SecurityException.class, () ->
            resourceManager.validateInputSize(largeInput, "Test input"));

        assertTrue(exception.getMessage().contains("exceeds maximum size limit"));
    }

    @Test
    @DisplayName("Should execute process successfully within timeout")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testProcessExecutionSuccess() throws IOException, TimeoutException {
        ProcessBuilder pb = new ProcessBuilder("echo", "Hello World");
        ResourceManager.ProcessResult result = resourceManager.executeWithTimeout(pb);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("Hello World"));
    }

    @Test
    @DisplayName("Should timeout long-running processes")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testProcessTimeout() {
        // Set very short timeout for this test
        ReflectionTestUtils.setField(resourceManager, "compilationTimeoutSeconds", 1);

        ProcessBuilder pb = new ProcessBuilder("sleep", "10"); // Sleep for 10 seconds
        assertThrows(TimeoutException.class, () ->
            resourceManager.executeWithTimeout(pb));
    }

    @Test
    @DisplayName("Should capture process error output")
    void testProcessErrorOutput() throws IOException, TimeoutException {
        ProcessBuilder pb = new ProcessBuilder("ls", "/nonexistent/directory");
        ResourceManager.ProcessResult result = resourceManager.executeWithTimeout(pb);

        assertFalse(result.isSuccess());
        assertNotEquals(0, result.getExitCode());
        assertTrue(result.getErrorOutput().length() > 0 ||
                  result.getOutput().contains("cannot access") ||
                  result.getOutput().contains("No such file"));
    }

    @Test
    @DisplayName("Should limit process output size")
    void testProcessOutputLimiting() throws IOException, TimeoutException {
        // Generate large output
        String command = System.getProperty("os.name").toLowerCase().contains("win")
            ? "cmd.exe"
            : "sh";
        String script = System.getProperty("os.name").toLowerCase().contains("win")
            ? "/c for /L %i in (1,1,1000) do @echo This is a line of output"
            : "-c 'for i in {1..1000}; do echo \"This is a line of output\"; done'";

        ProcessBuilder pb = new ProcessBuilder(command, script);
        ResourceManager.ProcessResult result = resourceManager.executeWithTimeout(pb);

        // Output should be limited
        assertTrue(result.getOutput().length() <= 1024 + 100); // 1KB + truncation message
        if (result.getOutput().length() > 100) {
            assertTrue(result.getOutput().contains("truncated"));
        }
    }

    @Test
    @DisplayName("Should handle process execution errors gracefully")
    void testProcessExecutionError() {
        ProcessBuilder pb = new ProcessBuilder("/nonexistent/command");

        assertThrows(IOException.class, () ->
            resourceManager.executeWithTimeout(pb));
    }

    @Test
    @DisplayName("Should validate input size for null input")
    void testValidateNullInput() {
        assertDoesNotThrow(() ->
            resourceManager.validateInputSize(null, "Test input"));
    }

    @Test
    @DisplayName("Should respect configured limits")
    void testConfiguredLimits() {
        assertEquals(2, resourceManager.getCompilationTimeoutSeconds());
        assertEquals(1, resourceManager.getMaxResponseSizeKb());
    }

    @Test
    @DisplayName("Should handle empty output correctly")
    void testEmptyOutput() {
        String empty = "";
        String result = resourceManager.limitOutput(empty);
        assertEquals(empty, result);
    }

    @Test
    @DisplayName("Should handle output exactly at limit")
    void testOutputAtExactLimit() {
        String exactLimit = "A".repeat(1024); // Exactly 1KB
        String result = resourceManager.limitOutput(exactLimit);

        assertEquals(exactLimit, result);
        assertFalse(result.contains("truncated"));
    }

    @Test
    @DisplayName("Should handle output one byte over limit")
    void testOutputOneBytOverLimit() {
        String overLimit = "A".repeat(1025); // 1KB + 1 byte
        String result = resourceManager.limitOutput(overLimit);

        // Verify truncation occurred
        assertTrue(result.contains("truncated"),
            "Expected result to contain 'truncated'");
        // The content before the truncation message should be exactly 1024 bytes (the limit)
        int truncationMsgStart = result.indexOf("\n...");
        assertTrue(truncationMsgStart > 0 && truncationMsgStart <= 1024,
            "Content before truncation message should be <= 1024 bytes, got: " + truncationMsgStart);
    }
}