package com.github.sshailabh.antlr4mcp.tools;

import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.support.AbstractToolTest;
import com.github.sshailabh.antlr4mcp.support.TestScenarios;
import com.github.sshailabh.antlr4mcp.visualization.AtnVisualizer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.github.sshailabh.antlr4mcp.support.GrammarFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for VisualizeAtnTool.
 *
 * Tests ATN (Augmented Transition Network) visualization in multiple formats:
 * DOT, Mermaid, and SVG. Uses parameterized tests for format variations.
 */
@DisplayName("VisualizeAtnTool - Comprehensive Tests")
class VisualizeAtnToolTest extends AbstractToolTest {

    @Autowired
    private GrammarCompiler grammarCompiler;

    @Autowired
    private AtnVisualizer atnVisualizer;

    private VisualizeAtnTool tool;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        tool = new VisualizeAtnTool(grammarCompiler, atnVisualizer, objectMapper);
    }

    // ========== SCHEMA TESTS ==========

    @Test
    @DisplayName("Should have valid tool schema")
    void testToolSchema() {
        McpSchema.Tool schema = tool.toTool();

        assertValidToolSchema(schema, "visualize_atn", "grammarText", "ruleName");
        assertTrue(schema.description().contains("ATN") || schema.description().contains("network"));
    }

    // ========== FORMAT TESTS ==========

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("com.github.sshailabh.antlr4mcp.support.TestScenarios#visualizationFormats")
    @DisplayName("Should visualize ATN in various formats")
    void testVisualizationFormats(String description, String grammar, String ruleName,
                                   String format, String expectedKey) throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", grammar)
                .with("ruleName", ruleName)
                .with("format", format)
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"));
        assertEquals(ruleName, response.get("ruleName"));
        assertTrue((Integer) response.get("stateCount") > 0, "Should have at least one state");
        assertTrue((Integer) response.get("transitionCount") >= 0, "Should have transition count");
        assertTrue(response.containsKey(expectedKey), "Should contain " + expectedKey + " key");
    }

    @Test
    @DisplayName("Should generate DOT format only")
    void testDotFormatOnly() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", "expr")
                .with("format", "dot")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue(response.containsKey("dot"));
        assertFalse(response.containsKey("mermaid"), "Should not include mermaid for DOT-only");

        String dot = (String) response.get("dot");
        assertTrue(dot.contains("digraph") || dot.contains("->"), "Should be valid DOT format");
    }

    @Test
    @DisplayName("Should generate Mermaid format only")
    void testMermaidFormatOnly() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", "term")
                .with("format", "mermaid")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue(response.containsKey("mermaid"));
        assertFalse(response.containsKey("dot"), "Should not include DOT for Mermaid-only");

        String mermaid = (String) response.get("mermaid");
        assertTrue(mermaid.contains("stateDiagram"), "Should be valid Mermaid format");
    }

    @Test
    @DisplayName("Should generate all formats when requested")
    void testAllFormats() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", PRECEDENCE_CALC)
                .with("ruleName", "expr")
                .with("format", "all")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue(response.containsKey("dot"), "Should include DOT");
        assertTrue(response.containsKey("mermaid"), "Should include Mermaid");
        // SVG may or may not be present depending on Graphviz availability
    }

    // ========== RULE TESTS ==========

    @ParameterizedTest(name = "Rule: {0}")
    @ValueSource(strings = {"expr", "term", "factor"})
    @DisplayName("Should visualize different rules in calculator grammar")
    void testDifferentRules(String ruleName) throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", ruleName)
                .with("format", "dot")
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"));
        assertEquals(ruleName, response.get("ruleName"));
    }

    // ========== COMPLEXITY TESTS ==========

    @Test
    @DisplayName("Should handle simple grammar with minimal states")
    void testSimpleGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_HELLO)
                .with("ruleName", "start")
                .with("format", "all")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        int stateCount = (Integer) response.get("stateCount");
        assertTrue(stateCount >= 1 && stateCount <= 5,
            "Simple grammar should have 1-5 states, got: " + stateCount);
    }

    @Test
    @DisplayName("Should handle complex left-recursive grammar with many states")
    void testComplexGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", PRECEDENCE_CALC)
                .with("ruleName", "expr")
                .with("format", "all")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        int stateCount = (Integer) response.get("stateCount");
        int transitionCount = (Integer) response.get("transitionCount");

        assertTrue(stateCount >= 5, "Complex grammar should have 5+ states, got: " + stateCount);
        assertTrue(transitionCount >= 5, "Complex grammar should have 5+ transitions, got: " + transitionCount);
    }

    @Test
    @DisplayName("Should handle JSON grammar with multiple alternatives")
    void testJsonGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", JSON_INLINE)
                .with("ruleName", "value")
                .with("format", "dot")
                .build()));

        assertToolSuccess(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertTrue((Boolean) response.get("success"));
        assertTrue((Integer) response.get("stateCount") >= 7,
            "JSON 'value' rule should have multiple states for alternatives");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Should fail gracefully for non-existent rule")
    void testNonExistentRule() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", "nonexistent")
                .build()));

        assertTrue(result.isError(), "Should return error for non-existent rule");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertFalse((Boolean) response.get("success"));
        assertTrue(response.containsKey("error"));
        assertTrue(response.get("error").toString().toLowerCase().contains("rule") ||
                   response.get("error").toString().toLowerCase().contains("not found"));
    }

    @Test
    @DisplayName("Should fail gracefully for invalid grammar")
    void testInvalidGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", UNDEFINED_RULE)
                .with("ruleName", "start")
                .build()));

        assertTrue(result.isError(), "Should return error for invalid grammar");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        assertFalse((Boolean) response.get("success"));
        assertTrue(response.containsKey("error"));
    }

    @Test
    @DisplayName("Should fail gracefully for empty grammar")
    void testEmptyGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", "")
                .with("ruleName", "start")
                .build()));

        assertTrue(result.isError(), "Should return error for empty grammar");
    }

    // ========== STATE AND TRANSITION VALIDATION ==========

    @Test
    @DisplayName("Should report accurate state and transition counts")
    void testStateAndTransitionCounts() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", "factor")
                .with("format", "dot")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        int stateCount = (Integer) response.get("stateCount");
        int transitionCount = (Integer) response.get("transitionCount");

        // Factor rule: INT | '(' expr ')'
        // Should have at least 3-5 states for this simple rule with alternatives
        assertTrue(stateCount >= 2, "Factor rule should have at least 2 states");
        assertTrue(transitionCount >= 2, "Factor rule should have at least 2 transitions");
        assertTrue(transitionCount >= stateCount - 1,
            "Transitions should be at least states-1 for connected graph");
    }

    // ========== FORMAT CONTENT VALIDATION ==========

    @Test
    @DisplayName("DOT output should be well-formed")
    void testDotOutputWellFormed() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", "expr")
                .with("format", "dot")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        String dot = (String) response.get("dot");
        assertNotNull(dot);
        assertFalse(dot.isEmpty());

        // Basic DOT format validation
        assertTrue(dot.contains("digraph") || dot.contains("graph"));
        assertTrue(dot.contains("->") || dot.contains("--"));
        assertTrue(dot.contains("{") && dot.contains("}"));
    }

    @Test
    @DisplayName("Mermaid output should be well-formed")
    void testMermaidOutputWellFormed() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("visualize_atn", arguments()
                .with("grammarText", SIMPLE_CALC)
                .with("ruleName", "expr")
                .with("format", "mermaid")
                .build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = parseResult(result, Map.class);

        String mermaid = (String) response.get("mermaid");
        assertNotNull(mermaid);
        assertFalse(mermaid.isEmpty());

        // Basic Mermaid format validation
        assertTrue(mermaid.contains("stateDiagram"));
        assertTrue(mermaid.contains("-->") || mermaid.contains(":"));
    }
}
