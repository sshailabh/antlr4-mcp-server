package com.github.sshailabh.antlr4mcp.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility for file system operations.
 *
 * Consolidates duplicate directory deletion implementations across the codebase.
 * Provides both File and Path-based APIs for compatibility.
 */
@Slf4j
public final class FileSystemUtils {

    private FileSystemUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Delete directory recursively using NIO for proper error handling.
     * Uses reverse-ordered deletion to handle nested directories.
     * This is the preferred method for new code.
     *
     * @param directory Directory to delete
     * @throws IOException if deletion fails
     */
    public static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         log.warn("Failed to delete: {}", path, e);
                     }
                 });
        }
    }

    /**
     * Delete directory recursively using File API for legacy compatibility.
     * 
     * @param directory Directory to delete
     */
    public static void deleteDirectoryRecursively(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    if (!file.delete()) {
                        log.warn("Failed to delete file: {}", file.getPath());
                    }
                }
            }
        }
        
        if (!directory.delete()) {
            log.warn("Failed to delete directory: {}", directory.getPath());
        }
    }

    /**
     * Securely delete directory with symlink protection.
     * Prevents escaping the intended directory through symlinks.
     *
     * @param directory Directory to delete
     */
    public static void secureDeleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Check for symlinks to prevent escaping the temp directory
                try {
                    if (Files.isSymbolicLink(file.toPath())) {
                        log.warn("Skipping symlink during cleanup: {}", file.getPath());
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Could not check symlink status for: {}", file.getPath());
                    continue;
                }

                if (file.isDirectory()) {
                    secureDeleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.warn("Failed to delete file: {}", file.getPath());
                    }
                }
            }
        }

        if (!directory.delete()) {
            log.warn("Failed to delete directory: {}", directory.getPath());
        }
    }
}
