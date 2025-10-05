package com.github.sshailabh.antlr4mcp.security;

import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates file system paths to prevent path traversal attacks and
 * ensure access is restricted to allowed directories.
 */
@Component
@Slf4j
public class PathValidator {

    private final List<Path> allowedPaths;
    private final boolean enabled;

    @Autowired
    public PathValidator(AntlrMcpProperties properties) {
        this.enabled = properties.getSecurity().isSanitizePaths();
        this.allowedPaths = properties.getResources().getAllowedPaths()
            .stream()
            .map(Paths::get)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .collect(Collectors.toList());

        log.info("PathValidator initialized with {} allowed paths: {}",
                allowedPaths.size(), allowedPaths);
    }

    /**
     * Check if a path is within allowed directories
     */
    public boolean isPathAllowed(Path path) {
        if (!enabled) {
            return true; // Security disabled, allow all paths
        }

        if (path == null) {
            return false;
        }

        Path normalizedPath = path.toAbsolutePath().normalize();

        boolean allowed = allowedPaths.stream()
            .anyMatch(allowedPath -> normalizedPath.startsWith(allowedPath));

        if (!allowed) {
            log.warn("Path access denied: {} (not in allowed paths)", normalizedPath);
        }

        return allowed;
    }

    /**
     * Validate a path, throwing exception if not allowed
     */
    public void validatePath(Path path) {
        if (!enabled) {
            return; // Security disabled
        }

        if (path == null) {
            throw new SecurityException("Path cannot be null");
        }

        // Check for path traversal attempts
        String pathStr = path.toString();
        if (pathStr.contains("..") || pathStr.contains("~")) {
            log.error("Path traversal attempt detected: {}", pathStr);
            throw new SecurityException("Path traversal attempt detected: " + path);
        }

        // Check if path is in allowed directories
        if (!isPathAllowed(path)) {
            log.error("Path not in allowed directories: {}", path);
            throw new SecurityException("Path not allowed: " + path);
        }

        log.debug("Path validation passed: {}", path);
    }

    /**
     * Validate a URI string representing a file path
     */
    public void validateUriPath(String uriString) {
        if (!enabled) {
            return;
        }

        if (uriString == null || uriString.isEmpty()) {
            throw new SecurityException("URI cannot be null or empty");
        }

        // Extract path from file:// URI
        if (uriString.startsWith("file://")) {
            String pathStr = uriString.substring("file://".length());
            Path path = Paths.get(pathStr);
            validatePath(path);
        } else {
            throw new SecurityException("Only file:// URIs are supported");
        }
    }

    /**
     * Get list of allowed paths for informational purposes
     */
    public List<Path> getAllowedPaths() {
        return List.copyOf(allowedPaths);
    }

    /**
     * Check if path validation is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
