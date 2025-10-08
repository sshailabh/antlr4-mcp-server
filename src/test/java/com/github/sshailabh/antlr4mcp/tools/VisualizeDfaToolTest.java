package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.DecisionVisualization;
import com.github.sshailabh.antlr4mcp.service.DecisionVisualizer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * TDD tests for VisualizeDfaTool.
 * Tests define expected behavior before implementation.
 */
@SpringBootTest
class VisualizeDfaToolTest {

    @Autowired
    private DecisionVisualizer decisionVisualizer;

    @Autowired
    private ObjectMapper objectMapper;

    private VisualizeDfaTool visualizeDfaTool;
    private McpSyncServerExchange mockExchange;

    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        expr : term (('+' | '-') term)* ;
        term : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String COMPLEX_GRAMMAR = """
        grammar Complex;
        expr : term (('+' | '-') term)* ;
        term : factor (('*' | '/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        visualizeDfaTool = new VisualizeDfaTool(decisionVisualizer, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool tool = visualizeDfaTool.toTool();

        assertNotNull(tool, "Tool should not be null");
        assertEquals("visualize_dfa", tool.name(), "Tool name should be visualize_dfa");
        assertNotNull(tool.description(), "Description should not be null");
        assertTrue(tool.description().contains("decision"), "Description should mention decision");
        assertNotNull(tool.inputSchema(), "Input schema should not be null");
    }

    @Test
    void testVisualizeDecisionsInSimpleGrammar() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", SIMPLE_GRAMMAR);
        arguments.put("rule_name", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isError(), "Result should not be an error");

        String contentText = getContentText(result);
        DecisionVisualization viz = objectMapper.readValue(contentText, DecisionVisualization.class);

        assertNotNull(viz, "Visualization should not be null");
        assertEquals("expr", viz.getRuleName(), "Rule name should match");
        assertTrue(viz.getTotalDecisions() > 0, "expr rule should have decisions");
        assertFalse(viz.getDecisions().isEmpty(), "Should have decision points");
    }

    @Test
    void testVisualizeDecisionsInComplexGrammar() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", COMPLEX_GRAMMAR);
        arguments.put("rule_name", "factor");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        DecisionVisualization viz = objectMapper.readValue(contentText, DecisionVisualization.class);

        assertNotNull(viz);
        assertEquals("factor", viz.getRuleName());
        assertTrue(viz.getTotalDecisions() > 0, "factor rule should have decisions");
        assertEquals(1, viz.getDecisions().size(), "factor should have 1 decision");

        // Check decision details
        var decision = viz.getDecisions().get(0);
        assertNotNull(decision.getDotFormat(), "DOT format should be generated");
        assertTrue(decision.getAlternativeCount() >= 2, "Should have multiple alternatives");
    }

    @Test
    void testMultipleRulesHaveDecisions() throws Exception {
        // Test expr rule
        Map<String, Object> exprArgs = new HashMap<>();
        exprArgs.put("grammar_text", COMPLEX_GRAMMAR);
        exprArgs.put("rule_name", "expr");

        McpSchema.CallToolRequest exprRequest = new McpSchema.CallToolRequest("visualize_dfa", exprArgs);
        McpSchema.CallToolResult exprResult = visualizeDfaTool.execute(mockExchange, exprRequest);

        String exprText = getContentText(exprResult);
        DecisionVisualization exprViz = objectMapper.readValue(exprText, DecisionVisualization.class);
        assertTrue(exprViz.getTotalDecisions() > 0, "expr should have decisions");

        // Test term rule
        Map<String, Object> termArgs = new HashMap<>();
        termArgs.put("grammar_text", COMPLEX_GRAMMAR);
        termArgs.put("rule_name", "term");

        McpSchema.CallToolRequest termRequest = new McpSchema.CallToolRequest("visualize_dfa", termArgs);
        McpSchema.CallToolResult termResult = visualizeDfaTool.execute(mockExchange, termRequest);

        String termText = getContentText(termResult);
        DecisionVisualization termViz = objectMapper.readValue(termText, DecisionVisualization.class);
        assertTrue(termViz.getTotalDecisions() > 0, "term should have decisions");
    }

    @Test
    void testInvalidRuleName() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", SIMPLE_GRAMMAR);
        arguments.put("rule_name", "nonexistent");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertTrue(result.isError(), "Should return error for nonexistent rule");
    }

    @Test
    void testNullGrammarText() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", null);
        arguments.put("rule_name", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertTrue(result.isError(), "Should return error for null grammar");
    }

    @Test
    void testEmptyGrammarText() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", "");
        arguments.put("rule_name", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertTrue(result.isError(), "Should return error for empty grammar");
    }

    @Test
    void testMissingArguments() throws Exception {
        // Missing rule_name
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", SIMPLE_GRAMMAR);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertTrue(result.isError(), "Should return error when rule_name is missing");
    }

    @Test
    void testRuleWithNoDecisions() throws Exception {
        String simpleTokenGrammar = """
            grammar SimpleToken;
            expr : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", simpleTokenGrammar);
        arguments.put("rule_name", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        DecisionVisualization viz = objectMapper.readValue(contentText, DecisionVisualization.class);

        assertNotNull(viz);
        assertEquals("expr", viz.getRuleName());
        assertEquals(0, viz.getTotalDecisions(), "Simple rule should have no decisions");
        assertTrue(viz.getDecisions().isEmpty());
    }

    @Test
    void testDotFormatContent() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", COMPLEX_GRAMMAR);
        arguments.put("rule_name", "factor");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_dfa", arguments);
        McpSchema.CallToolResult result = visualizeDfaTool.execute(mockExchange, request);

        String contentText = getContentText(result);
        DecisionVisualization viz = objectMapper.readValue(contentText, DecisionVisualization.class);

        assertFalse(viz.getDecisions().isEmpty());
        String dotFormat = viz.getDecisions().get(0).getDotFormat();

        assertNotNull(dotFormat);
        assertTrue(dotFormat.startsWith("digraph"), "DOT should start with digraph");
        assertTrue(dotFormat.contains("->"), "DOT should contain transitions");
        assertTrue(dotFormat.trim().endsWith("}"), "DOT should end with }");
    }
}
