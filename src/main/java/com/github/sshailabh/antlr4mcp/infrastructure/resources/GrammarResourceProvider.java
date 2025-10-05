package com.github.sshailabh.antlr4mcp.infrastructure.resources;

import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import com.github.sshailabh.antlr4mcp.security.PathValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides MCP Resources for ANTLR4 grammar files
 */
@Component
@Slf4j
public class GrammarResourceProvider {

    private final AntlrMcpProperties properties;
    private final PathValidator pathValidator;
    private final FileSystemService fileSystemService;

    @Autowired
    public GrammarResourceProvider(AntlrMcpProperties properties,
                                    PathValidator pathValidator,
                                    FileSystemService fileSystemService) {
        this.properties = properties;
        this.pathValidator = pathValidator;
        this.fileSystemService = fileSystemService;
    }

    /**
     * List all .g4 files in configured directories
     *
     * @return List of resource descriptors
     */
    public List<GrammarResource> listResources() {
        if (!properties.getResources().isEnabled()) {
            log.info("Resources are disabled");
            return List.of();
        }

        log.info("Listing grammar resources from: {}",
                 properties.getResources().getAllowedPaths());

        List<GrammarResource> resources = new ArrayList<>();
        for (String allowedPath : properties.getResources().getAllowedPaths()) {
            try {
                Path directory = Paths.get(allowedPath);
                if (!Files.exists(directory)) {
                    log.warn("Allowed path does not exist: {}", allowedPath);
                    continue;
                }

                List<Path> grammarFiles = fileSystemService.discoverGrammarFiles(directory);
                for (Path grammarPath : grammarFiles) {
                    GrammarResource resource = GrammarResource.builder()
                        .uri("file://" + grammarPath.toAbsolutePath())
                        .name(grammarPath.getFileName().toString())
                        .path(grammarPath.toAbsolutePath().toString())
                        .mimeType("text/x-antlr-grammar")
                        .description("ANTLR4 Grammar File")
                        .build();
                    resources.add(resource);
                }
            } catch (IOException e) {
                log.error("Failed to list resources in: {}", allowedPath, e);
            }
        }

        log.info("Found {} grammar resources", resources.size());
        return resources;
    }

    /**
     * Read grammar content from URI
     *
     * @param uri Resource URI (file:// scheme)
     * @return Grammar content
     */
    public String readResource(String uri) throws IOException {
        if (!properties.getResources().isEnabled()) {
            throw new IllegalStateException("Resources are disabled");
        }

        log.info("Reading grammar resource: {}", uri);

        // Parse URI
        URI resourceUri = URI.create(uri);
        if (!"file".equals(resourceUri.getScheme())) {
            throw new IllegalArgumentException("Only file:// URIs supported, got: " + uri);
        }

        // Extract path and validate
        String pathStr = resourceUri.getPath();
        Path path = Paths.get(pathStr);

        // Validate path is in allowed directories
        pathValidator.validatePath(path);

        // Read file
        String content = fileSystemService.loadGrammarFile(path);
        log.debug("Read resource, size: {} bytes", content.length());

        return content;
    }

    /**
     * Check if a URI represents a valid grammar resource
     */
    public boolean isValidResourceUri(String uri) {
        if (!properties.getResources().isEnabled()) {
            return false;
        }

        try {
            URI resourceUri = URI.create(uri);
            if (!"file".equals(resourceUri.getScheme())) {
                return false;
            }

            Path path = Paths.get(resourceUri.getPath());
            return pathValidator.isPathAllowed(path) &&
                   Files.exists(path) &&
                   path.toString().endsWith(".g4");
        } catch (Exception e) {
            return false;
        }
    }
}
