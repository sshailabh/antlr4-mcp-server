package com.github.sshailabh.antlr4mcp.infrastructure.imports;

import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import com.github.sshailabh.antlr4mcp.infrastructure.cache.GrammarCacheManager;
import com.github.sshailabh.antlr4mcp.infrastructure.resources.FileSystemService;
import com.github.sshailabh.antlr4mcp.security.PathValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves ANTLR grammar imports and manages import dependencies
 */
@Service
@Slf4j
public class ImportResolver {

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "import\\s+([A-Za-z][A-Za-z0-9_,\\s]*)\\s*;"
    );

    private final AntlrMcpProperties properties;
    private final FileSystemService fileSystemService;
    private final PathValidator pathValidator;
    private final GrammarCacheManager cacheManager;

    @Autowired
    public ImportResolver(AntlrMcpProperties properties,
                         FileSystemService fileSystemService,
                         PathValidator pathValidator,
                         GrammarCacheManager cacheManager) {
        this.properties = properties;
        this.fileSystemService = fileSystemService;
        this.pathValidator = pathValidator;
        this.cacheManager = cacheManager;
    }

    /**
     * Resolve all imports in a grammar
     *
     * @param grammarContent Main grammar content
     * @param grammarPath    Path to main grammar (for resolving relative imports)
     * @return Map of import name to ImportedGrammar
     */
    public Map<String, ImportedGrammar> resolveImports(String grammarContent, Path grammarPath)
        throws IOException {

        if (!properties.getFeatures().isImportResolution()) {
            log.debug("Import resolution is disabled");
            return Collections.emptyMap();
        }

        log.info("Resolving imports for grammar at: {}", grammarPath);

        ImportGraph importGraph = new ImportGraph(properties.getSecurity().getMaxImportDepth());
        Map<String, ImportedGrammar> resolved = new HashMap<>();

        String mainGrammarName = extractGrammarName(grammarContent);
        resolveImportsRecursive(
            mainGrammarName,
            grammarContent,
            grammarPath,
            importGraph,
            resolved,
            0
        );

        log.info("Resolved {} imports for {}", resolved.size(), mainGrammarName);
        return resolved;
    }

    /**
     * Extract import statements from grammar content
     *
     * @param grammarContent Grammar content
     * @return List of imported grammar names
     */
    public List<String> extractImports(String grammarContent) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(grammarContent);

        while (matcher.find()) {
            String importList = matcher.group(1);
            // Split comma-separated imports
            String[] names = importList.split("\\s*,\\s*");
            for (String name : names) {
                imports.add(name.trim());
            }
        }

        log.debug("Extracted {} imports", imports.size());
        return imports;
    }

    /**
     * Check if grammar has imports
     */
    public boolean hasImports(String grammarContent) {
        return IMPORT_PATTERN.matcher(grammarContent).find();
    }

    /**
     * Resolve a single imported grammar
     *
     * @param importName   Name of imported grammar
     * @param baseDir      Base directory for resolving relative paths
     * @return Imported grammar
     */
    public ImportedGrammar resolveImport(String importName, Path baseDir) throws IOException {
        log.debug("Resolving import: {} from {}", importName, baseDir);

        // Check cache first
        String cacheKey = importName + ":" + baseDir.toString();
        Optional<ImportedGrammar> cached = cacheManager.get(
            "resolvedGrammars",
            cacheKey,
            ImportedGrammar.class
        );

        if (cached.isPresent()) {
            log.debug("Cache hit for import: {}", importName);
            return cached.get();
        }

        // Try to find the grammar file
        Path grammarPath = findGrammarFile(importName, baseDir);
        if (grammarPath == null) {
            throw new IOException("Could not find imported grammar: " + importName);
        }

        // Validate path security
        pathValidator.validatePath(grammarPath);

        // Load grammar content
        String content = fileSystemService.loadGrammarFile(grammarPath);

        ImportedGrammar imported = ImportedGrammar.builder()
            .name(importName)
            .content(content)
            .path(grammarPath)
            .uri("file://" + grammarPath.toAbsolutePath())
            .build();

        // Cache the result
        cacheManager.put("resolvedGrammars", cacheKey, imported);

        log.debug("Resolved import {} to {}", importName, grammarPath);
        return imported;
    }

    /**
     * Recursively resolve imports
     */
    private void resolveImportsRecursive(
        String grammarName,
        String grammarContent,
        Path grammarPath,
        ImportGraph importGraph,
        Map<String, ImportedGrammar> resolved,
        int depth
    ) throws IOException {

        if (depth > properties.getSecurity().getMaxImportDepth()) {
            throw new IllegalStateException(
                "Import depth exceeds maximum: " + properties.getSecurity().getMaxImportDepth()
            );
        }

        List<String> imports = extractImports(grammarContent);
        if (imports.isEmpty()) {
            return;
        }

        Path baseDir = grammarPath != null ? grammarPath.getParent() : Paths.get(".");

        for (String importName : imports) {
            // Check for circular dependencies
            if (importGraph.wouldCreateCycle(grammarName, importName)) {
                throw new IllegalStateException(
                    "Circular import detected: " + grammarName + " -> " + importName
                );
            }

            // Skip if already resolved
            if (resolved.containsKey(importName)) {
                importGraph.addDependency(grammarName, importName);
                continue;
            }

            // Resolve the import
            ImportedGrammar imported = resolveImport(importName, baseDir);
            resolved.put(importName, imported);
            importGraph.addDependency(grammarName, importName);

            // Recursively resolve nested imports
            resolveImportsRecursive(
                importName,
                imported.getContent(),
                imported.getPath(),
                importGraph,
                resolved,
                depth + 1
            );
        }

        // Validate depth after all imports are processed
        importGraph.validateDepth(grammarName);
    }

    /**
     * Find a grammar file by name
     */
    private Path findGrammarFile(String grammarName, Path baseDir) throws IOException {
        // Try direct path in base directory
        Path directPath = baseDir.resolve(grammarName + ".g4");
        if (fileSystemService.fileExists(directPath)) {
            return directPath;
        }

        // Try searching in allowed paths
        if (properties.getResources().isEnabled()) {
            for (String allowedPath : properties.getResources().getAllowedPaths()) {
                Path allowedDir = Paths.get(allowedPath);
                Path candidate = allowedDir.resolve(grammarName + ".g4");

                if (fileSystemService.fileExists(candidate)) {
                    return candidate;
                }

                // Try subdirectories if auto-discovery is enabled
                if (properties.getResources().isAutoDiscovery()) {
                    List<Path> discoveredPaths = fileSystemService.discoverGrammarFiles(allowedDir);
                    for (Path discoveredPath : discoveredPaths) {
                        if (fileSystemService.getFileName(discoveredPath)
                            .equals(grammarName + ".g4")) {
                            return discoveredPath;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Extract grammar name from content
     */
    private String extractGrammarName(String grammarContent) {
        Pattern pattern = Pattern.compile(
            "(lexer\\s+grammar|parser\\s+grammar|grammar)\\s+([A-Za-z][A-Za-z0-9_]*)\\s*;"
        );
        Matcher matcher = pattern.matcher(grammarContent);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "Unknown";
    }
}
