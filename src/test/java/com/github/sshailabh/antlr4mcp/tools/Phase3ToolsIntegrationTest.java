package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.codegen.MultiTargetCompiler;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.visualization.AtnVisualizer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Phase 3 MCP Tools Integration Tests")
class Phase3ToolsIntegrationTest {

    @Autowired
    private MultiTargetCompiler multiTargetCompiler;

    @Autowired
    private GrammarCompiler grammarCompiler;

    @Autowired
    private AtnVisualizer atnVisualizer;

    @Autowired
    private ObjectMapper objectMapper;

    private CompileGrammarMultiTargetTool compileMultiTargetTool;
    private VisualizeAtnTool visualizeAtnTool;

    private String calculatorGrammar;
    private String expressionGrammar;

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @BeforeEach
    void setUp() {
        compileMultiTargetTool = new CompileGrammarMultiTargetTool(
            multiTargetCompiler, objectMapper
        );

        visualizeAtnTool = new VisualizeAtnTool(
            grammarCompiler, atnVisualizer, objectMapper
        );

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

        expressionGrammar = """
            grammar Expr;
            expr : term (('+' | '-') term)* ;
            term : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;
    }

    @Test
    @DisplayName("Test compile_grammar_multi_target tool - Java")
    void testCompileMultiTargetToolJava() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", calculatorGrammar);
        arguments.put("targetLanguage", "java");
        arguments.put("includeGeneratedCode", false);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "compile_grammar_multi_target",
            arguments
        );

        McpSchema.CallToolResult result = compileMultiTargetTool.execute(null, request);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isError(), "Should not be an error");

        // Extract text from MCP content
        String jsonContent = getContentText(result);

        // Parse result JSON
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"), "Compilation should succeed");
        assertEquals("Java", response.get("targetLanguage"));
        assertTrue((Integer) response.get("fileCount") > 0, "Should generate files");
    }

    @Test
    @DisplayName("Test compile_grammar_multi_target tool - Python")
    void testCompileMultiTargetToolPython() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", calculatorGrammar);
        arguments.put("targetLanguage", "python3");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "compile_grammar_multi_target",
            arguments
        );

        McpSchema.CallToolResult result = compileMultiTargetTool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));
        assertEquals("Python3", response.get("targetLanguage"));
    }

    @Test
    @DisplayName("Test compile_grammar_multi_target tool - JavaScript")
    void testCompileMultiTargetToolJavaScript() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", calculatorGrammar);
        arguments.put("targetLanguage", "javascript");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "compile_grammar_multi_target",
            arguments
        );

        McpSchema.CallToolResult result = compileMultiTargetTool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));
        assertEquals("JavaScript", response.get("targetLanguage"));
    }

    @Test
    @DisplayName("Test compile_grammar_multi_target with include code")
    void testCompileMultiTargetWithCode() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", expressionGrammar);
        arguments.put("targetLanguage", "java");
        arguments.put("includeGeneratedCode", true);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "compile_grammar_multi_target",
            arguments
        );

        McpSchema.CallToolResult result = compileMultiTargetTool.execute(null, request);

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> files =
            (java.util.List<Map<String, Object>>) response.get("files");

        // At least one file should have content
        boolean hasContent = files.stream()
            .anyMatch(f -> f.containsKey("content") && f.get("content") != null);

        assertTrue(hasContent, "Should include generated code");
    }

    @Test
    @DisplayName("Test compile_grammar_multi_target with invalid language")
    void testCompileMultiTargetInvalidLanguage() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", calculatorGrammar);
        arguments.put("targetLanguage", "invalid_lang");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "compile_grammar_multi_target",
            arguments
        );

        McpSchema.CallToolResult result = compileMultiTargetTool.execute(null, request);

        assertTrue(result.isError(), "Should return error for invalid language");

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertFalse((Boolean) response.get("success"));
        assertTrue(response.containsKey("error"));
    }

    @Test
    @DisplayName("Test visualize_atn tool - DOT format")
    void testVisualizeAtnToolDot() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", expressionGrammar);
        arguments.put("ruleName", "expr");
        arguments.put("format", "dot");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "visualize_atn",
            arguments
        );

        McpSchema.CallToolResult result = visualizeAtnTool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));
        assertEquals("expr", response.get("ruleName"));
        assertTrue((Integer) response.get("stateCount") > 0);
        assertTrue((Integer) response.get("transitionCount") > 0);
        assertTrue(response.containsKey("dot"));
        assertFalse(response.containsKey("mermaid"));
    }

    @Test
    @DisplayName("Test visualize_atn tool - Mermaid format")
    void testVisualizeAtnToolMermaid() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", expressionGrammar);
        arguments.put("ruleName", "expr");
        arguments.put("format", "mermaid");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "visualize_atn",
            arguments
        );

        McpSchema.CallToolResult result = visualizeAtnTool.execute(null, request);

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));
        assertTrue(response.containsKey("mermaid"));
        assertFalse(response.containsKey("dot"));

        String mermaid = (String) response.get("mermaid");
        assertTrue(mermaid.contains("stateDiagram-v2"));
    }

    @Test
    @DisplayName("Test visualize_atn tool - All formats")
    void testVisualizeAtnToolAllFormats() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", expressionGrammar);
        arguments.put("ruleName", "expr");
        arguments.put("format", "all");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "visualize_atn",
            arguments
        );

        McpSchema.CallToolResult result = visualizeAtnTool.execute(null, request);

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));
        assertTrue(response.containsKey("dot"));
        assertTrue(response.containsKey("mermaid"));
        // SVG may or may not be present depending on Graphviz availability
    }

    @Test
    @DisplayName("Test visualize_atn tool with invalid rule")
    void testVisualizeAtnToolInvalidRule() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", expressionGrammar);
        arguments.put("ruleName", "nonexistent");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "visualize_atn",
            arguments
        );

        McpSchema.CallToolResult result = visualizeAtnTool.execute(null, request);

        assertTrue(result.isError(), "Should return error for invalid rule");

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertFalse((Boolean) response.get("success"));
        assertTrue(response.containsKey("error"));
    }

    @Test
    @DisplayName("Test tool schemas are valid")
    void testToolSchemas() {
        McpSchema.Tool compileSchema = compileMultiTargetTool.toTool();
        McpSchema.Tool atnSchema = visualizeAtnTool.toTool();

        assertNotNull(compileSchema);
        assertEquals("compile_grammar_multi_target", compileSchema.name());
        assertNotNull(compileSchema.description());
        assertNotNull(compileSchema.inputSchema());

        assertNotNull(atnSchema);
        assertEquals("visualize_atn", atnSchema.name());
        assertNotNull(atnSchema.description());
        assertNotNull(atnSchema.inputSchema());
    }

    @Test
    @DisplayName("Test compile tool runtime info")
    void testCompileToolRuntimeInfo() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", expressionGrammar);
        arguments.put("targetLanguage", "python3");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "compile_grammar_multi_target",
            arguments
        );

        McpSchema.CallToolResult result = compileMultiTargetTool.execute(null, request);

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue(response.containsKey("runtimeInfo"));

        @SuppressWarnings("unchecked")
        Map<String, Object> runtimeInfo =
            (Map<String, Object>) response.get("runtimeInfo");

        assertEquals("antlr4", runtimeInfo.get("import"));
        assertFalse((Boolean) runtimeInfo.get("stronglyTyped"));
        assertTrue((Boolean) runtimeInfo.get("garbageCollected"));
    }

    @Test
    @DisplayName("Test visualize_atn with complex grammar")
    void testVisualizeAtnComplexGrammar() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammarText", calculatorGrammar);
        arguments.put("ruleName", "expr");
        arguments.put("format", "all");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            "visualize_atn",
            arguments
        );

        McpSchema.CallToolResult result = visualizeAtnTool.execute(null, request);

        String jsonContent = getContentText(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(
            jsonContent, Map.class
        );

        assertTrue((Boolean) response.get("success"));

        int stateCount = (Integer) response.get("stateCount");
        int transitionCount = (Integer) response.get("transitionCount");

        // Complex grammar should have more states/transitions
        assertTrue(stateCount >= 5, "Should have multiple states");
        assertTrue(transitionCount >= 5, "Should have multiple transitions");
    }
}
