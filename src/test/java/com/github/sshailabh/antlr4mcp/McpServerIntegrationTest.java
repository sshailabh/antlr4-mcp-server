package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.service.RuntimeAmbiguityDetector;
import com.github.sshailabh.antlr4mcp.service.ErrorTransformer;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.GrammarInterpreter;
import com.github.sshailabh.antlr4mcp.tools.DetectAmbiguityTool;
import com.github.sshailabh.antlr4mcp.tools.ParseSampleTool;
import com.github.sshailabh.antlr4mcp.tools.ValidateGrammarTool;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class McpServerIntegrationTest {

    @Autowired
    private GrammarCompiler grammarCompiler;

    @Autowired
    private GrammarInterpreter grammarInterpreter;

    @Autowired
    private ErrorTransformer errorTransformer;

    @Autowired
    private RuntimeAmbiguityDetector runtimeAmbiguityDetector;

    // TreeVisualizer removed - no longer needed

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testMcpServerBuilds() {
        // Test with core essential tools only
        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarInterpreter, errorTransformer, objectMapper);
        ParseSampleTool parseSampleTool = new ParseSampleTool(grammarInterpreter, errorTransformer, objectMapper);
        DetectAmbiguityTool detectAmbiguityTool = new DetectAmbiguityTool(runtimeAmbiguityDetector, objectMapper);

        var jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        InputStream mockInput = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        OutputStream mockOutput = new ByteArrayOutputStream();
        var transportProvider = new StdioServerTransportProvider(jsonMapper, mockInput, mockOutput);

        McpSyncServer mcpServer = McpServer.sync(transportProvider)
            .serverInfo("antlr4-mcp-server", "0.2.0")
            .toolCall(validateGrammarTool.toTool(), validateGrammarTool::execute)
            .toolCall(parseSampleTool.toTool(), parseSampleTool::execute)
            .toolCall(detectAmbiguityTool.toTool(), detectAmbiguityTool::execute)
            .immediateExecution(true)
            .build();

        assertNotNull(mcpServer);
        mcpServer.close();
    }

    @Test
    public void testToolSchemas() {
        // Test core essential tools only
        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarInterpreter, errorTransformer, objectMapper);
        ParseSampleTool parseSampleTool = new ParseSampleTool(grammarInterpreter, errorTransformer, objectMapper);
        DetectAmbiguityTool detectAmbiguityTool = new DetectAmbiguityTool(runtimeAmbiguityDetector, objectMapper);

        McpSchema.Tool validateTool = validateGrammarTool.toTool();
        assertNotNull(validateTool);
        assertEquals("validate_grammar", validateTool.name());
        assertNotNull(validateTool.description());
        assertNotNull(validateTool.inputSchema());

        McpSchema.Tool parseTool = parseSampleTool.toTool();
        assertNotNull(parseTool);
        assertEquals("parse_sample", parseTool.name());

        McpSchema.Tool ambiguityTool = detectAmbiguityTool.toTool();
        assertNotNull(ambiguityTool);
        assertEquals("detect_ambiguity", ambiguityTool.name());
    }

    @Test
    public void testValidateGrammarToolExecution() throws Exception {
        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarInterpreter, errorTransformer, objectMapper);

        String validGrammar = "grammar Test; start: 'hello' 'world';";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", validGrammar);

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("validate_grammar")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = validateGrammarTool.execute(null, request);

        assertNotNull(result);
        assertNotNull(result.content());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertTrue(validationResult.isSuccess());
        assertEquals("Test", validationResult.getGrammarName());
    }

    @Test
    public void testValidateGrammarToolWithInvalidGrammar() throws Exception {
        ValidateGrammarTool validateGrammarTool = new ValidateGrammarTool(grammarInterpreter, errorTransformer, objectMapper);

        String invalidGrammar = "grammar Test; start: undefinedRule;";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", invalidGrammar);

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("validate_grammar")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = validateGrammarTool.execute(null, request);

        assertNotNull(result);
        assertNotNull(result.content());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);
        assertFalse(validationResult.isSuccess());
        assertFalse(validationResult.getErrors().isEmpty());
    }
}
