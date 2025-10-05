package com.github.sshailabh.antlr4mcp.infrastructure.resources;

import com.github.sshailabh.antlr4mcp.security.PathValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for file system operations with security validation
 */
@Service
@Slf4j
public class FileSystemService {

    private final PathValidator pathValidator;

    @Autowired
    public FileSystemService(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    /**
     * Discover all .g4 grammar files in a directory
     *
     * @param directory Directory to search
     * @return List of grammar file paths
     */
    public List<Path> discoverGrammarFiles(Path directory) throws IOException {
        log.info("Discovering grammar files in: {}", directory);

        pathValidator.validatePath(directory);

        if (!Files.exists(directory)) {
            throw new FileNotFoundException("Directory not found: " + directory);
        }

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> grammarFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".g4"))
                .collect(Collectors.toList());

            log.info("Found {} grammar files in {}", grammarFiles.size(), directory);
            return grammarFiles;
        }
    }

    /**
     * Load grammar file content
     *
     * @param grammarPath Path to grammar file
     * @return Grammar content as string
     */
    public String loadGrammarFile(Path grammarPath) throws IOException {
        log.debug("Loading grammar file: {}", grammarPath);

        pathValidator.validatePath(grammarPath);

        if (!Files.exists(grammarPath)) {
            throw new FileNotFoundException("Grammar file not found: " + grammarPath);
        }

        if (!Files.isRegularFile(grammarPath)) {
            throw new IllegalArgumentException("Not a file: " + grammarPath);
        }

        String content = Files.readString(grammarPath, StandardCharsets.UTF_8);
        log.debug("Loaded grammar file, size: {} bytes", content.length());

        return content;
    }

    /**
     * Check if a file exists
     */
    public boolean fileExists(Path path) {
        try {
            pathValidator.validatePath(path);
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (SecurityException e) {
            log.warn("Security validation failed for path: {}", path);
            return false;
        }
    }

    /**
     * Get file name from path
     */
    public String getFileName(Path path) {
        return path.getFileName().toString();
    }

    /**
     * Get parent directory of a file
     */
    public Path getParentDirectory(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        return parent;
    }

    /**
     * Resolve a relative path against a base directory
     */
    public Path resolvePath(Path baseDirectory, String relativePath) {
        Path resolved = baseDirectory.resolve(relativePath).normalize();
        pathValidator.validatePath(resolved);
        return resolved;
    }
}
