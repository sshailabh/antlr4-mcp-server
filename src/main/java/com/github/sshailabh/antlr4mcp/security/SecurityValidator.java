package com.github.sshailabh.antlr4mcp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Security validation utility to prevent injection attacks and path traversal.
 * Validates grammar names, rule names, and file paths according to strict patterns.
 * Can be disabled via antlr.security.validation.enabled property.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "antlr.security.validation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityValidator {

    @Value("${antlr.security.validation.enabled:true}")
    private boolean validationEnabled;

    // Grammar name must start with letter, followed by letters, digits, or underscore, max 50 chars
    private static final Pattern GRAMMAR_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,49}$");

    // Rule name follows similar pattern but can start with lowercase
    private static final Pattern RULE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][A-Za-z0-9_]{0,49}$");

    // File extension validation
    private static final Pattern SAFE_FILE_EXTENSION = Pattern.compile("^[A-Za-z0-9]+$");

    // Dangerous characters that could be used for injection
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[;&|`$(){}\\[\\]<>\\\\\"']");

    /**
     * Validates a grammar name to prevent command injection.
     *
     * @param name The grammar name to validate
     * @throws SecurityException if the name is invalid or potentially malicious
     */
    public void validateGrammarName(String name) {
        if (!validationEnabled) {
            log.debug("Security validation disabled, skipping grammar name validation");
            return;
        }

        if (name == null || name.isEmpty()) {
            throw new SecurityException("Grammar name cannot be null or empty");
        }

        if (name.length() > 50) {
            throw new SecurityException("Grammar name exceeds maximum length of 50 characters");
        }

        if (!GRAMMAR_NAME_PATTERN.matcher(name).matches()) {
            log.warn("Invalid grammar name attempted: {}", sanitizeForLog(name));
            throw new SecurityException("Invalid grammar name format. Must start with a letter and contain only letters, digits, and underscores");
        }

        if (DANGEROUS_CHARS.matcher(name).find()) {
            log.warn("Potential injection attempt in grammar name: {}", sanitizeForLog(name));
            throw new SecurityException("Grammar name contains forbidden characters");
        }
    }

    /**
     * Validates a rule name to prevent command injection.
     *
     * @param name The rule name to validate
     * @throws SecurityException if the name is invalid or potentially malicious
     */
    public void validateRuleName(String name) {
        if (!validationEnabled) {
            log.debug("Security validation disabled, skipping rule name validation");
            return;
        }

        if (name == null || name.isEmpty()) {
            throw new SecurityException("Rule name cannot be null or empty");
        }

        if (name.length() > 50) {
            throw new SecurityException("Rule name exceeds maximum length of 50 characters");
        }

        if (!RULE_NAME_PATTERN.matcher(name).matches()) {
            log.warn("Invalid rule name attempted: {}", sanitizeForLog(name));
            throw new SecurityException("Invalid rule name format. Must start with a letter and contain only letters, digits, and underscores");
        }

        if (DANGEROUS_CHARS.matcher(name).find()) {
            log.warn("Potential injection attempt in rule name: {}", sanitizeForLog(name));
            throw new SecurityException("Rule name contains forbidden characters");
        }
    }

    /**
     * Validates that a path stays within the allowed base directory.
     * Prevents path traversal attacks.
     *
     * @param path The path to validate
     * @param baseDir The base directory that the path must be within
     * @throws SecurityException if path traversal is detected
     */
    public void validatePath(Path path, Path baseDir) {
        if (!validationEnabled) {
            log.debug("Security validation disabled, skipping path validation");
            return;
        }

        if (path == null || baseDir == null) {
            throw new SecurityException("Path validation requires non-null paths");
        }

        // Normalize paths to resolve .. and . components
        Path normalizedPath = path.normalize().toAbsolutePath();
        Path normalizedBase = baseDir.normalize().toAbsolutePath();

        if (!normalizedPath.startsWith(normalizedBase)) {
            log.warn("Path traversal attempt detected: {} not within {}",
                     sanitizeForLog(normalizedPath.toString()),
                     sanitizeForLog(normalizedBase.toString()));
            throw new SecurityException("Path traversal detected - access denied");
        }

        // Additional check for symlinks - only if the file exists
        try {
            if (java.nio.file.Files.exists(path)) {
                Path realPath = path.toRealPath();
                Path realBase = baseDir.toRealPath();
                if (!realPath.startsWith(realBase)) {
                    log.warn("Symlink escape attempt detected: {}", sanitizeForLog(realPath.toString()));
                    throw new SecurityException("Symlink escape detected - access denied");
                }
            }
            // If file doesn't exist yet, just check the normalized paths
        } catch (java.io.IOException e) {
            // If we can't resolve the real path but normalized path is OK, allow it
            log.debug("Could not resolve real path for: {}, using normalized path check", sanitizeForLog(path.toString()));
        }
    }

    /**
     * Validates a file extension to ensure it's safe.
     *
     * @param extension The file extension to validate (without the dot)
     * @throws SecurityException if the extension is invalid
     */
    public void validateFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return; // No extension is acceptable
        }

        if (!SAFE_FILE_EXTENSION.matcher(extension).matches()) {
            log.warn("Invalid file extension attempted: {}", sanitizeForLog(extension));
            throw new SecurityException("Invalid file extension");
        }
    }

    /**
     * Sanitizes input for safe logging to prevent log injection.
     *
     * @param input The input to sanitize
     * @return Sanitized string safe for logging
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        // Replace newlines and control characters
        return input.replaceAll("[\\r\\n\\t]", "_")
                   .replaceAll("[\\p{Cntrl}]", "?");
    }

    /**
     * Validates that a string doesn't contain shell metacharacters.
     *
     * @param input The input to validate
     * @throws SecurityException if dangerous characters are found
     */
    public void validateNoShellMetacharacters(String input) {
        if (input == null) {
            return;
        }

        if (DANGEROUS_CHARS.matcher(input).find()) {
            log.warn("Shell metacharacters detected in input");
            throw new SecurityException("Input contains forbidden shell metacharacters");
        }
    }
}