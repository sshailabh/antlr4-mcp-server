package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.LeftRecursionReport;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.LeftRecursionAnalyzer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for AnalyzeLeftRecursionTool.
 * 
 * Left recursion is fundamental to expression parsing in compilers.
 * ANTLR4 automatically transforms left-recursive rules, which affects
 * operator precedence and associativity.
 */
class AnalyzeLeftRecursionToolTest {

    private AnalyzeLeftRecursionTool tool;
    private LeftRecursionAnalyzer analyzer;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        GrammarCompiler grammarCompiler = new GrammarCompiler(securityValidator);
        analyzer = new LeftRecursionAnalyzer(grammarCompiler);
        objectMapper = new ObjectMapper();
        tool = new AnalyzeLeftRecursionTool(analyzer, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool mcpTool = tool.toTool();

        assertNotNull(mcpTool);
        assertEquals("analyze_left_recursion", mcpTool.name());
        assertNotNull(mcpTool.description());
        assertTrue(mcpTool.description().contains("left recursion"));
        assertNotNull(mcpTool.inputSchema());
    }

    @Test
    void testDirectLeftRecursion() throws Exception {
        // Classic left-recursive expression grammar
        String grammar = """
            grammar Expr;
            expr
                : expr '*' expr
                | expr '+' expr
                | INT
                ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_left_recursion", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        LeftRecursionReport report = objectMapper.readValue(contentText, LeftRecursionReport.class);

        assertTrue(report.isSuccess());
        assertTrue(report.isHasLeftRecursion());
        assertFalse(report.getRecursiveRules().isEmpty());
        
        // Should find the 'expr' rule as left-recursive
        boolean foundExpr = report.getRecursiveRules().stream()
            .anyMatch(r -> r.getRuleName().equals("expr"));
        assertTrue(foundExpr, "Should detect 'expr' as left-recursive");
    }

    @Test
    void testNonLeftRecursiveGrammar() throws Exception {
        // Simple non-recursive grammar
        String grammar = """
            grammar Simple;
            start : 'hello' 'world' ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_left_recursion", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        LeftRecursionReport report = objectMapper.readValue(contentText, LeftRecursionReport.class);

        assertTrue(report.isSuccess());
        assertFalse(report.isHasLeftRecursion());
    }

    @Test
    void testMultipleLeftRecursiveRules() throws Exception {
        // Grammar with multiple left-recursive rules
        String grammar = """
            grammar MultiRecursive;
            expr
                : expr '+' term
                | term
                ;
            term
                : term '*' factor
                | factor
                ;
            factor
                : '(' expr ')'
                | INT
                ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_left_recursion", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        LeftRecursionReport report = objectMapper.readValue(contentText, LeftRecursionReport.class);

        assertTrue(report.isSuccess());
        // Both expr and term should be left-recursive
        assertTrue(report.getLeftRecursiveRuleCount() >= 2);
    }

    @Test
    void testJsonGrammar() throws Exception {
        // JSON grammar is typically not left-recursive
        String grammar = """
            grammar Json;
            json : value ;
            value
                : STRING
                | NUMBER
                | obj
                | arr
                | 'true'
                | 'false'
                | 'null'
                ;
            obj : '{' pair (',' pair)* '}' | '{' '}' ;
            pair : STRING ':' value ;
            arr : '[' value (',' value)* ']' | '[' ']' ;
            STRING : '"' .*? '"' ;
            NUMBER : '-'? [0-9]+ ('.' [0-9]+)? ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_left_recursion", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        LeftRecursionReport report = objectMapper.readValue(contentText, LeftRecursionReport.class);

        assertTrue(report.isSuccess());
        // JSON grammar doesn't have left recursion
        assertFalse(report.isHasLeftRecursion());
    }

    @Test
    void testStatisticsReporting() throws Exception {
        String grammar = """
            grammar Stats;
            expr
                : expr '+' expr
                | expr '*' expr
                | ID
                ;
            stmt : expr ';' ;
            ID : [a-z]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_left_recursion", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        LeftRecursionReport report = objectMapper.readValue(contentText, LeftRecursionReport.class);

        assertTrue(report.isSuccess());
        assertTrue(report.getTotalRules() > 0);
        assertTrue(report.getLeftRecursiveRuleCount() > 0);
    }
}

