package com.github.sshailabh.antlr4mcp.infrastructure.resources;

import com.github.sshailabh.antlr4mcp.security.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileSystemServiceTest {

    private FileSystemService fileSystemService;
    private PathValidator mockPathValidator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockPathValidator = mock(PathValidator.class);
        fileSystemService = new FileSystemService(mockPathValidator);

        // By default, allow all paths
        doNothing().when(mockPathValidator).validatePath(any(Path.class));
    }

    @Test
    void testDiscoverGrammarFiles() throws IOException {
        // Create test grammar files
        Path grammar1 = tempDir.resolve("Test1.g4");
        Path grammar2 = tempDir.resolve("Test2.g4");
        Path notGrammar = tempDir.resolve("Test.txt");

        Files.writeString(grammar1, "grammar Test1;");
        Files.writeString(grammar2, "grammar Test2;");
        Files.writeString(notGrammar, "not a grammar");

        List<Path> grammarFiles = fileSystemService.discoverGrammarFiles(tempDir);

        assertEquals(2, grammarFiles.size());
        assertTrue(grammarFiles.contains(grammar1));
        assertTrue(grammarFiles.contains(grammar2));
        assertFalse(grammarFiles.contains(notGrammar));
    }

    @Test
    void testDiscoverGrammarFilesInNonExistentDirectory() {
        Path nonExistent = tempDir.resolve("nonexistent");

        assertThrows(FileNotFoundException.class, () -> {
            fileSystemService.discoverGrammarFiles(nonExistent);
        });
    }

    @Test
    void testDiscoverGrammarFilesWithFile() throws IOException {
        Path file = tempDir.resolve("Test.g4");
        Files.writeString(file, "grammar Test;");

        assertThrows(IllegalArgumentException.class, () -> {
            fileSystemService.discoverGrammarFiles(file);
        });
    }

    @Test
    void testDiscoverGrammarFilesWithInvalidPath() {
        doThrow(new SecurityException("Invalid path"))
            .when(mockPathValidator).validatePath(any(Path.class));

        assertThrows(SecurityException.class, () -> {
            fileSystemService.discoverGrammarFiles(tempDir);
        });
    }

    @Test
    void testLoadGrammarFile() throws IOException {
        Path grammarFile = tempDir.resolve("Test.g4");
        String content = "grammar Test; rule: 'test';";
        Files.writeString(grammarFile, content);

        String loaded = fileSystemService.loadGrammarFile(grammarFile);

        assertEquals(content, loaded);
    }

    @Test
    void testLoadNonExistentGrammarFile() {
        Path nonExistent = tempDir.resolve("NonExistent.g4");

        assertThrows(FileNotFoundException.class, () -> {
            fileSystemService.loadGrammarFile(nonExistent);
        });
    }

    @Test
    void testLoadGrammarFileWithDirectory() throws IOException {
        Path directory = tempDir.resolve("subdir");
        Files.createDirectory(directory);

        assertThrows(IllegalArgumentException.class, () -> {
            fileSystemService.loadGrammarFile(directory);
        });
    }

    @Test
    void testLoadGrammarFileWithInvalidPath() throws IOException {
        Path grammarFile = tempDir.resolve("Test.g4");
        Files.writeString(grammarFile, "grammar Test;");

        doThrow(new SecurityException("Invalid path"))
            .when(mockPathValidator).validatePath(grammarFile);

        assertThrows(SecurityException.class, () -> {
            fileSystemService.loadGrammarFile(grammarFile);
        });
    }

    @Test
    void testFileExists() throws IOException {
        Path file = tempDir.resolve("Test.g4");
        Files.writeString(file, "grammar Test;");

        assertTrue(fileSystemService.fileExists(file));
    }

    @Test
    void testFileExistsWithNonExistentFile() {
        Path nonExistent = tempDir.resolve("NonExistent.g4");

        assertFalse(fileSystemService.fileExists(nonExistent));
    }

    @Test
    void testFileExistsWithDirectory() throws IOException {
        Path directory = tempDir.resolve("subdir");
        Files.createDirectory(directory);

        assertFalse(fileSystemService.fileExists(directory));
    }

    @Test
    void testFileExistsWithSecurityException() throws IOException {
        Path file = tempDir.resolve("Test.g4");
        Files.writeString(file, "grammar Test;");

        doThrow(new SecurityException("Invalid path"))
            .when(mockPathValidator).validatePath(file);

        assertFalse(fileSystemService.fileExists(file));
    }

    @Test
    void testGetFileName() throws IOException {
        Path file = tempDir.resolve("Test.g4");

        String fileName = fileSystemService.getFileName(file);

        assertEquals("Test.g4", fileName);
    }

    @Test
    void testGetParentDirectory() throws IOException {
        Path file = tempDir.resolve("Test.g4");

        Path parent = fileSystemService.getParentDirectory(file);

        assertEquals(tempDir, parent);
    }

    @Test
    void testGetParentDirectoryWithRootPath() {
        Path root = Path.of("/");

        assertThrows(IllegalArgumentException.class, () -> {
            fileSystemService.getParentDirectory(root);
        });
    }

    @Test
    void testResolvePath() {
        String relativePath = "subdir/Test.g4";

        Path resolved = fileSystemService.resolvePath(tempDir, relativePath);

        assertEquals(tempDir.resolve(relativePath).normalize(), resolved);
        verify(mockPathValidator).validatePath(resolved);
    }

    @Test
    void testResolvePathWithInvalidPath() {
        String relativePath = "../../../etc/passwd";

        doThrow(new SecurityException("Path outside allowed directories"))
            .when(mockPathValidator).validatePath(any(Path.class));

        assertThrows(SecurityException.class, () -> {
            fileSystemService.resolvePath(tempDir, relativePath);
        });
    }
}
