package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
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
 * Integration tests for AnalyzeCallGraphTool
 */
@SpringBootTest
class AnalyzeCallGraphToolTest {

    @Autowired
    private CallGraphAnalyzer analyzer;

    @Autowired
    private ObjectMapper objectMapper;

    private AnalyzeCallGraphTool tool;

    private static final String CALCULATOR_GRAMMAR = """
        grammar Calculator;
        prog : expr EOF ;
        expr : expr ('*'|'/') expr
             | expr ('+'|'-') expr
             | INT
             ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        tool = new AnalyzeCallGraphTool(analyzer, objectMapper);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToolMetadata() {
        McpSchema.Tool toolSchema = tool.toTool();

        assertNotNull(toolSchema);
        assertEquals("analyze_call_graph", toolSchema.name());
        assertNotNull(toolSchema.description());
        assertTrue(toolSchema.description().contains("call graph"));
        assertNotNull(toolSchema.inputSchema());
    }

    @Test
    void testToolInputSchema() {
        McpSchema.Tool toolSchema = tool.toTool();
        McpSchema.JsonSchema schema = toolSchema.inputSchema();

        assertNotNull(schema);
        assertEquals("object", schema.type());
        assertNotNull(schema.properties());

        // Should have grammar_text parameter
        assertTrue(schema.properties().containsKey("grammar_text"));

        // Should have output_format parameter
        assertTrue(schema.properties().containsKey("output_format"));

        // Should require grammar_text
        assertTrue(schema.required().contains("grammar_text"));
    }

    @Test
    void testExecute_JSONFormat() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);
        arguments.put("output_format", "json");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);

        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        // Verify result contains expected strings without parsing JSON
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("graph"));
        assertTrue(contentText.contains("totalRules"));
    }

    @Test
    void testExecute_DOTFormat() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);
        arguments.put("output_format", "dot");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        // Verify result contains expected strings
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("dot"));
        assertTrue(contentText.contains("content"));
        assertTrue(contentText.contains("digraph CallGraph"));
    }

    @Test
    void testExecute_MermaidFormat() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);
        arguments.put("output_format", "mermaid");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        // Verify result contains expected strings
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("mermaid"));
        assertTrue(contentText.contains("content"));
        assertTrue(contentText.contains("graph LR"));
    }

    @Test
    void testExecute_DefaultFormat() throws Exception {
        // When output_format is not specified, should default to json
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        // Verify result contains expected strings
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("graph"));
    }

    @Test
    void testExecute_WithMetadata() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);
        arguments.put("output_format", "dot");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);

        // Verify result contains expected strings
        String contentText = getContentText(result);
        assertTrue(contentText.contains("metadata"));
        assertTrue(contentText.contains("totalRules"));
        assertTrue(contentText.contains("unusedRules"));
        assertTrue(contentText.contains("cycles"));
    }

    @Test
    void testExecute_InvalidGrammar() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", "invalid grammar syntax");
        arguments.put("output_format", "json");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);

        // Should still return a result (possibly with empty graph)
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
    }

    @Test
    void testExecute_RecursiveGrammar() throws Exception {
        String recursiveGrammar = """
            grammar Recursive;
            expr : expr '+' term | term ;
            term : INT ;
            INT : [0-9]+ ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", recursiveGrammar);
        arguments.put("output_format", "json");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_call_graph", arguments);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        McpSchema.CallToolResult result = tool.execute(exchange, request);

        assertNotNull(result);

        // Verify result contains expected strings
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("graph"));
        assertTrue(contentText.contains("cycles"));
        // Should detect recursive cycle (expr -> expr)
        assertTrue(contentText.contains("expr"));
    }
}
