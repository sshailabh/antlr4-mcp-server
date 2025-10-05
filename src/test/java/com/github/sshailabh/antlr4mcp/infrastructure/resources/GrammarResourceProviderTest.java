package com.github.sshailabh.antlr4mcp.infrastructure.resources;

import com.github.sshailabh.antlr4mcp.config.AntlrMcpProperties;
import com.github.sshailabh.antlr4mcp.security.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrammarResourceProviderTest {

    private GrammarResourceProvider resourceProvider;
    private AntlrMcpProperties mockProperties;
    private PathValidator mockPathValidator;
    private FileSystemService mockFileSystemService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockProperties = mock(AntlrMcpProperties.class);
        mockPathValidator = mock(PathValidator.class);
        mockFileSystemService = mock(FileSystemService.class);

        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(true);
        resourcesProps.setAllowedPaths(List.of(tempDir.toString()));

        when(mockProperties.getResources()).thenReturn(resourcesProps);

        resourceProvider = new GrammarResourceProvider(
            mockProperties,
            mockPathValidator,
            mockFileSystemService
        );
    }

    @Test
    void testListResourcesWhenDisabled() {
        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(false);
        when(mockProperties.getResources()).thenReturn(resourcesProps);

        List<GrammarResource> resources = resourceProvider.listResources();

        assertTrue(resources.isEmpty());
    }

    @Test
    void testListResourcesWithValidPath() throws IOException {
        Path grammar1 = tempDir.resolve("Test1.g4");
        Path grammar2 = tempDir.resolve("Test2.g4");

        when(mockFileSystemService.discoverGrammarFiles(any()))
            .thenReturn(List.of(grammar1, grammar2));

        List<GrammarResource> resources = resourceProvider.listResources();

        assertEquals(2, resources.size());
        assertEquals("Test1.g4", resources.get(0).getName());
        assertEquals("Test2.g4", resources.get(1).getName());
        assertEquals("text/x-antlr-grammar", resources.get(0).getMimeType());
    }

    @Test
    void testListResourcesWithNonExistentPath() throws IOException {
        Path nonExistent = tempDir.resolve("nonexistent");

        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(true);
        resourcesProps.setAllowedPaths(List.of(nonExistent.toString()));
        when(mockProperties.getResources()).thenReturn(resourcesProps);

        List<GrammarResource> resources = resourceProvider.listResources();

        assertTrue(resources.isEmpty());
    }

    @Test
    void testListResourcesWithIOException() throws IOException {
        when(mockFileSystemService.discoverGrammarFiles(any()))
            .thenThrow(new IOException("Test exception"));

        List<GrammarResource> resources = resourceProvider.listResources();

        assertTrue(resources.isEmpty());
    }

    @Test
    void testReadResourceWhenDisabled() {
        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(false);
        when(mockProperties.getResources()).thenReturn(resourcesProps);

        String uri = "file://" + tempDir.resolve("Test.g4");

        assertThrows(IllegalStateException.class, () -> {
            resourceProvider.readResource(uri);
        });
    }

    @Test
    void testReadResourceWithValidUri() throws IOException {
        Path grammarFile = tempDir.resolve("Test.g4");
        String uri = "file://" + grammarFile.toAbsolutePath();
        String content = "grammar Test;";

        when(mockFileSystemService.loadGrammarFile(any()))
            .thenReturn(content);
        doNothing().when(mockPathValidator).validatePath(any());

        String result = resourceProvider.readResource(uri);

        assertEquals(content, result);
        verify(mockPathValidator).validatePath(any());
    }

    @Test
    void testReadResourceWithInvalidScheme() {
        String uri = "http://example.com/Test.g4";

        assertThrows(IllegalArgumentException.class, () -> {
            resourceProvider.readResource(uri);
        });
    }

    @Test
    void testReadResourceWithInvalidPath() {
        Path grammarFile = tempDir.resolve("Test.g4");
        String uri = "file://" + grammarFile.toAbsolutePath();

        doThrow(new SecurityException("Invalid path"))
            .when(mockPathValidator).validatePath(any());

        assertThrows(SecurityException.class, () -> {
            resourceProvider.readResource(uri);
        });
    }

    @Test
    void testIsValidResourceUriWhenDisabled() {
        AntlrMcpProperties.ResourcesProperties resourcesProps = new AntlrMcpProperties.ResourcesProperties();
        resourcesProps.setEnabled(false);
        when(mockProperties.getResources()).thenReturn(resourcesProps);

        String uri = "file://" + tempDir.resolve("Test.g4");

        assertFalse(resourceProvider.isValidResourceUri(uri));
    }

    @Test
    void testIsValidResourceUriWithValidUri() throws IOException {
        Path grammarFile = tempDir.resolve("Test.g4");
        Files.writeString(grammarFile, "grammar Test;");

        String uri = "file://" + grammarFile.toAbsolutePath();

        when(mockPathValidator.isPathAllowed(any())).thenReturn(true);

        boolean result = resourceProvider.isValidResourceUri(uri);

        assertTrue(result);
    }

    @Test
    void testIsValidResourceUriWithInvalidScheme() {
        String uri = "http://example.com/Test.g4";

        assertFalse(resourceProvider.isValidResourceUri(uri));
    }

    @Test
    void testIsValidResourceUriWithNonG4Extension() throws IOException {
        Path textFile = tempDir.resolve("Test.txt");
        Files.writeString(textFile, "not a grammar");

        String uri = "file://" + textFile.toAbsolutePath();

        when(mockPathValidator.isPathAllowed(any())).thenReturn(true);

        boolean result = resourceProvider.isValidResourceUri(uri);

        assertFalse(result);
    }

    @Test
    void testIsValidResourceUriWithPathNotAllowed() throws IOException {
        Path grammarFile = tempDir.resolve("Test.g4");
        Files.writeString(grammarFile, "grammar Test;");

        String uri = "file://" + grammarFile.toAbsolutePath();

        when(mockPathValidator.isPathAllowed(any())).thenReturn(false);

        boolean result = resourceProvider.isValidResourceUri(uri);

        assertFalse(result);
    }

    @Test
    void testIsValidResourceUriWithNonExistentFile() {
        Path nonExistent = tempDir.resolve("NonExistent.g4");
        String uri = "file://" + nonExistent.toAbsolutePath();

        when(mockPathValidator.isPathAllowed(any())).thenReturn(true);

        boolean result = resourceProvider.isValidResourceUri(uri);

        assertFalse(result);
    }
}
