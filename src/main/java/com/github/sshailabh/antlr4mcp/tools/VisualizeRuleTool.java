package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.VisualizationResult;
import com.github.sshailabh.antlr4mcp.service.TreeVisualizer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class VisualizeRuleTool {

    private final TreeVisualizer treeVisualizer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("visualize_rule")
            .description("Generates visual representation of an ANTLR4 grammar rule as SVG or DOT format. " +
                       "Useful for understanding rule structure and dependencies. " +
                       "Note: Not implemented in M1, will be available in M2.")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar");
        properties.put("grammar_text", grammarText);

        Map<String, Object> ruleName = new HashMap<>();
        ruleName.put("type", "string");
        ruleName.put("description", "Name of the rule to visualize");
        properties.put("rule_name", ruleName);

        Map<String, Object> format = new HashMap<>();
        format.put("type", "string");
        format.put("description", "Output format: 'svg' or 'dot' (default: svg)");
        format.put("enum", new String[]{"svg", "dot"});
        properties.put("format", format);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            java.util.List.of("grammar_text", "rule_name"),
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
            String ruleName = (String) arguments.get("rule_name");
            String format = (String) arguments.getOrDefault("format", "svg");

            log.info("visualize_rule invoked for rule: {}, format: {}", ruleName, format);

            VisualizationResult result = treeVisualizer.visualize(grammarText, ruleName, format);

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("visualize_rule failed", e);
            try {
                VisualizationResult errorResult = VisualizationResult.builder()
                    .success(false)
                    .errors(java.util.List.of(com.github.sshailabh.antlr4mcp.model.GrammarError.builder()
                        .type("internal_error")
                        .message("Tool execution failed: " + e.getMessage())
                        .build()))
                    .build();
                String jsonResult = objectMapper.writeValueAsString(errorResult);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"errors\":[{\"type\":\"internal_error\",\"message\":\"" + ex.getMessage() + "\"}]}",
                    true
                );
            }
        }
    }
}
