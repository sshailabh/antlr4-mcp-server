package com.github.sshailabh.antlr4mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.service.AmbiguityDetector;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.tools.DetectAmbiguityTool;
import com.github.sshailabh.antlr4mcp.tools.ParseSampleTool;
import com.github.sshailabh.antlr4mcp.tools.ValidateGrammarTool;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RealWorldGrammarTest {

    @Autowired
    private GrammarCompiler grammarCompiler;

    @Autowired
    private AmbiguityDetector ambiguityDetector;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CALCULATOR_GRAMMAR =
        "grammar Calculator;\n" +
        "\n" +
        "expr\n" +
        "    : expr ('*'|'/') expr       # MulDiv\n" +
        "    | expr ('+'|'-') expr       # AddSub\n" +
        "    | NUMBER                     # Num\n" +
        "    | '(' expr ')'              # Parens\n" +
        "    ;\n" +
        "\n" +
        "NUMBER : [0-9]+ ('.' [0-9]+)? ;\n" +
        "WS     : [ \\t\\r\\n]+ -> skip ;\n";

    private static final String JSON_GRAMMAR =
        "grammar JsonSubset;\n" +
        "\n" +
        "json\n" +
        "    : value\n" +
        "    ;\n" +
        "\n" +
        "value\n" +
        "    : object\n" +
        "    | array\n" +
        "    | STRING\n" +
        "    | NUMBER\n" +
        "    | 'true'\n" +
        "    | 'false'\n" +
        "    | 'null'\n" +
        "    ;\n" +
        "\n" +
        "object\n" +
        "    : '{' pair (',' pair)* '}'\n" +
        "    | '{' '}'\n" +
        "    ;\n" +
        "\n" +
        "pair\n" +
        "    : STRING ':' value\n" +
        "    ;\n" +
        "\n" +
        "array\n" +
        "    : '[' value (',' value)* ']'\n" +
        "    | '[' ']'\n" +
        "    ;\n" +
        "\n" +
        "STRING\n" +
        "    : '\"' ( ESC | ~[\"\\\\] )* '\"'\n" +
        "    ;\n" +
        "\n" +
        "fragment ESC\n" +
        "    : '\\\\' [\"\\\\/bfnrt]\n" +
        "    ;\n" +
        "\n" +
        "NUMBER\n" +
        "    : '-'? INT ('.' [0-9]+)? EXP?\n" +
        "    ;\n" +
        "\n" +
        "fragment INT\n" +
        "    : '0'\n" +
        "    | [1-9] [0-9]*\n" +
        "    ;\n" +
        "\n" +
        "fragment EXP\n" +
        "    : [Ee] [+\\-]? INT\n" +
        "    ;\n" +
        "\n" +
        "WS\n" +
        "    : [ \\t\\r\\n]+ -> skip\n" +
        "    ;\n";

    @Test
    public void testValidateCalculatorGrammar() throws Exception {
        ValidateGrammarTool tool = new ValidateGrammarTool(grammarCompiler, objectMapper);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("validate_grammar")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);

        assertTrue(validationResult.isSuccess());
        assertEquals("Calculator", validationResult.getGrammarName());
        assertTrue(validationResult.getErrors().isEmpty());
    }

    @Test
    public void testValidateJsonGrammar() throws Exception {
        ValidateGrammarTool tool = new ValidateGrammarTool(grammarCompiler, objectMapper);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", JSON_GRAMMAR);

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("validate_grammar")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ValidationResult validationResult = objectMapper.readValue(contentText, ValidationResult.class);

        assertTrue(validationResult.isSuccess());
        assertEquals("JsonSubset", validationResult.getGrammarName());
        assertTrue(validationResult.getErrors().isEmpty());
    }

    @Test
    public void testParseCalculatorExpression() throws Exception {
        ParseSampleTool tool = new ParseSampleTool(grammarCompiler, objectMapper);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);
        arguments.put("sample_input", "42 + 10 * 3");
        arguments.put("start_rule", "expr");

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("parse_sample")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);

        assertTrue(parseResult.isSuccess());
        assertNotNull(parseResult.getParseTree());
        assertTrue(parseResult.getParseTree().contains("expr"));
    }

    @Test
    public void testParseJsonObject() throws Exception {
        ParseSampleTool tool = new ParseSampleTool(grammarCompiler, objectMapper);

        String jsonInput = "{\"name\": \"test\", \"value\": 42, \"active\": true}";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", JSON_GRAMMAR);
        arguments.put("sample_input", jsonInput);
        arguments.put("start_rule", "json");

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("parse_sample")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);

        assertTrue(parseResult.isSuccess());
        assertNotNull(parseResult.getParseTree());
        assertTrue(parseResult.getParseTree().contains("object"));
    }

    @Test
    public void testParseJsonArray() throws Exception {
        ParseSampleTool tool = new ParseSampleTool(grammarCompiler, objectMapper);

        String jsonInput = "[1, 2, 3, \"test\", true, null]";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", JSON_GRAMMAR);
        arguments.put("sample_input", jsonInput);
        arguments.put("start_rule", "json");

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("parse_sample")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);

        assertTrue(parseResult.isSuccess());
        assertNotNull(parseResult.getParseTree());
        assertTrue(parseResult.getParseTree().contains("array"));
    }

    @Test
    public void testDetectCalculatorAmbiguity() throws Exception {
        DetectAmbiguityTool tool = new DetectAmbiguityTool(ambiguityDetector, objectMapper);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("detect_ambiguity")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        AmbiguityReport report = objectMapper.readValue(contentText, AmbiguityReport.class);

        assertNotNull(report);
        assertFalse(report.isHasAmbiguities());
        assertTrue(report.getAmbiguities().isEmpty());
    }

    @Test
    public void testParseCalculatorWithParentheses() throws Exception {
        ParseSampleTool tool = new ParseSampleTool(grammarCompiler, objectMapper);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", CALCULATOR_GRAMMAR);
        arguments.put("sample_input", "(10 + 20) * 3");
        arguments.put("start_rule", "expr");

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("parse_sample")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);

        assertTrue(parseResult.isSuccess());
        assertNotNull(parseResult.getParseTree());
        assertTrue(parseResult.getParseTree().contains("("));
        assertTrue(parseResult.getParseTree().contains(")"));
    }

    @Test
    public void testParseNestedJsonObject() throws Exception {
        ParseSampleTool tool = new ParseSampleTool(grammarCompiler, objectMapper);

        String jsonInput = "{\"user\": {\"name\": \"Alice\", \"age\": 30}, \"items\": [1, 2, 3]}";

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("grammar_text", JSON_GRAMMAR);
        arguments.put("sample_input", jsonInput);
        arguments.put("start_rule", "json");

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("parse_sample")
            .arguments(arguments)
            .build();

        McpSchema.CallToolResult result = tool.execute(null, request);

        assertNotNull(result);
        assertFalse(result.isError());

        String contentText = ((McpSchema.TextContent) result.content().get(0)).text();
        ParseResult parseResult = objectMapper.readValue(contentText, ParseResult.class);

        assertTrue(parseResult.isSuccess());
        assertNotNull(parseResult.getParseTree());
    }
}
