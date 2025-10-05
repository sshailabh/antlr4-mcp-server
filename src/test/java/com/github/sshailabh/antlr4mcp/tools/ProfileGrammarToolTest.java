package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.service.GrammarProfiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProfileGrammarToolTest {

    @Autowired
    private GrammarProfiler grammarProfiler;

    @Autowired
    private ObjectMapper objectMapper;

    private ProfileGrammarTool tool;

    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        prog : expr EOF ;
        expr : term (('+' | '-') term)* ;
        term : factor (('*' | '/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        tool = new ProfileGrammarTool(grammarProfiler, objectMapper);
    }

    @Test
    void testGetTool() {
        McpSchema.Tool toolSchema = tool.toTool();

        assertNotNull(toolSchema);
        assertEquals("profile_grammar", toolSchema.name());
        assertNotNull(toolSchema.description());
        assertNotNull(toolSchema.inputSchema());

        // Verify input schema exists
        assertNotNull(toolSchema.inputSchema());
    }

    @Test
    void testHandleToolCallSuccess() {
        Map<String, Object> arguments = Map.of(
            "grammar", SIMPLE_GRAMMAR,
            "input", "10 + 20"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("profile"));
        assertTrue(contentText.contains("parserStats"));
    }

    @Test
    void testHandleToolCallWithStartRule() {
        Map<String, Object> arguments = Map.of(
            "grammar", SIMPLE_GRAMMAR,
            "input", "10 + 20",
            "startRule", "expr"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("profile"));
    }

    @Test
    void testHandleToolCallComplexExpression() {
        Map<String, Object> arguments = Map.of(
            "grammar", SIMPLE_GRAMMAR,
            "input", "10 + 20 * 30 - (5 + 3)"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("decisionStats"));
        assertTrue(contentText.contains("totalInvocations"));
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
        // Should return result with success=false
        String contentText = getContentText(result);
        assertTrue(contentText.contains("success") || contentText.contains("error"));
    }

    @Test
    void testHandleToolCallEmptyInput() {
        String emptyGrammar = """
            grammar Empty;
            prog : EOF ;
            """;

        Map<String, Object> arguments = Map.of(
            "grammar", emptyGrammar,
            "input", ""
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
    }

    @Test
    void testHandleToolCallAmbiguousGrammar() {
        String ambiguousGrammar = """
            grammar Ambiguous;
            prog : expr EOF ;
            expr : expr ('*'|'/') expr
                 | expr ('+'|'-') expr
                 | INT
                 ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = Map.of(
            "grammar", ambiguousGrammar,
            "input", "10 + 20 * 30"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("profile"));
    }

    @Test
    void testHandleToolCallLargeInput() {
        // Generate larger expression
        StringBuilder largeInput = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            if (i > 0) largeInput.append(" + ");
            largeInput.append(i);
        }

        Map<String, Object> arguments = Map.of(
            "grammar", SIMPLE_GRAMMAR,
            "input", largeInput.toString()
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("parsingTimeMs"));
    }

    @Test
    void testHandleToolCallVerifyMetrics() {
        Map<String, Object> arguments = Map.of(
            "grammar", SIMPLE_GRAMMAR,
            "input", "10 + 20 * 30"
        );

        McpSchema.CallToolRequest request = createRequest(arguments);
        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        assertTrue(contentText.contains("success"));
        assertTrue(contentText.contains("totalDecisions"));
        assertTrue(contentText.contains("totalInvocations"));
        assertTrue(contentText.contains("totalAmbiguities"));
        assertTrue(contentText.contains("parsingTimeMs"));
    }

    // Helper methods
    private McpSchema.CallToolRequest createRequest(Map<String, Object> arguments) {
        return new McpSchema.CallToolRequest(
            "profile_grammar",
            arguments
        );
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
