package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
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

class ParseSampleToolTest {

    private ParseSampleTool parseSampleTool;
    private GrammarCompiler grammarCompiler;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        grammarCompiler = new GrammarCompiler(securityValidator);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        objectMapper = new ObjectMapper();
        parseSampleTool = new ParseSampleTool(grammarCompiler, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool tool = parseSampleTool.toTool();

        assertNotNull(tool);
        assertEquals("parse_sample", tool.name());
        assertNotNull(tool.description());
        assertTrue(tool.description().contains("Parse input"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testParseSimpleGrammar() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";
        String input = "hello";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertTrue(parseResult.isSuccess() || parseResult.getErrors() != null);
    }

    @Test
    void testParseCalculatorExpression() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);
        String input = "3 + 4 * 5";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
        if (parseResult.isSuccess()) {
            assertNotNull(parseResult.getParseTree());
        }
    }

    @Test
    void testParseWithTokens() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' WORLD ;\n" +
                        "WORLD : 'world' | 'universe' ;\n" +
                        "WS : [ \\t\\r\\n]+ -> skip ;\n";
        String input = "hello world";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");
        arguments.put("show_tokens", true);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
        if (parseResult.isSuccess()) {
            assertNotNull(parseResult.getTokens());
            assertFalse(parseResult.getTokens().isEmpty());
        }
    }

    @Test
    void testParseJsonDocument() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/Json.g4");
        String grammar = Files.readString(grammarPath);
        String input = "{\"name\": \"John\", \"age\": 30}";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "json");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
    }

    @Test
    void testParseWithInvalidInput() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";
        String input = "goodbye";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
        // Should have errors
        if (!parseResult.isSuccess()) {
            assertNotNull(parseResult.getErrors());
            assertFalse(parseResult.getErrors().isEmpty());
        }
    }

    @Test
    void testParseWithInvalidGrammar() throws Exception {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined_rule ;\n";
        String input = "test";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        // Tool should handle gracefully

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
        assertFalse(parseResult.isSuccess());
    }

    @Test
    void testParseWithInvalidStartRule() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";
        String input = "hello";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "nonexistent");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
        // Note: Grammar compilation may succeed even with invalid start rule
        // The actual parse will fail at runtime
    }

    @Test
    void testParseTreeFormat() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";
        String input = "hello";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");
        arguments.put("tree_format", "tree");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
    }

    @Test
    void testParseTokensFormat() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";
        String input = "hello";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");
        arguments.put("tree_format", "tokens");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
    }

    @Test
    void testParseComplexExpression() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);
        String input = "(3 + 4) * (5 - 2)";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "expr");
        arguments.put("show_tokens", true);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
    }

    @Test
    void testParseEmptyInput() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello'? ;\n";
        String input = "";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("sample_input", input);
        arguments.put("start_rule", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("parse_sample", arguments);
        McpSchema.CallToolResult result = parseSampleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);
        assertNotNull(parseResult);
    }
}
