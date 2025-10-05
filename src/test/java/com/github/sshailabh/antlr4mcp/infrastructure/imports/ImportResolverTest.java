package com.github.sshailabh.antlr4mcp.infrastructure.imports;

import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import com.github.sshailabh.antlr4mcp.infrastructure.cache.GrammarCacheManager;
import com.github.sshailabh.antlr4mcp.infrastructure.resources.FileSystemService;
import com.github.sshailabh.antlr4mcp.security.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImportResolverTest {

    private ImportResolver importResolver;
    private AntlrMcpProperties mockProperties;
    private FileSystemService mockFileSystemService;
    private PathValidator mockPathValidator;
    private GrammarCacheManager mockCacheManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockProperties = mock(AntlrMcpProperties.class);
        mockFileSystemService = mock(FileSystemService.class);
        mockPathValidator = mock(PathValidator.class);
        mockCacheManager = mock(GrammarCacheManager.class);

        // Setup default property mocks
        AntlrMcpProperties.FeaturesProperties featuresProps = new AntlrMcpProperties.FeaturesProperties();
        featuresProps.setImportResolution(true);
        when(mockProperties.getFeatures()).thenReturn(featuresProps);

        AntlrMcpProperties.SecurityProperties securityProps = new AntlrMcpProperties.SecurityProperties();
        securityProps.setMaxImportDepth(10);
        when(mockProperties.getSecurity()).thenReturn(securityProps);

        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(true);
        resourcesProps.setAllowedPaths(List.of(tempDir.toString()));
        resourcesProps.setAutoDiscovery(false);
        when(mockProperties.getResources()).thenReturn(resourcesProps);

        // Default cache behavior
        when(mockCacheManager.get(anyString(), anyString(), eq(ImportedGrammar.class)))
            .thenReturn(Optional.empty());

        importResolver = new ImportResolver(
            mockProperties,
            mockFileSystemService,
            mockPathValidator,
            mockCacheManager
        );
    }

    @Test
    void testExtractImportsSingle() {
        String grammar = "grammar Test;\nimport Common;\nrule: 'test';";
        List<String> imports = importResolver.extractImports(grammar);

        assertEquals(1, imports.size());
        assertEquals("Common", imports.get(0));
    }

    @Test
    void testExtractImportsMultiple() {
        String grammar = "grammar Test;\nimport Common, Lexer, Parser;\nrule: 'test';";
        List<String> imports = importResolver.extractImports(grammar);

        assertEquals(3, imports.size());
        assertTrue(imports.contains("Common"));
        assertTrue(imports.contains("Lexer"));
        assertTrue(imports.contains("Parser"));
    }

    @Test
    void testExtractImportsMultipleStatements() {
        String grammar = "grammar Test;\nimport Common;\nimport Lexer;\nrule: 'test';";
        List<String> imports = importResolver.extractImports(grammar);

        assertEquals(2, imports.size());
        assertTrue(imports.contains("Common"));
        assertTrue(imports.contains("Lexer"));
    }

    @Test
    void testExtractImportsNone() {
        String grammar = "grammar Test;\nrule: 'test';";
        List<String> imports = importResolver.extractImports(grammar);

        assertTrue(imports.isEmpty());
    }

    @Test
    void testHasImports() {
        String grammarWithImport = "grammar Test;\nimport Common;\nrule: 'test';";
        assertTrue(importResolver.hasImports(grammarWithImport));

        String grammarWithoutImport = "grammar Test;\nrule: 'test';";
        assertFalse(importResolver.hasImports(grammarWithoutImport));
    }

    @Test
    void testResolveImportFromCache() throws IOException {
        String importName = "Common";
        Path baseDir = tempDir;

        ImportedGrammar cached = ImportedGrammar.builder()
            .name(importName)
            .content("grammar Common;")
            .path(tempDir.resolve("Common.g4"))
            .uri("file://" + tempDir.resolve("Common.g4"))
            .build();

        when(mockCacheManager.get(anyString(), anyString(), eq(ImportedGrammar.class)))
            .thenReturn(Optional.of(cached));

        ImportedGrammar result = importResolver.resolveImport(importName, baseDir);

        assertNotNull(result);
        assertEquals(importName, result.getName());
        verify(mockFileSystemService, never()).loadGrammarFile(any());
    }

    @Test
    void testResolveImportFromFile() throws IOException {
        String importName = "Common";
        Path baseDir = tempDir;
        Path importPath = baseDir.resolve("Common.g4");
        String content = "grammar Common;\nrule: 'common';";

        when(mockFileSystemService.fileExists(importPath)).thenReturn(true);
        when(mockFileSystemService.loadGrammarFile(importPath)).thenReturn(content);
        doNothing().when(mockPathValidator).validatePath(any());

        ImportedGrammar result = importResolver.resolveImport(importName, baseDir);

        assertNotNull(result);
        assertEquals(importName, result.getName());
        assertEquals(content, result.getContent());
        verify(mockCacheManager).put(eq("resolvedGrammars"), anyString(), any());
    }

    @Test
    void testResolveImportNotFound() {
        String importName = "NonExistent";
        Path baseDir = tempDir;

        when(mockFileSystemService.fileExists(any())).thenReturn(false);

        assertThrows(IOException.class, () -> {
            importResolver.resolveImport(importName, baseDir);
        });
    }

    @Test
    void testResolveImportsDisabled() throws IOException {
        AntlrMcpProperties.FeaturesProperties featuresProps = new AntlrMcpProperties.FeaturesProperties();
        featuresProps.setImportResolution(false);
        when(mockProperties.getFeatures()).thenReturn(featuresProps);

        String grammar = "grammar Test;\nimport Common;\nrule: 'test';";
        Map<String, ImportedGrammar> result = importResolver.resolveImports(grammar, tempDir);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveImportsSimple() throws IOException {
        Path mainGrammar = tempDir.resolve("Test.g4");
        Path importedGrammar = tempDir.resolve("Common.g4");

        String mainContent = "grammar Test;\nimport Common;\nrule: 'test';";
        String importContent = "grammar Common;\ncommon: 'common';";

        when(mockFileSystemService.fileExists(importedGrammar)).thenReturn(true);
        when(mockFileSystemService.loadGrammarFile(importedGrammar)).thenReturn(importContent);
        doNothing().when(mockPathValidator).validatePath(any());

        Map<String, ImportedGrammar> result = importResolver.resolveImports(mainContent, mainGrammar);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Common"));
        assertEquals(importContent, result.get("Common").getContent());
    }

    @Test
    void testResolveImportsNested() throws IOException {
        Path mainGrammar = tempDir.resolve("Test.g4");
        Path imported1 = tempDir.resolve("Common.g4");
        Path imported2 = tempDir.resolve("Base.g4");

        String mainContent = "grammar Test;\nimport Common;\nrule: 'test';";
        String import1Content = "grammar Common;\nimport Base;\ncommon: 'common';";
        String import2Content = "grammar Base;\nbase: 'base';";

        when(mockFileSystemService.fileExists(imported1)).thenReturn(true);
        when(mockFileSystemService.fileExists(imported2)).thenReturn(true);
        when(mockFileSystemService.loadGrammarFile(imported1)).thenReturn(import1Content);
        when(mockFileSystemService.loadGrammarFile(imported2)).thenReturn(import2Content);
        doNothing().when(mockPathValidator).validatePath(any());

        Map<String, ImportedGrammar> result = importResolver.resolveImports(mainContent, mainGrammar);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("Common"));
        assertTrue(result.containsKey("Base"));
    }

    @Test
    void testResolveImportsCircularDependency() throws IOException {
        Path mainGrammar = tempDir.resolve("Test.g4");
        Path imported = tempDir.resolve("Common.g4");

        String mainContent = "grammar Test;\nimport Common;\nrule: 'test';";
        String importContent = "grammar Common;\nimport Test;\ncommon: 'common';";

        when(mockFileSystemService.fileExists(imported)).thenReturn(true);
        when(mockFileSystemService.loadGrammarFile(imported)).thenReturn(importContent);
        when(mockFileSystemService.fileExists(mainGrammar)).thenReturn(true);
        when(mockFileSystemService.loadGrammarFile(mainGrammar)).thenReturn(mainContent);
        doNothing().when(mockPathValidator).validatePath(any());

        assertThrows(IllegalStateException.class, () -> {
            importResolver.resolveImports(mainContent, mainGrammar);
        });
    }

    @Test
    void testResolveImportsMaxDepthExceeded() throws IOException {
        AntlrMcpProperties.SecurityProperties securityProps = new AntlrMcpProperties.SecurityProperties();
        securityProps.setMaxImportDepth(1);
        when(mockProperties.getSecurity()).thenReturn(securityProps);

        Path mainGrammar = tempDir.resolve("Test.g4");
        Path imported1 = tempDir.resolve("Common.g4");
        Path imported2 = tempDir.resolve("Base.g4");

        String mainContent = "grammar Test;\nimport Common;\nrule: 'test';";
        String import1Content = "grammar Common;\nimport Base;\ncommon: 'common';";
        String import2Content = "grammar Base;\nbase: 'base';";

        when(mockFileSystemService.fileExists(imported1)).thenReturn(true);
        when(mockFileSystemService.fileExists(imported2)).thenReturn(true);
        when(mockFileSystemService.loadGrammarFile(imported1)).thenReturn(import1Content);
        when(mockFileSystemService.loadGrammarFile(imported2)).thenReturn(import2Content);
        doNothing().when(mockPathValidator).validatePath(any());

        assertThrows(IllegalStateException.class, () -> {
            importResolver.resolveImports(mainContent, mainGrammar);
        });
    }

    @Test
    void testResolveImportsSecurityValidation() throws IOException {
        Path mainGrammar = tempDir.resolve("Test.g4");
        Path imported = tempDir.resolve("Common.g4");

        String mainContent = "grammar Test;\nimport Common;\nrule: 'test';";

        when(mockFileSystemService.fileExists(imported)).thenReturn(true);
        doThrow(new SecurityException("Invalid path"))
            .when(mockPathValidator).validatePath(imported);

        assertThrows(SecurityException.class, () -> {
            importResolver.resolveImport("Common", tempDir);
        });
    }
}
