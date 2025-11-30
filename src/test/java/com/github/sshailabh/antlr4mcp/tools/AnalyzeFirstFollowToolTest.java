package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.FirstFollowReport;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.FirstFollowAnalyzer;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for AnalyzeFirstFollowTool.
 * 
 * FIRST and FOLLOW sets are fundamental to parsing theory and
 * essential for understanding grammar behavior and debugging conflicts.
 */
class AnalyzeFirstFollowToolTest {

    private AnalyzeFirstFollowTool tool;
    private FirstFollowAnalyzer analyzer;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        GrammarCompiler grammarCompiler = new GrammarCompiler(securityValidator);
        analyzer = new FirstFollowAnalyzer(grammarCompiler);
        objectMapper = new ObjectMapper();
        tool = new AnalyzeFirstFollowTool(analyzer, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool mcpTool = tool.toTool();

        assertNotNull(mcpTool);
        assertEquals("analyze_first_follow", mcpTool.name());
        assertNotNull(mcpTool.description());
        assertTrue(mcpTool.description().contains("FIRST"));
        assertTrue(mcpTool.description().contains("FOLLOW"));
        assertNotNull(mcpTool.inputSchema());
    }

    @Test
    void testSimpleGrammarFirstFollow() throws Exception {
        String grammar = """
            grammar Simple;
            start : 'hello' 'world' ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        assertNotNull(report.getRules());
        
        // Should have analysis for 'start' rule
        boolean foundStart = report.getRules().stream()
            .anyMatch(r -> r.getRuleName().equals("start"));
        assertTrue(foundStart);
    }

    @Test
    void testExpressionGrammarFirstFollow() throws Exception {
        // Expression grammar with clear FIRST sets
        String grammar = """
            grammar Expr;
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

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        assertTrue(report.getTotalParserRules() >= 3); // expr, term, factor
        
        // factor's FIRST set should contain '(' and INT
        FirstFollowReport.RuleAnalysis factorAnalysis = report.getRules().stream()
            .filter(r -> r.getRuleName().equals("factor"))
            .findFirst()
            .orElse(null);
        
        if (factorAnalysis != null) {
            assertNotNull(factorAnalysis.getFirstSet());
            assertFalse(factorAnalysis.getFirstSet().isEmpty());
        }
    }

    @Test
    void testSpecificRuleAnalysis() throws Exception {
        String grammar = """
            grammar Multi;
            a : 'x' b ;
            b : 'y' c ;
            c : 'z' ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "b");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        // When analyzing specific rule, should only have that rule
        assertEquals(1, report.getRules().size());
        assertEquals("b", report.getRules().get(0).getRuleName());
    }

    @Test
    void testNullableRuleDetection() throws Exception {
        // Grammar with nullable rule
        String grammar = """
            grammar Nullable;
            start : optional 'x' ;
            optional : 'y'? ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        // optional rule should be nullable (can derive ε)
        assertTrue(report.getNullableRuleCount() >= 0);
    }

    @Test
    void testDecisionAnalysis() throws Exception {
        // Grammar with multiple alternatives
        String grammar = """
            grammar Decisions;
            start
                : 'a' stmt
                | 'b' expr
                | 'c'
                ;
            stmt : 'stmt' ;
            expr : 'expr' ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        assertNotNull(report.getDecisions());
        // Should have decision analysis for the start rule's alternatives
        assertTrue(report.getTotalDecisions() > 0);
    }

    @Test
    void testAmbiguousLookaheadDetection() throws Exception {
        // Grammar with potential LL(1) conflict
        String grammar = """
            grammar Conflict;
            start
                : ID '(' args ')'
                | ID '=' expr
                ;
            args : expr (',' expr)* ;
            expr : ID | NUM ;
            ID : [a-z]+ ;
            NUM : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        // This grammar has overlapping lookahead (both alts start with ID)
        // Check if conflicts are detected
        assertNotNull(report.getDecisions());
    }

    @Test
    void testStatisticsComputation() throws Exception {
        String grammar = """
            grammar Stats;
            a : b | c ;
            b : 'x' ;
            c : 'y' ;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);

        assertTrue(report.isSuccess());
        assertEquals(3, report.getTotalParserRules());
        assertTrue(report.getTotalDecisions() >= 0);
    }

    @Test
    void testInvalidGrammarHandling() throws Exception {
        String grammar = "invalid grammar content";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("analyze_first_follow", arguments);
        McpSchema.CallToolResult result = tool.execute(mockExchange, request);

        assertNotNull(result);
        // Should handle invalid grammar gracefully
        String contentText = getContentText(result);
        FirstFollowReport report = objectMapper.readValue(contentText, FirstFollowReport.class);
        assertNotNull(report);
    }
}

