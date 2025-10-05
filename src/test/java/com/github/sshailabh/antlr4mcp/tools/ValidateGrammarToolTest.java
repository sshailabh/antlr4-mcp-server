package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ValidateGrammarToolTest {

    private ValidateGrammarTool validateGrammarTool;
    private GrammarCompiler grammarCompiler;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        objectMapper = new ObjectMapper();
        validateGrammarTool = new ValidateGrammarTool(grammarCompiler, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool tool = validateGrammarTool.toTool();

        assertNotNull(tool);
        assertEquals("validate_grammar", tool.name());
        assertNotNull(tool.description());
        assertTrue(tool.description().contains("Validates ANTLR4 grammar"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testValidateSimpleGrammar() throws Exception {
        String grammar = "grammar Simple;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertTrue(validationResult.isSuccess());
        assertEquals("Simple", validationResult.getGrammarName());
        assertEquals(1, validationResult.getParserRules());
        assertEquals(0, validationResult.getLexerRules());
    }

    @Test
    void testValidateGrammarFromFile() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertTrue(validationResult.isSuccess());
        assertEquals("SimpleCalc", validationResult.getGrammarName());
        assertEquals(3, validationResult.getParserRules());
        assertEquals(2, validationResult.getLexerRules());
    }

    @Test
    void testValidateComplexGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/Json.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertTrue(validationResult.isSuccess());
        assertEquals("Json", validationResult.getGrammarName());
        assertEquals(5, validationResult.getParserRules());
        assertTrue(validationResult.getLexerRules() >= 3);
    }

    @Test
    void testValidateInvalidGrammar() throws Exception {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined_rule ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertFalse(validationResult.isSuccess());
        assertFalse(validationResult.getErrors().isEmpty());
    }

    @Test
    void testValidateGrammarWithExpectedName() throws Exception {
        String grammar = "grammar Test;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("grammar_name", "Test");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertTrue(validationResult.isSuccess());
        assertEquals("Test", validationResult.getGrammarName());
    }

    @Test
    void testValidateGrammarWithWrongExpectedName() throws Exception {
        String grammar = "grammar Test;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("grammar_name", "WrongName");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertFalse(validationResult.isSuccess());
        assertFalse(validationResult.getErrors().isEmpty());
        assertTrue(validationResult.getErrors().get(0).getMessage().contains("Expected grammar name"));
    }

    @Test
    void testValidateGrammarWithoutDeclaration() throws Exception {
        String grammar = "start : 'hello' ;";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertFalse(validationResult.isSuccess());
    }

    @Test
    void testValidateLexerGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/CommonLexer.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertTrue(validationResult.isSuccess());
        assertEquals("CommonLexer", validationResult.getGrammarName());
        assertEquals(0, validationResult.getParserRules());
        assertTrue(validationResult.getLexerRules() >= 5);
    }

    @Test
    void testValidateEmptyGrammar() throws Exception {
        String grammar = "";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        // Empty grammar should result in error
        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertFalse(validationResult.isSuccess());
    }

    @Test
    void testValidateGrammarWithSyntaxErrors() throws Exception {
        String grammar = "grammar Bad;\n" +
                        "start : 'hello' \n" +  // Missing semicolon
                        "end : 'world' ;";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("validate_grammar", arguments);
        McpSchema.CallToolResult result = validateGrammarTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertFalse(validationResult.isSuccess());
        assertFalse(validationResult.getErrors().isEmpty());
    }
}
