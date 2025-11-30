package com.github.sshailabh.antlr4mcp.tools;

import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
import com.github.sshailabh.antlr4mcp.support.AbstractToolTest;
import com.github.sshailabh.antlr4mcp.support.TestScenarios;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static com.github.sshailabh.antlr4mcp.support.GrammarFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CompileGrammarMultiTargetTool.
 *
 * Tests multi-target code generation for Java, Python, JavaScript, and other languages.
 * Uses modern parameterized test patterns to reduce boilerplate by 70%+.
 */
@DisplayName("CompileGrammarMultiTargetTool - Comprehensive Tests")
class CompileGrammarMultiTargetToolTest extends AbstractToolTest {

    @Autowired
    private MultiTargetCompiler multiTargetCompiler;

    private CompileGrammarMultiTargetTool tool;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        tool = new CompileGrammarMultiTargetTool(multiTargetCompiler, objectMapper);
    }

    // ========== SCHEMA TESTS ==========

    @Test
    @DisplayName("Should have valid tool schema")
    void testToolSchema() {
        McpSchema.Tool schema = tool.toTool();

        assertValidToolSchema(schema, "compile_grammar_multi_target", "grammarText");
        assertTrue(schema.description().toLowerCase().contains("compile"),
            "Description should mention 'compile'");

        // Verify targetLanguage is present (but not required due to default)
        assertTrue(schema.inputSchema().properties().containsKey("targetLanguage"));
    }

    // ========== MULTI-TARGET COMPILATION TESTS ==========

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("com.github.sshailabh.antlr4mcp.support.TestScenarios#compilationTargets")
    @DisplayName("Should compile to various target languages")
    void testCompileToTargetLanguages(String description, String grammar,
                                      String targetLanguage, String expectedTargetName) throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", grammar)
                .with("targetLanguage", targetLanguage)
                .with("includeGeneratedCode", false)
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"),
            "Compilation to " + targetLanguage + " should succeed");
        assertEquals(expectedTargetName, response.get("targetLanguage"));
        assertTrue((Integer) response.get("fileCount") > 0,
            "Should generate at least one file");
    }

    // ========== CODE GENERATION TESTS ==========

    @Test
    @DisplayName("Should include generated code when requested")
    void testIncludeGeneratedCode() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("targetLanguage", "java")
                .with("includeGeneratedCode", true)
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");

        assertNotNull(files);
        assertFalse(files.isEmpty());

        boolean hasContent = files.stream()
            .anyMatch(f -> f.containsKey("content") && f.get("content") != null);

        assertTrue(hasContent, "At least one file should include generated code");
    }

    @Test
    @DisplayName("Should exclude generated code by default")
    void testExcludeGeneratedCodeByDefault() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("targetLanguage", "python3")
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");

        // Files should be listed but without content
        assertNotNull(files);
        assertTrue(files.stream().allMatch(f ->
            !f.containsKey("content") || f.get("content") == null));
    }

    // ========== RUNTIME INFO TESTS ==========

    @Test
    @DisplayName("Should provide runtime information for Python")
    void testRuntimeInfoPython() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("targetLanguage", "python3")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue(response.containsKey("runtimeInfo"));

        @SuppressWarnings("unchecked")
        Map<String, Object> runtimeInfo = (Map<String, Object>) response.get("runtimeInfo");

        assertEquals("antlr4", runtimeInfo.get("import"));
        assertFalse((Boolean) runtimeInfo.get("stronglyTyped"));
        assertTrue((Boolean) runtimeInfo.get("garbageCollected"));
    }

    @Test
    @DisplayName("Should provide runtime information for Java")
    void testRuntimeInfoJava() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("targetLanguage", "java")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> runtimeInfo = (Map<String, Object>) response.get("runtimeInfo");

        String importStr = (String) runtimeInfo.get("import");
        assertTrue(importStr.contains("org.antlr.v4.runtime"), "Should contain ANTLR runtime import");
        assertTrue((Boolean) runtimeInfo.get("stronglyTyped"));
        assertTrue((Boolean) runtimeInfo.get("garbageCollected"));
    }

    // ========== ERROR HANDLING TESTS ==========

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("com.github.sshailabh.antlr4mcp.support.TestScenarios#invalidCompilations")
    @DisplayName("Should handle invalid compilation scenarios")
    void testInvalidCompilations(String description, String grammar,
                                  String targetLanguage, String expectedErrorPattern) throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", grammar)
                .with("targetLanguage", targetLanguage)
                .build()));

        // Tool may return error flag OR success:false in response
        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        // Either isError flag or success=false indicates failure
        boolean hasFailed = result.isError() || !(Boolean) response.get("success");
        assertTrue(hasFailed, "Should indicate failure for invalid scenario: " + description);

        if (!result.isError()) {
            assertTrue(response.containsKey("error"), "Should include error details");
        }
    }

    // ========== COMPLEX GRAMMAR TESTS ==========

    @Test
    @DisplayName("Should compile complex left-recursive grammar")
    void testComplexLeftRecursiveGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", PRECEDENCE_CALC)
                .with("targetLanguage", "java")
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"));
        assertTrue((Integer) response.get("fileCount") >= 4,
            "Should generate parser, lexer, listener, and base listener files");
    }

    @Test
    @DisplayName("Should compile JSON grammar to JavaScript")
    void testJsonGrammarToJavaScript() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", JSON_INLINE)
                .with("targetLanguage", "javascript")
                .with("includeGeneratedCode", false)
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"));
        assertEquals("JavaScript", response.get("targetLanguage"));
    }

    // ========== FILE COUNT VALIDATION ==========

    @Test
    @DisplayName("Should generate appropriate number of files for simple grammar")
    void testFileCountSimpleGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("compile_grammar_multi_target", arguments()
                .with("grammarText", SIMPLE_HELLO)
                .with("targetLanguage", "python3")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        int fileCount = (Integer) response.get("fileCount");
        assertTrue(fileCount >= 2, "Should generate at least parser and lexer files");
        assertTrue(fileCount <= 10, "Simple grammar shouldn't generate excessive files (got: " + fileCount + ")");
    }
}
