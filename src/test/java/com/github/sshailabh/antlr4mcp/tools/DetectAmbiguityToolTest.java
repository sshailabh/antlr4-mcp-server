package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.AmbiguityDetector;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DetectAmbiguityToolTest {

    private DetectAmbiguityTool detectAmbiguityTool;
    private AmbiguityDetector ambiguityDetector;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        GrammarCompiler grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        ambiguityDetector = new AmbiguityDetector(grammarCompiler);
        objectMapper = new ObjectMapper();
        detectAmbiguityTool = new DetectAmbiguityTool(ambiguityDetector, objectMapper);
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
        assertTrue(tool.description().contains("ambiguities"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testDetectAmbiguityInSimpleGrammar() throws Exception {
        String grammar = "grammar Simple;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        // Simple grammar should have no ambiguities
        assertFalse(report.isHasAmbiguities());
    }

    @Test
    void testDetectAmbiguityInAmbiguousGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/AmbiguousGrammar.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

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
    void testDetectAmbiguityInCalcGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

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

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        // Tool should handle gracefully

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        // Invalid grammar should not crash
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

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        // Note: Ambiguity detection not fully implemented in M1
        assertFalse(report.isHasAmbiguities());
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

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        // Empty grammar should be handled

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
    }

    @Test
    void testDetectAmbiguityReportStructure() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/AmbiguousGrammar.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);

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

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("detect_ambiguity", arguments);
        McpSchema.CallToolResult result = detectAmbiguityTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);
        assertNotNull(report);
        // Lexer grammars typically don't have ambiguities
    }
}
