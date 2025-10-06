package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ParseTrace;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.service.ParseTracer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP tool for generating parse traces with step-by-step execution details.
 */
@Slf4j
@RequiredArgsConstructor
public class ParseWithTraceTool {

    private final ParseTracer parseTracer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("parse_with_trace")
            .description("Generates a step-by-step trace of the parsing process for debugging and understanding. " +
                       "Shows how the parser processes input including rule entries/exits and token consumption. " +
                       "Useful for debugging grammar issues and understanding parser behavior.")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar text");
        properties.put("grammar_text", grammarText);

        Map<String, Object> sampleInput = new HashMap<>();
        sampleInput.put("type", "string");
        sampleInput.put("description", "Sample input text to parse and trace");
        properties.put("sample_input", sampleInput);

        Map<String, Object> startRule = new HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Name of the start rule to use for parsing");
        properties.put("start_rule", startRule);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            java.util.List.of("grammar_text", "sample_input", "start_rule"),
            null,
            null,
            null
        );
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammar_text");
            String sampleInput = (String) arguments.get("sample_input");
            String startRule = (String) arguments.get("start_rule");

            log.info("parse_with_trace invoked for rule: {}, input length: {}",
                startRule, sampleInput != null ? sampleInput.length() : 0);

            ParseTrace result = parseTracer.trace(grammarText, sampleInput, startRule);

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("parse_with_trace failed", e);
            try {
                ParseTrace errorResult = ParseTrace.builder()
                    .success(false)
                    .errors(java.util.List.of(GrammarError.builder()
                        .type("internal_error")
                        .message("Tool execution failed: " + e.getMessage())
                        .build()))
                    .build();
                String jsonResult = objectMapper.writeValueAsString(errorResult);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"errors\":[{\"type\":\"internal_error\",\"message\":\"" +
                    ex.getMessage() + "\"}]}",
                    true
                );
            }
        }
    }
}