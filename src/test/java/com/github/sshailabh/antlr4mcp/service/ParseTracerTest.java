package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ParseTrace;
import com.github.sshailabh.antlr4mcp.model.TraceEvent;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParseTracer Service Tests")
class ParseTracerTest {

    private ParseTracer parseTracer;
    private GrammarCompiler grammarCompiler;

    // Test grammars
    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        start : 'hello' 'world' ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String EXPR_GRAMMAR = """
        grammar Expr;
        expr : expr '+' term
             | term
             ;
        term : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String NESTED_GRAMMAR = """
        grammar Nested;
        statement : 'if' expr 'then' statement
                  | 'print' expr
                  ;
        expr : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String RECURSIVE_GRAMMAR = """
        grammar Recursive;
        list : '[' elements ']' ;
        elements : element (',' element)* | ;
        element : INT | list ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        parseTracer = new ParseTracer(grammarCompiler);
    }

    @Test
    @DisplayName("Should trace simple expression parsing")
    void testTraceSimpleExpression() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        ParseTrace trace = parseTracer.trace(SIMPLE_GRAMMAR, sampleInput, "start");

        // Assert
        assertTrue(trace.isSuccess());
        assertNotNull(trace.getEvents());
        assertFalse(trace.getEvents().isEmpty());
        assertEquals("start", trace.getStartRule());
        assertEquals(sampleInput, trace.getInput());

        // Verify trace contains expected events
        List<String> eventTypes = trace.getEvents().stream()
            .map(TraceEvent::getType)
            .collect(Collectors.toList());

        assertTrue(eventTypes.contains("ENTER_RULE"));
        assertTrue(eventTypes.contains("CONSUME_TOKEN"));
        assertTrue(eventTypes.contains("EXIT_RULE"));

        // Verify tokens are consumed
        List<String> consumedTokens = trace.getEvents().stream()
            .filter(e -> "CONSUME_TOKEN".equals(e.getType()))
            .map(TraceEvent::getTokenText)
            .filter(t -> !"<EOF>".equals(t))
            .collect(Collectors.toList());

        assertTrue(consumedTokens.contains("hello"));
        assertTrue(consumedTokens.contains("world"));
    }

    @Test
    @DisplayName("Should trace nested rule execution")
    void testTraceNestedRules() {
        // Arrange
        String sampleInput = "1 + 2";

        // Act
        ParseTrace trace = parseTracer.trace(EXPR_GRAMMAR, sampleInput, "expr");

        // Assert
        assertTrue(trace.isSuccess());

        // Verify nested depth tracking
        int maxDepth = trace.getEvents().stream()
            .mapToInt(TraceEvent::getDepth)
            .max()
            .orElse(0);

        assertTrue(maxDepth > 0, "Should have nested rule calls");

        // Verify rule names are tracked
        List<String> ruleNames = trace.getEvents().stream()
            .filter(e -> "ENTER_RULE".equals(e.getType()))
            .map(TraceEvent::getRuleName)
            .distinct()
            .collect(Collectors.toList());

        assertTrue(ruleNames.contains("expr"));
        assertTrue(ruleNames.contains("term"));
    }

    @Test
    @DisplayName("Should trace recursive rule execution")
    void testTraceRecursiveRules() {
        // Arrange
        String sampleInput = "[1, [2, 3], 4]";

        // Act
        ParseTrace trace = parseTracer.trace(RECURSIVE_GRAMMAR, sampleInput, "list");

        // Assert
        assertTrue(trace.isSuccess());

        // Verify recursive calls are tracked
        long listRuleCount = trace.getEvents().stream()
            .filter(e -> "ENTER_RULE".equals(e.getType()) && "list".equals(e.getRuleName()))
            .count();

        assertTrue(listRuleCount >= 2, "Should have multiple list rule entries for nested lists");
    }

    @Test
    @DisplayName("Should track token consumption with line and column")
    void testTokenConsumptionTracking() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        ParseTrace trace = parseTracer.trace(SIMPLE_GRAMMAR, sampleInput, "start");

        // Assert
        assertTrue(trace.isSuccess());

        // Find token consumption events
        List<TraceEvent> tokenEvents = trace.getEvents().stream()
            .filter(e -> "CONSUME_TOKEN".equals(e.getType()) && !"<EOF>".equals(e.getTokenText()))
            .collect(Collectors.toList());

        assertFalse(tokenEvents.isEmpty());

        // First token should be at line 1, column 0
        TraceEvent firstToken = tokenEvents.get(0);
        assertEquals("hello", firstToken.getTokenText());
        assertEquals(1, firstToken.getLine());
        assertEquals(0, firstToken.getColumn());
    }

    @Test
    @DisplayName("Should handle empty input")
    void testEmptyInput() {
        // Arrange
        String grammar = """
            grammar Empty;
            start : item* ;
            item : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        // Act
        ParseTrace trace = parseTracer.trace(grammar, "", "start");

