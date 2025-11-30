package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.service.InterpreterParser;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool for parsing input using ANTLR4 grammars.
 * Uses fast interpreter mode (10-100x faster than compilation).
 */
@Slf4j
public class ParseSampleTool {

    private final InterpreterParser interpreterParser;
    private final ObjectMapper objectMapper;

    public ParseSampleTool(GrammarCompiler grammarCompiler, ObjectMapper objectMapper) {
        this.interpreterParser = new InterpreterParser(grammarCompiler);
        this.objectMapper = objectMapper;
    }

    public McpSchema.Tool toTool() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar text");
        properties.put("grammar_text", grammarText);

        Map<String, Object> sampleInput = new HashMap<>();
        sampleInput.put("type", "string");
        sampleInput.put("description", "Input text to parse");
        properties.put("sample_input", sampleInput);

        Map<String, Object> startRule = new HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Parser rule to start from (e.g., 'program', 'expression')");
        properties.put("start_rule", startRule);

        Map<String, Object> showTokens = new HashMap<>();
        showTokens.put("type", "boolean");
        showTokens.put("description", "Include token stream in output (default: true)");
        properties.put("show_tokens", showTokens);

        return McpSchema.Tool.builder()
            .name("parse_sample")
            .description("Parse input using ANTLR4 grammar. Uses fast interpreter mode. " +
                "Returns LISP-format parse tree, token stream, and parse errors. " +
                "Essential for testing grammar rules during development.")
            .inputSchema(new McpSchema.JsonSchema(
                "object", properties,
                List.of("grammar_text", "sample_input", "start_rule"),
                null, null, null))
            .build();
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = (Map<String, Object>) request.arguments();

            String grammarText = (String) args.get("grammar_text");
            String sampleInput = (String) args.get("sample_input");
            String startRule = (String) args.get("start_rule");
            boolean showTokens = args.get("show_tokens") == null || 
                                 Boolean.TRUE.equals(args.get("show_tokens"));

            log.info("parse_sample: grammar={}b, input={}b, rule={}", 
                grammarText.length(), sampleInput.length(), startRule);

            ParseResult result = interpreterParser.parseInterpreted(grammarText, sampleInput, startRule);
            
            if (!showTokens) {
                result.setTokens(null);
            }

            return new McpSchema.CallToolResult(objectMapper.writeValueAsString(result), false);

        } catch (Exception e) {
            log.error("parse_sample failed", e);
            try {
                ParseResult error = ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("internal_error")
                        .message("Parse failed: " + e.getMessage())
                        .build()))
                    .build();
                return new McpSchema.CallToolResult(objectMapper.writeValueAsString(error), true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"errors\":[{\"message\":\"" + ex.getMessage() + "\"}]}", true);
            }
        }
    }
}
