package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.VisualizationResult;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.TreeVisualizer;
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

class VisualizeRuleToolTest {

    private VisualizeRuleTool visualizeRuleTool;
    private TreeVisualizer treeVisualizer;
    private GrammarCompiler grammarCompiler;
    private ObjectMapper objectMapper;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        treeVisualizer = new TreeVisualizer(grammarCompiler);
        objectMapper = new ObjectMapper();
        visualizeRuleTool = new VisualizeRuleTool(treeVisualizer, objectMapper);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    private String getContentText(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test
    void testToTool() {
        McpSchema.Tool tool = visualizeRuleTool.toTool();

        assertNotNull(tool);
        assertEquals("visualize_rule", tool.name());
        assertNotNull(tool.description());
        assertTrue(tool.description().contains("visual representation"));
        assertNotNull(tool.inputSchema());
    }

    @Test
    void testVisualizeSimpleRule() throws Exception {
        String grammar = "grammar Simple;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
        // Note: This feature is not implemented in M1, so it might return success=false
    }

    @Test
    void testVisualizeRuleFromCalcGrammar() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "expr");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }

    @Test
    void testVisualizeRuleWithSvgFormat() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' WORLD ;\n" +
                        "WORLD : 'world' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "start");
        arguments.put("format", "svg");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }

    @Test
    void testVisualizeRuleWithDotFormat() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' WORLD ;\n" +
                        "WORLD : 'world' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "start");
        arguments.put("format", "dot");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }

    @Test
    void testVisualizeNonExistentRule() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "nonexistent");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
        assertFalse(vizResult.isSuccess());
    }

    @Test
    void testVisualizeRuleWithInvalidGrammar() throws Exception {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined_rule ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "start");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
        assertFalse(vizResult.isSuccess());
    }

    @Test
    void testVisualizeComplexRule() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/Json.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "value");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }

    @Test
    void testVisualizeTermRule() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "term");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }

    @Test
    void testVisualizeFactorRule() throws Exception {
        Path grammarPath = Paths.get("src/test/resources/grammars/SimpleCalc.g4");
        String grammar = Files.readString(grammarPath);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "factor");

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }

    @Test
    void testVisualizeRuleWithDefaultFormat() throws Exception {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", grammar);
        arguments.put("rule_name", "start");
        // No format specified, should default to svg

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("visualize_rule", arguments);
        McpSchema.CallToolResult result = visualizeRuleTool.execute(mockExchange, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = getContentText(result);
        VisualizationResult vizResult = objectMapper.readValue(contentText, VisualizationResult.class);
        assertNotNull(vizResult);
    }
}