        // Assert
        assertTrue(trace.isSuccess());
        assertNotNull(trace.getEvents());

        // Should still have ENTER/EXIT events for start rule
        boolean hasEnterStart = trace.getEvents().stream()
            .anyMatch(e -> "ENTER_RULE".equals(e.getType()) && "start".equals(e.getRuleName()));
        assertTrue(hasEnterStart);
    }

    @Test
    @DisplayName("Should track step numbers sequentially")
    void testStepNumbering() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        ParseTrace trace = parseTracer.trace(SIMPLE_GRAMMAR, sampleInput, "start");

        // Assert
        assertTrue(trace.isSuccess());

        // Verify step numbers are sequential
        for (int i = 0; i < trace.getEvents().size(); i++) {
            assertEquals(i, trace.getEvents().get(i).getStep(),
                "Step numbers should be sequential");
        }
    }

    @Test
    @DisplayName("Should include timing information")
    void testTimingInformation() {
        // Arrange
        String sampleInput = "1 + 2";

        // Act
        ParseTrace trace = parseTracer.trace(EXPR_GRAMMAR, sampleInput, "expr");

        // Assert
        assertTrue(trace.isSuccess());

        // Verify timestamps are present and increasing
        List<Long> timestamps = trace.getEvents().stream()
            .map(TraceEvent::getTimestamp)
            .filter(ts -> ts != null)
            .collect(Collectors.toList());

        assertFalse(timestamps.isEmpty(), "Should have timestamp information");

        // Timestamps should be non-decreasing
        for (int i = 1; i < timestamps.size(); i++) {
            assertTrue(timestamps.get(i) >= timestamps.get(i - 1),
                "Timestamps should be non-decreasing");
        }
    }

    @Test
    @DisplayName("Should handle parse errors gracefully")
    void testParseErrorsInTrace() {
        // Arrange
        String sampleInput = "hello"; // Missing 'world'

        // Act
        ParseTrace trace = parseTracer.trace(SIMPLE_GRAMMAR, sampleInput, "start");

        // Assert
        assertNotNull(trace);
        // Even with parse errors, should still provide trace
        assertNotNull(trace.getEvents());

        // Should consume 'hello' token
        boolean hasHelloToken = trace.getEvents().stream()
            .anyMatch(e -> "CONSUME_TOKEN".equals(e.getType()) && "hello".equals(e.getTokenText()));
        assertTrue(hasHelloToken);
    }

    @Test
    @DisplayName("Should handle invalid grammar")
    void testInvalidGrammar() {
        // Arrange
        String invalidGrammar = "This is not valid!";

        // Act
        ParseTrace trace = parseTracer.trace(invalidGrammar, "test", "start");

        // Assert
        assertFalse(trace.isSuccess());
        assertNotNull(trace.getErrors());
        assertFalse(trace.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should handle invalid rule name")
    void testInvalidRuleName() {
        // Act
        ParseTrace trace = parseTracer.trace(SIMPLE_GRAMMAR, "hello world", "nonexistent");

        // Assert
        assertFalse(trace.isSuccess());
        assertNotNull(trace.getErrors());
        assertTrue(trace.getErrors().stream()
            .anyMatch(e -> e.getMessage().contains("nonexistent")));
    }

    @Test
    @DisplayName("Should provide meaningful messages in events")
    void testEventMessages() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        ParseTrace trace = parseTracer.trace(SIMPLE_GRAMMAR, sampleInput, "start");

        // Assert
        assertTrue(trace.isSuccess());

        // Verify events have meaningful messages
        trace.getEvents().forEach(event -> {
            assertNotNull(event.getMessage(), "Event should have a message");
            assertFalse(event.getMessage().isEmpty(), "Message should not be empty");
        });
    }

    @Test
    @DisplayName("Should set totalSteps correctly")
    void testTotalSteps() {
        // Arrange
        String sampleInput = "1 + 2";

        // Act
        ParseTrace trace = parseTracer.trace(EXPR_GRAMMAR, sampleInput, "expr");

        // Assert
        assertTrue(trace.isSuccess());
        assertNotNull(trace.getTotalSteps());
        assertEquals(trace.getEvents().size(), trace.getTotalSteps());
    }
}