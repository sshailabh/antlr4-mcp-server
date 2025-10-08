package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.LeftRecursionAnalysis;
import com.github.sshailabh.antlr4mcp.service.LeftRecursionAnalyzer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP tool for analyzing left-recursion in ANTLR4 grammars.
 * Identifies left-recursive rules and provides details about ANTLR's
 * automatic transformation process.
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeLeftRecursionTool {

    private final LeftRecursionAnalyzer analyzer;
    private final ObjectMapper objectMapper;

    /**
     * Build MCP tool schema definition.
     */
    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
                .name("analyze_left_recursion")
                .description("Analyzes left-recursion patterns in an ANTLR4 grammar. " +
                           "Identifies left-recursive rules, detects ANTLR's automatic transformations, " +
                           "and extracts precedence levels from transformed rules.")
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
     * Execute left-recursion analysis.
     */
    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammar_text");

            log.info("analyze_left_recursion invoked, grammar size: {} bytes",
                    grammarText != null ? grammarText.length() : 0);

            // Perform left-recursion analysis
            LeftRecursionAnalysis analysis = analyzer.analyze(grammarText);

            // Serialize result to JSON
            String jsonResult = objectMapper.writeValueAsString(analysis);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for left-recursion analysis", e);
            return createErrorResult("invalid_input", e.getMessage());
        } catch (Exception e) {
            log.error("analyze_left_recursion failed", e);
            return createErrorResult("analysis_error",
                e.getMessage() != null ? e.getMessage() : "Failed to analyze left-recursion");
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
