package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.AmbiguityVisualization;
import com.github.sshailabh.antlr4mcp.service.AmbiguityVisualizer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * MCP tool for visualizing ambiguities in ANTLR grammars
 */
@Slf4j
@RequiredArgsConstructor
public class VisualizeAmbiguitiesTool {

    private final AmbiguityVisualizer ambiguityVisualizer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("visualize_ambiguities")
            .description("Visualize ambiguities in ANTLR grammar parsing with alternative parse trees")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new java.util.HashMap<>();

        Map<String, Object> grammar = new java.util.HashMap<>();
        grammar.put("type", "string");
        grammar.put("description", "ANTLR4 grammar text");
        properties.put("grammar", grammar);

        Map<String, Object> input = new java.util.HashMap<>();
        input.put("type", "string");
        input.put("description", "Input text to parse");
        properties.put("input", input);

        Map<String, Object> startRule = new java.util.HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Optional start rule (default: first parser rule)");
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
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammar = (String) arguments.get("grammar");
            String input = (String) arguments.get("input");
            String startRule = arguments.containsKey("start_rule") ? (String) arguments.get("start_rule") : null;

            log.info("Visualizing ambiguities for input length: {}", input.length());

            AmbiguityVisualization result = ambiguityVisualizer.visualize(grammar, input, startRule);

            String responseJson = objectMapper.writeValueAsString(Map.of(
                "success", true,
                "visualization", result
            ));

            return new McpSchema.CallToolResult(responseJson, false);

        } catch (Exception e) {
            log.error("Error in visualize_ambiguities tool", e);

            try {
                String errorJson = objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "error", "Tool execution failed: " + e.getMessage()
                ));

                return new McpSchema.CallToolResult(errorJson, true);
            } catch (Exception jsonError) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"error\":\"Internal error\"}",
                    true
                );
            }
        }
    }
}
