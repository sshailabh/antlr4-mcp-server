package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.analysis.CallGraph;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Tool for analyzing grammar call graph
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeCallGraphTool {

    private final CallGraphAnalyzer callGraphAnalyzer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("analyze_call_graph")
            .description("Analyze grammar rule call graph to show dependencies, detect cycles, and identify unused rules. "
                + "Supports DOT and Mermaid output formats for visualization.")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "The grammar text to analyze");
        properties.put("grammar_text", grammarText);

        Map<String, Object> outputFormat = new HashMap<>();
        outputFormat.put("type", "string");
        outputFormat.put("description", "Output format: json (default), dot, or mermaid");
        outputFormat.put("enum", java.util.List.of("json", "dot", "mermaid"));
        properties.put("output_format", outputFormat);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            java.util.List.of("grammar_text"),
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
            String outputFormat = arguments.containsKey("output_format")
                ? (String) arguments.get("output_format")
                : "json";

            log.info("analyze_call_graph invoked, grammar size: {} bytes, format: {}",
                    grammarText.length(), outputFormat);

            // Analyze call graph
            CallGraph graph = callGraphAnalyzer.analyzeGrammar(grammarText);

            // Format output based on requested format
            Object result;
            switch (outputFormat.toLowerCase()) {
                case "dot":
                    result = Map.of(
                        "success", true,
                        "format", "dot",
                        "content", callGraphAnalyzer.toDOT(graph),
                        "metadata", Map.of(
                            "totalRules", graph.getTotalRules(),
                            "unusedRules", graph.getUnusedRules(),
                            "cycles", graph.getCycles().size()
                        )
                    );
                    break;
                case "mermaid":
                    result = Map.of(
                        "success", true,
                        "format", "mermaid",
                        "content", callGraphAnalyzer.toMermaid(graph),
                        "metadata", Map.of(
                            "totalRules", graph.getTotalRules(),
                            "unusedRules", graph.getUnusedRules(),
                            "cycles", graph.getCycles().size()
                        )
                    );
                    break;
                case "json":
                default:
                    result = Map.of(
                        "success", true,
                        "graph", graph
                    );
                    break;
            }

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("Error analyzing call graph", e);
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
