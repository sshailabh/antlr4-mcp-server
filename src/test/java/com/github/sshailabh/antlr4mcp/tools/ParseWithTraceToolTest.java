package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ParseTrace;
import com.github.sshailabh.antlr4mcp.model.TraceEvent;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.ParseTracer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("ParseWithTraceTool Integration Tests")
class ParseWithTraceToolTest {

    private ParseWithTraceTool parseWithTraceTool;
    private ParseTracer parseTracer;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        GrammarCompiler grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        parseTracer = new ParseTracer(grammarCompiler);
        objectMapper = new ObjectMapper();
        parseWithTraceTool = new ParseWithTraceTool(parseTracer, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    @DisplayName("Should create tool with correct metadata")
    void testToTool() {
        // Act
        McpSchema.Tool tool = parseWithTraceTool.toTool();

        // Assert
        assertNotNull(tool);
        assertEquals("parse_with_trace", tool.name());
        assertNotNull(tool.description());
        assertTrue(tool.description().contains("step-by-step trace"));

        // Verify input schema
        assertNotNull(tool.inputSchema());
        assertEquals("object", tool.inputSchema().type());

        Map<String, Object> properties = tool.inputSchema().properties();
        assertTrue(properties.containsKey("grammar_text"));
        assertTrue(properties.containsKey("sample_input"));
        assertTrue(properties.containsKey("start_rule"));

        List<String> required = tool.inputSchema().required();
        assertEquals(3, required.size());
        assertTrue(required.contains("grammar_text"));
        assertTrue(required.contains("sample_input"));
        assertTrue(required.contains("start_rule"));
    }

    @Test
    @DisplayName("Should execute trace for simple grammar")
    void testExecuteSimpleGrammar() throws Exception {
        // Arrange
        String grammar = """
            grammar Simple;
            start : 'hello' 'world' ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "hello world");
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertTrue(trace.isSuccess());
        assertNotNull(trace.getEvents());
        assertFalse(trace.getEvents().isEmpty());
        assertEquals("start", trace.getStartRule());
        assertEquals("hello world", trace.getInput());
    }

    @Test
    @DisplayName("Should trace expression parsing")
    void testExecuteExpressionGrammar() throws Exception {
        // Arrange
        String grammar = """
            grammar Expr;
            expr : expr '+' term | term ;
            term : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "1 + 2 + 3");
        arguments.put("start_rule", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertTrue(trace.isSuccess());

        // Verify trace contains tokens
        long tokenCount = trace.getEvents().stream()
            .filter(e -> "CONSUME_TOKEN".equals(e.getType()))
            .count();
        assertTrue(tokenCount > 0);

        // Verify trace contains rule entries
        long ruleEntries = trace.getEvents().stream()
            .filter(e -> "ENTER_RULE".equals(e.getType()))
            .count();
        assertTrue(ruleEntries > 0);
    }

    @Test
    @DisplayName("Should handle empty input")
    void testExecuteEmptyInput() throws Exception {
        // Arrange
        String grammar = """
            grammar Empty;
            start : item* ;
            item : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "");
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertTrue(trace.isSuccess());
        assertNotNull(trace.getEvents());
    }

    @Test
    @DisplayName("Should handle invalid grammar")
    void testExecuteInvalidGrammar() throws Exception {
        // Arrange
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", "This is not valid!");
        arguments.put("sample_input", "test");
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError()); // Tool itself doesn't error

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertFalse(trace.isSuccess());
        assertNotNull(trace.getErrors());
        assertFalse(trace.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should handle invalid rule name")
    void testExecuteInvalidRule() throws Exception {
        // Arrange
        String grammar = """
            grammar Test;
            start : 'test' ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "test");
        arguments.put("start_rule", "nonexistent");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertFalse(trace.isSuccess());
        assertTrue(trace.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("nonexistent")));
    }

    @Test
    @DisplayName("Should track nested rule depth")
    void testNestedRuleDepth() throws Exception {
        // Arrange
        String grammar = """
            grammar Nested;
            statement : 'if' expr 'then' statement | 'print' expr ;
            expr : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "if 1 then print 2");
        arguments.put("start_rule", "statement");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertTrue(trace.isSuccess());

        // Check depth tracking
        int maxDepth = trace.getEvents().stream()
            .mapToInt(TraceEvent::getDepth)
            .max()
            .orElse(0);
        assertTrue(maxDepth > 0);
    }

    @Test
    @DisplayName("Should include timing information")
    void testTimingInformation() throws Exception {
        // Arrange
        String grammar = """
            grammar Test;
            start : 'a' 'b' 'c' ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "a b c");
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertTrue(trace.isSuccess());

        // Check timestamps exist
        boolean hasTimestamps = trace.getEvents().stream()
            .anyMatch(e -> e.getTimestamp() != null);
        assertTrue(hasTimestamps);
    }

    @Test
    @DisplayName("Should handle missing required parameters")
    void testMissingParameters() throws Exception {
        // Arrange - missing start_rule
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", "grammar Test; start : 'test' ;");
        arguments.put("sample_input", "test");
        // Missing 'start_rule' which is required

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert - Should return error result
        assertNotNull(result);
        assertFalse(result.isError()); // Tool itself doesn't error

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertFalse(trace.isSuccess());
        assertNotNull(trace.getErrors());
        assertFalse(trace.getErrors().isEmpty());
        assertTrue(trace.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("Start rule cannot be null")));
    }

    @Test
    @DisplayName("Should provide step-by-step trace")
    void testStepByStepTrace() throws Exception {
        // Arrange
        String grammar = """
            grammar Steps;
            start : a b ;
            a : 'alpha' ;
            b : 'beta' ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", "alpha beta");
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_with_trace", arguments);

        // Act
        McpSchema.CallToolResult result = parseWithTraceTool.execute(mockExchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());

        String jsonResult = getContentText(result);
        ParseTrace trace = objectMapper.readValue(jsonResult, ParseTrace.class);

        assertTrue(trace.isSuccess());

        // Verify sequential step numbers
        for (int i = 0; i < trace.getEvents().size(); i++) {
            assertEquals(i, trace.getEvents().get(i).getStep());
        }

        // Verify totalSteps matches event count
        assertEquals(trace.getEvents().size(), trace.getTotalSteps());
    }
}