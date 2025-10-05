package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ProfileResult;
import com.github.sshailabh.antlr4mcp.service.GrammarProfiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP tool for profiling ANTLR grammar performance during parsing.
 * Provides detailed statistics about decision-making, ambiguities, and timing.
 */
@Slf4j
@RequiredArgsConstructor
public class ProfileGrammarTool {
    private final GrammarProfiler grammarProfiler;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("profile_grammar")
            .description("Profile ANTLR grammar parsing performance with detailed decision statistics")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammar = new HashMap<>();
        grammar.put("type", "string");
        grammar.put("description", "The ANTLR grammar content to profile");
        properties.put("grammar", grammar);

        Map<String, Object> input = new HashMap<>();
        input.put("type", "string");
        input.put("description", "Sample input to parse for profiling");
        properties.put("input", input);

        Map<String, Object> startRule = new HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Name of the start rule for parsing (default: first parser rule)");
        properties.put("start_rule", startRule);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            java.util.List.of("grammar", "input"),
            null,
            null,
            null
        );
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammar = (String) arguments.get("grammar");
            String input = (String) arguments.get("input");
            String startRule = arguments.containsKey("start_rule")
                ? (String) arguments.get("start_rule")
                : null;

            log.debug("Profiling grammar with input length: {}", input.length());

            ProfileResult result = grammarProfiler.profile(grammar, input, startRule);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("profile", result);

            String responseJson = objectMapper.writeValueAsString(response);
            return new McpSchema.CallToolResult(responseJson, false);

        } catch (Exception e) {
            log.error("Error profiling grammar", e);
            try {
                String errorResult = objectMapper.writeValueAsString(
                    Map.of("success", false, "error", e.getMessage())
                );
                return new McpSchema.CallToolResult(errorResult, true);
            } catch (Exception jsonError) {
                return new McpSchema.CallToolResult(
                    "{\"success\": false, \"error\": \"Failed to serialize error\"}", true
                );
            }
        }
    }
}
