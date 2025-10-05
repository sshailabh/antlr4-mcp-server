package com.github.sshailabh.antlr4mcp.codegen;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("MultiTargetCompiler Integration Tests")
class MultiTargetCompilerIntegrationTest {

    @Autowired
    private MultiTargetCompiler compiler;

    private String simpleGrammar;
    private String calculatorGrammar;
    private String grammarWithEmbeddedCode;

    @BeforeEach
    void setUp() {
        simpleGrammar = """
            grammar Simple;
            start : 'hello' 'world' ;
            """;

        calculatorGrammar = """
            grammar Calculator;

            expr : expr '+' term
                 | expr '-' term
                 | term
                 ;

            term : term '*' factor
                 | term '/' factor
                 | factor
                 ;

            factor : INT
                   | '(' expr ')'
                   ;

            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        grammarWithEmbeddedCode = """
            grammar JavaCode;

            @header {
                import java.util.*;
            }

            @members {
                private int count = 0;
            }

            expr : INT { count++; } ;
            INT : [0-9]+ ;
            """;
    }

    @Test
    @DisplayName("Compile simple grammar for Java")
    void testCompileSimpleGrammarForJava() {
        CompilationResult result = compiler.compileForTarget(simpleGrammar, TargetLanguage.JAVA);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertEquals(TargetLanguage.JAVA, result.getTargetLanguage());
        assertTrue(result.hasGeneratedFiles(), "Should generate files");
        assertTrue(result.getGeneratedFileCount() > 0, "Should have at least one file");

        // Check for expected files
        List<GeneratedFile> files = result.getGeneratedFiles();
        boolean hasLexer = files.stream().anyMatch(f -> f.getFileType().equals("lexer"));
        boolean hasParser = files.stream().anyMatch(f -> f.getFileType().equals("parser"));

        assertTrue(hasLexer, "Should generate lexer");
        assertTrue(hasParser, "Should generate parser");
    }

    @Test
    @DisplayName("Compile simple grammar for Python3")
    void testCompileSimpleGrammarForPython() {
        CompilationResult result = compiler.compileForTarget(simpleGrammar, TargetLanguage.PYTHON3);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertEquals(TargetLanguage.PYTHON3, result.getTargetLanguage());
        assertTrue(result.hasGeneratedFiles(), "Should generate files");

        // Check file extensions
        List<GeneratedFile> files = result.getGeneratedFiles();
        boolean hasPythonFile = files.stream()
            .anyMatch(f -> f.getFileName().endsWith(".py"));

        assertTrue(hasPythonFile, "Should generate .py files");
    }

    @Test
    @DisplayName("Compile simple grammar for JavaScript")
    void testCompileSimpleGrammarForJavaScript() {
        CompilationResult result = compiler.compileForTarget(simpleGrammar, TargetLanguage.JAVASCRIPT);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertEquals(TargetLanguage.JAVASCRIPT, result.getTargetLanguage());
        assertTrue(result.hasGeneratedFiles(), "Should generate files");

        // Check file extensions
        List<GeneratedFile> files = result.getGeneratedFiles();
        boolean hasJsFile = files.stream()
            .anyMatch(f -> f.getFileName().endsWith(".js"));

        assertTrue(hasJsFile, "Should generate .js files");
    }

    @Test
    @DisplayName("Compile calculator grammar for multiple targets")
    void testCompileCalculatorForMultipleTargets() {
        TargetLanguage[] targets = {
            TargetLanguage.JAVA,
            TargetLanguage.PYTHON3,
            TargetLanguage.JAVASCRIPT
        };

        for (TargetLanguage target : targets) {
            CompilationResult result = compiler.compileForTarget(calculatorGrammar, target);

            assertTrue(result.isSuccess(),
                "Compilation should succeed for " + target.getDisplayName());
            assertTrue(result.hasGeneratedFiles(),
                "Should generate files for " + target.getDisplayName());
            assertTrue(result.getGeneratedFileCount() >= 4,
                "Should generate multiple files for " + target.getDisplayName());
        }
    }

    @Test
    @DisplayName("Compile grammar with embedded Java code for Java")
    void testCompileGrammarWithEmbeddedCode() {
        CompilationResult result = compiler.compileForTarget(
            grammarWithEmbeddedCode, TargetLanguage.JAVA);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertTrue(result.hasGeneratedFiles(), "Should generate files");

        // Check that generated code contains embedded code
        List<GeneratedFile> files = result.getGeneratedFiles();
        boolean hasEmbeddedCode = files.stream()
            .anyMatch(f -> f.getContent() != null &&
                          f.getContent().contains("private int count"));

        assertTrue(hasEmbeddedCode, "Generated code should contain embedded code");
    }

    @Test
    @DisplayName("Handle invalid grammar gracefully")
    void testInvalidGrammar() {
        String invalidGrammar = """
            grammar Invalid;
            expr : INT [ ;
            INT : [0-9]+ ;
            """;

        CompilationResult result = compiler.compileForTarget(invalidGrammar, TargetLanguage.JAVA);

        assertFalse(result.isSuccess(), "Compilation should fail for invalid grammar");
        assertFalse(result.getErrors().isEmpty(), "Should have error messages");
    }

    @Test
    @DisplayName("Handle missing grammar declaration")
    void testMissingGrammarDeclaration() {
        String noGrammar = """
            expr : INT ;
            INT : [0-9]+ ;
            """;

        CompilationResult result = compiler.compileForTarget(noGrammar, TargetLanguage.JAVA);

        assertFalse(result.isSuccess(), "Should fail without grammar declaration");
    }

    @Test
    @DisplayName("Verify generated file metadata")
    void testGeneratedFileMetadata() {
        CompilationResult result = compiler.compileForTarget(calculatorGrammar, TargetLanguage.JAVA);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        List<GeneratedFile> files = result.getGeneratedFiles();

        for (GeneratedFile file : files) {
            assertNotNull(file.getFileName(), "File name should not be null");
            assertNotNull(file.getFileType(), "File type should not be null");
            assertNotNull(file.getContent(), "Content should not be null");
            assertNotNull(file.getTargetLanguage(), "Target language should not be null");
            assertTrue(file.getLineCount() > 0, "Line count should be positive");
            assertTrue(file.getSize() > 0, "Size should be positive");
        }
    }

    @Test
    @DisplayName("Test TargetLanguage enum parsing")
    void testTargetLanguageParsing() {
        assertEquals(TargetLanguage.JAVA, TargetLanguage.fromString("java"));
        assertEquals(TargetLanguage.PYTHON3, TargetLanguage.fromString("python3"));
        assertEquals(TargetLanguage.JAVASCRIPT, TargetLanguage.fromString("javascript"));
        assertEquals(TargetLanguage.CPP, TargetLanguage.fromString("cpp"));

        // Case insensitive
        assertEquals(TargetLanguage.JAVA, TargetLanguage.fromString("JAVA"));
        assertEquals(TargetLanguage.PYTHON3, TargetLanguage.fromString("Python3"));

        // Default on null
        assertEquals(TargetLanguage.JAVA, TargetLanguage.fromString(null));

        // Exception on unknown
        assertThrows(IllegalArgumentException.class,
            () -> TargetLanguage.fromString("unknown"));
    }

    @Test
    @DisplayName("Verify language-specific properties")
    void testLanguageProperties() {
        assertTrue(TargetLanguage.JAVA.isStronglyTyped());
        assertFalse(TargetLanguage.PYTHON3.isStronglyTyped());
        assertTrue(TargetLanguage.TYPESCRIPT.isStronglyTyped());

        assertFalse(TargetLanguage.CPP.isGarbageCollected());
        assertTrue(TargetLanguage.JAVA.isGarbageCollected());
        assertTrue(TargetLanguage.PYTHON3.isGarbageCollected());

        assertEquals("org.antlr.v4.runtime", TargetLanguage.JAVA.getRuntimeImport());
        assertEquals("antlr4", TargetLanguage.PYTHON3.getRuntimeImport());
        assertEquals("antlr4", TargetLanguage.JAVASCRIPT.getRuntimeImport());
    }

    @Test
    @DisplayName("Compile with TypeScript target")
    void testTypeScriptCompilation() {
        CompilationResult result = compiler.compileForTarget(simpleGrammar, TargetLanguage.TYPESCRIPT);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertTrue(result.hasGeneratedFiles(), "Should generate files");

        List<GeneratedFile> files = result.getGeneratedFiles();
        boolean hasTsFile = files.stream()
            .anyMatch(f -> f.getFileName().endsWith(".ts"));

        assertTrue(hasTsFile, "Should generate .ts files");
    }

    @Test
    @DisplayName("Compile with C++ target")
    void testCppCompilation() {
        CompilationResult result = compiler.compileForTarget(simpleGrammar, TargetLanguage.CPP);

        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertTrue(result.hasGeneratedFiles(), "Should generate files");

        List<GeneratedFile> files = result.getGeneratedFiles();
        boolean hasCppFile = files.stream()
            .anyMatch(f -> f.getFileName().endsWith(".cpp"));
        boolean hasHeaderFile = files.stream()
            .anyMatch(f -> f.getFileName().endsWith(".h"));

        assertTrue(hasCppFile || hasHeaderFile, "Should generate C++ source or header files");
    }
}
