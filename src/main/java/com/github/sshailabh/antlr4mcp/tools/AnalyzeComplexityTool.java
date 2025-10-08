package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ComplexityMetrics;
import com.github.sshailabh.antlr4mcp.service.GrammarComplexityAnalyzer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP tool for analyzing ANTLR4 grammar complexity metrics.
 * Provides detailed analysis of grammar structure including rule counts,
 * alternatives, decision points, and call graph metrics.
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeComplexityTool {

    private final GrammarComplexityAnalyzer analyzer;
    private final ObjectMapper objectMapper;

    /**
     * Build MCP tool schema definition.
     */
    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
                .name("analyze_complexity")
                .description("Analyzes complexity metrics of an ANTLR4 grammar. " +
                           "Provides rule counts, alternatives per rule, decision points, " +
                           "call graph depth, and fan-in/fan-out metrics.")
                .inputSchema(getInputSchema())
                .build();
    }

    /**
     * Define input schema for the tool.
     */
    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar content to analyze");
        properties.put("grammar_text", grammarText);

        return new McpSchema.JsonSchema(
                "object",
                properties,
                java.util.List.of("grammar_text"),
                null,
                null,
                null
        );
    }

    /**
     * Execute complexity analysis.
     */
    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammar_text");

            log.info("analyze_complexity invoked, grammar size: {} bytes", grammarText.length());

            // Perform complexity analysis
            ComplexityMetrics metrics = analyzer.analyze(grammarText);

            // Serialize result to JSON
            String jsonResult = objectMapper.writeValueAsString(metrics);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for complexity analysis", e);
            return createErrorResult("invalid_input", e.getMessage());
        } catch (Exception e) {
            log.error("analyze_complexity failed", e);
            return createErrorResult("analysis_error",
                e.getMessage() != null ? e.getMessage() : "Failed to analyze grammar complexity");
        }
    }

    /**
     * Create error result with consistent structure.
     */
    private McpSchema.CallToolResult createErrorResult(String errorType, String message) {
        try {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", errorType);
            errorResult.put("message", message);

            String jsonResult = objectMapper.writeValueAsString(errorResult);
            return new McpSchema.CallToolResult(jsonResult, true);
        } catch (Exception ex) {
            log.error("Failed to serialize error", ex);
            return new McpSchema.CallToolResult(
                    "{\"error\":\"internal_error\",\"message\":\"" +
                    (ex.getMessage() != null ? ex.getMessage().replace("\"", "\\\"") : "Internal error") + "\"}",
                    true
            );
        }
    }
}
