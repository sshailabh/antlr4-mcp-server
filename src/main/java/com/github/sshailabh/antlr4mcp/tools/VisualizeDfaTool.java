package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.DecisionVisualization;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.service.DecisionVisualizer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tool for visualizing decision points in ANTLR4 grammar rules.
 * Decision points are locations where the parser must choose between multiple alternatives.
 *
 * <p>This tool helps developers:
 * <ul>
 *   <li>Understand where parsing decisions occur in grammar rules</li>
 *   <li>Identify potential ambiguity sources</li>
 *   <li>Visualize the decision structure in DOT format</li>
 *   <li>Analyze decision complexity (alternatives, states, transitions)</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class VisualizeDfaTool {

    private final DecisionVisualizer decisionVisualizer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("visualize_dfa")
            .description("Visualizes decision points (DFA states) in an ANTLR4 grammar rule. " +
                       "Shows where the parser makes choices between alternatives, " +
                       "including DOT format graphs and decision metrics. " +
                       "Useful for understanding parsing decisions and identifying potential ambiguities.")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar text");
        properties.put("grammar_text", grammarText);

        Map<String, Object> ruleName = new HashMap<>();
        ruleName.put("type", "string");
        ruleName.put("description", "Name of the rule to analyze for decision points");
        properties.put("rule_name", ruleName);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammar_text", "rule_name"),
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

            // Validate required arguments
            if (grammarText == null || grammarText.trim().isEmpty()) {
                return createErrorResult("grammar_text is required and cannot be empty", true);
            }

            if (ruleName == null || ruleName.trim().isEmpty()) {
                return createErrorResult("rule_name is required and cannot be empty", true);
            }

            log.info("visualize_dfa invoked for rule: {} (grammar size: {} bytes)",
                    ruleName, grammarText.length());

            // Perform decision visualization
            DecisionVisualization result = decisionVisualizer.visualize(grammarText, ruleName);

            // Convert to JSON
            String jsonResult = objectMapper.writeValueAsString(result);

            log.info("visualize_dfa completed: found {} decisions in rule '{}'",
                    result.getTotalDecisions(), ruleName);

            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (IllegalArgumentException e) {
            log.warn("visualize_dfa validation error", e);
            return createErrorResult(e.getMessage(), true);

        } catch (Exception e) {
            log.error("visualize_dfa failed", e);
            return createErrorResult("Decision visualization failed: " + e.getMessage(), true);
        }
    }

    private McpSchema.CallToolResult createErrorResult(String message, boolean isError) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", message);
            errorResponse.put("errors", List.of(
                GrammarError.builder()
                    .type("visualization_error")
                    .message(message)
                    .build()
            ));

            String jsonResult = objectMapper.writeValueAsString(errorResponse);
            return new McpSchema.CallToolResult(jsonResult, isError);

        } catch (Exception ex) {
            log.error("Failed to create error result", ex);
            return new McpSchema.CallToolResult(
                "{\"success\":false,\"error\":\"" + message.replace("\"", "\\\"") + "\"}",
                true
            );
        }
    }
}
