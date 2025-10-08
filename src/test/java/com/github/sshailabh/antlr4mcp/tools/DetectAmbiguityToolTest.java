package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.*;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for DetectAmbiguityTool using RuntimeAmbiguityDetector (Phase 2).
 */
class DetectAmbiguityToolTest {

    private DetectAmbiguityTool detectAmbiguityTool;
    private RuntimeAmbiguityDetector runtimeAmbiguityDetector;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        ParseTimeoutManager parseTimeoutManager = new ParseTimeoutManager();

        // Create dependencies for GrammarInterpreter
        com.github.sshailabh.antlr4mcp.analysis.EmbeddedCodeAnalyzer embeddedCodeAnalyzer =
            new com.github.sshailabh.antlr4mcp.analysis.EmbeddedCodeAnalyzer();

        GrammarInterpreter grammarInterpreter = new GrammarInterpreter(embeddedCodeAnalyzer, parseTimeoutManager);

        runtimeAmbiguityDetector = new RuntimeAmbiguityDetector(grammarInterpreter, parseTimeoutManager);
        objectMapper = new ObjectMapper();
        detectAmbiguityTool = new DetectAmbiguityTool(runtimeAmbiguityDetector, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool tool = detectAmbiguityTool.toTool();

        assertNotNull(tool);
        assertEquals("detect_ambiguity", tool.name());
        assertNotNull(tool.description());
        assertTrue(tool.description().contains("ambiguities") || tool.description().contains("profiling"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testDetectAmbiguityInSimpleGrammar() throws Exception {
        String grammar = "grammar Simple;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "start");
        arguments.put("sample_inputs", List.of("hello"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        // Simple grammar should have no ambiguities
        assertFalse(report.isHasAmbiguities());
        assertEquals(1, report.getTotalSamplesParsed());
    }

    @Test
    void testDetectAmbiguityInAmbiguousGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/AmbiguousGrammar.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "expr");
        arguments.put("sample_inputs", List.of("1", "1+2", "1+2*3"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        assertEquals(3, report.getTotalSamplesParsed());
    }

    @Test
    void testDetectAmbiguityInCalcGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "expr");
        arguments.put("sample_inputs", List.of("42", "1+2*3", "(1+2)*3"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        assertFalse(report.isHasAmbiguities());
    }

    @Test
    void testDetectAmbiguityInJsonGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/Json.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "json");
        arguments.put("sample_inputs", List.of("{}", "[]", "{\"key\":\"value\"}"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        // JSON grammar might have some ambiguities
    }

    @Test
    void testDetectAmbiguityWithInvalidGrammar() throws Exception {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined_rule ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "start");
        arguments.put("sample_inputs", List.of("test"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        // Invalid grammar may error
        assertTrue(result.isError());
    }

    @Test
    void testDetectAmbiguityWithComplexLeftRecursion() throws Exception {
        String grammar = "grammar LeftRecursive;\n" +
                        "expr\n" +
                        "    : expr '+' expr\n" +
                        "    | expr '*' expr\n" +
                        "    | NUMBER\n" +
                        "    ;\n" +
                        "NUMBER : [0-9]+ ;\n" +
                        "WS : [ \\t\\r\\n]+ -> skip ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "expr");
        arguments.put("sample_inputs", List.of("1", "1+2", "1*2+3"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        assertEquals(3, report.getTotalSamplesParsed());
    }

    @Test
    void testDetectAmbiguityWithAlternativeConflict() throws Exception {
        String grammar = "grammar Conflict;\n" +
                        "stat\n" +
                        "    : 'if' expr 'then' stat ('else' stat)?\n" +
                        "    | ID\n" +
                        "    ;\n" +
                        "expr : ID ;\n" +
                        "ID : [a-z]+ ;\n" +
                        "WS : [ \\t\\r\\n]+ -> skip ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "stat");
        arguments.put("sample_inputs", List.of("x", "if x then y", "if x then y else z"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
    }

    @Test
    void testDetectAmbiguityWithEmptyGrammar() throws Exception {
        String grammar = "";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "start");
        arguments.put("sample_inputs", List.of("test"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        // Empty grammar should error
        assertTrue(result.isError());
    }

    @Test
    void testDetectAmbiguityReportStructure() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/AmbiguousGrammar.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "expr");
        arguments.put("sample_inputs", List.of("1+2+3"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);

        // Check Phase 2 fields
        assertNotNull(report.getTotalSamplesParsed());
        assertNotNull(report.getTotalParseTimeMs());
        assertNotNull(report.getAmbiguitiesPerRule());

        if (report.isHasAmbiguities()) {
            assertNotNull(report.getAmbiguities());
            assertFalse(report.getAmbiguities().isEmpty());
            // Check structure of ambiguities
            report.getAmbiguities().forEach(ambiguity -> {
                assertNotNull(ambiguity.getRuleName());
            });
        }
    }

    @Test
    void testDetectAmbiguityWithLexerGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/CommonLexer.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("start_rule", "NUMBER"); // Lexer rule
        arguments.put("sample_inputs", List.of("123", "456"));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        // May error since lexer grammars don't have parser rules
        // Just verify it doesn't crash
    }
}
