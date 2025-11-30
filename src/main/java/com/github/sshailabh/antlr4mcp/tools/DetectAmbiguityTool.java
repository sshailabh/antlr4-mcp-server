package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.service.AmbiguityDetector;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DetectAmbiguityTool {

    private final AmbiguityDetector ambiguityDetector;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("detect_ambiguity")
            .description("Analyzes ANTLR4 grammar for ambiguities, conflicts, and prediction issues using static analysis. " +
                       "Reports line/column of ambiguous rules, conflicting alternatives, and lookahead conflicts with technical explanations.")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar to analyze for ambiguities");
        properties.put("grammar_text", grammarText);

        Map<String, Object> sampleInputs = new HashMap<>();
        sampleInputs.put("type", "array");
        Map<String, Object> items = new HashMap<>();
        items.put("type", "string");
        sampleInputs.put("items", items);
        sampleInputs.put("description", "Optional sample inputs to test for ambiguities");
        properties.put("sample_inputs", sampleInputs);

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
            @SuppressWarnings("unchecked")
            java.util.List<String> sampleInputs = arguments.containsKey("sample_inputs")
                ? (java.util.List<String>) arguments.get("sample_inputs")
                : null;

            log.info("detect_ambiguities invoked, grammar size: {} bytes, samples: {}",
                grammarText.length(), sampleInputs != null ? sampleInputs.size() : 0);

            AmbiguityReport report = ambiguityDetector.analyzeWithSamples(grammarText, sampleInputs);
            String jsonResult = objectMapper.writeValueAsString(report);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("detect_ambiguities failed", e);
            try {
                AmbiguityReport errorReport = AmbiguityReport.error("Tool execution failed: " + e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(errorReport);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult("{\"hasAmbiguities\":false,\"ambiguities\":[]}", true);
            }
        }
    }
}
