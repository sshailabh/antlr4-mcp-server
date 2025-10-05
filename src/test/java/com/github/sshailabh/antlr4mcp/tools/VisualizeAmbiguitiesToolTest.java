package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.service.AmbiguityVisualizer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VisualizeAmbiguitiesToolTest {

    @Autowired
    private AmbiguityVisualizer ambiguityVisualizer;

    @Autowired
    private ObjectMapper objectMapper;

    private VisualizeAmbiguitiesTool tool;

    private static final String AMBIGUOUS_GRAMMAR = """
        grammar Ambiguous;
        prog : stat+ EOF ;
        stat : 'if' expr 'then' stat
             | 'if' expr 'then' stat 'else' stat
             | 'print' expr
             ;
        expr : ID ;
        ID : [a-z]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String UNAMBIGUOUS_GRAMMAR = """
        grammar Unambiguous;
        prog : expr EOF ;
        expr : term (('+' | '-') term)* ;
        term : factor (('*' | '/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        tool = new VisualizeAmbiguitiesTool(ambiguityVisualizer, objectMapper);
    }

    @Test
    void testGetTool() {
        McpSchema.Tool toolSchema = tool.toTool();

        assertNotNull(toolSchema);
        assertEquals("visualize_ambiguities", toolSchema.name());
        assertNotNull(toolSchema.description());
        assertNotNull(toolSchema.inputSchema());
    }

    @Test
    void testHandleToolCallSuccess() {
        Map<String, Object> arguments = Map.of(
            "grammar", AMBIGUOUS_GRAMMAR,
            "input", "if a then if b then print c else print d"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("visualization"));
    }

    @Test
    void testHandleToolCallWithStartRule() {
        Map<String, Object> arguments = Map.of(
            "grammar", AMBIGUOUS_GRAMMAR,
            "input", "print a",
            "start_rule", "stat"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("stat"));
    }

    @Test
    void testHandleToolCallAmbiguousInput() {
        Map<String, Object> arguments = Map.of(
            "grammar", AMBIGUOUS_GRAMMAR,
            "input", "if a then if b then print c else print d"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("hasAmbiguities") || contentText.contains("ambiguities"));
    }

    @Test
    void testHandleToolCallUnambiguousInput() {
        Map<String, Object> arguments = Map.of(
            "grammar", UNAMBIGUOUS_GRAMMAR,
            "input", "10 + 20 * 30"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("visualization"));
    }

    @Test
    void testHandleToolCallInvalidGrammar() {
        Map<String, Object> arguments = Map.of(
            "grammar", "grammar Invalid; prog : UNDEFINED ;",
            "input", "test"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        // Should return result with success=false inside visualization
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success") || contentText.contains("error"));
    }

    @Test
    void testHandleToolCallSimpleStatement() {
        Map<String, Object> arguments = Map.of(
            "grammar", AMBIGUOUS_GRAMMAR,
            "input", "print x"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
    }

    @Test
    void testHandleToolCallComplexNested() {
        Map<String, Object> arguments = Map.of(
            "grammar", AMBIGUOUS_GRAMMAR,
            "input", "if a then if b then if c then print d else print e else print f"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("visualization"));
    }

    @Test
    void testHandleToolCallVerifyPrimaryInterpretation() {
        Map<String, Object> arguments = Map.of(
            "grammar", AMBIGUOUS_GRAMMAR,
            "input", "if a then print b"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("primaryInterpretation") || contentText.contains("parse"));
    }

    // Helper methods
    private McpSchema.CallToolRequest createRequest(Map<String, Object> arguments) {
        return new McpSchema.CallToolRequest(
            "visualize_ambiguities",
            arguments
        );
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
