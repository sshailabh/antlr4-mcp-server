package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import com.github.sshailabh.antlr4mcp.service.RuntimeAmbiguityDetector;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * MCP tool for detecting grammar ambiguities using runtime profiling (Phase 2).
 * Requires sample inputs to parse with ProfilingATNSimulator enabled.
 */
@Slf4j
@RequiredArgsConstructor
public class DetectAmbiguityTool {

    private final RuntimeAmbiguityDetector runtimeAmbiguityDetector;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("detect_ambiguity")
            .description("Detects grammar ambiguities using runtime profiling with ProfilingATNSimulator (Phase 2). " +
                       "Parses sample inputs with profiling enabled to identify actual runtime ambiguities. " +
                       "Reports conflicting alternatives, token positions, and provides actionable suggestions. " +
                       "Supports both manual sample inputs and auto-generation (Phase 2 Week 4).")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar to analyze for ambiguities");
        properties.put("grammar_text", grammarText);

        Map<String, Object> startRule = new HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Starting rule name for parsing samples (e.g., 'expr', 'statement')");
        properties.put("start_rule", startRule);

        Map<String, Object> sampleInputs = new HashMap<>();
        Map<String, Object> itemsSchema = new HashMap<>();
        itemsSchema.put("type", "string");
        sampleInputs.put("type", "array");
        sampleInputs.put("items", itemsSchema);
        sampleInputs.put("description", "List of sample inputs to parse for ambiguity detection (required if auto_generate=false)");
        properties.put("sample_inputs", sampleInputs);

        Map<String, Object> autoGenerate = new HashMap<>();
        autoGenerate.put("type", "boolean");
        autoGenerate.put("description", "Auto-generate test inputs (Phase 2 Week 4 - currently returns empty results)");
        autoGenerate.put("default", false);
        properties.put("auto_generate", autoGenerate);

        Map<String, Object> numSamples = new HashMap<>();
        numSamples.put("type", "integer");
        numSamples.put("description", "Number of samples to auto-generate (default: 10)");
        numSamples.put("default", 10);
        properties.put("num_samples", numSamples);

        Map<String, Object> timeoutPerSample = new HashMap<>();
        timeoutPerSample.put("type", "integer");
        timeoutPerSample.put("description", "Timeout in seconds per sample (default: 5)");
        timeoutPerSample.put("default", 5);
        properties.put("timeout_per_sample", timeoutPerSample);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            java.util.List.of("grammar_text", "start_rule"), // Only these are required
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
            String startRule = (String) arguments.get("start_rule");

            // Get optional parameters
            @SuppressWarnings("unchecked")
            List<String> sampleInputs = (List<String>) arguments.get("sample_inputs");
            Boolean autoGenerate = (Boolean) arguments.getOrDefault("auto_generate", false);
            Integer numSamples = (Integer) arguments.getOrDefault("num_samples", 10);
            Integer timeoutPerSample = (Integer) arguments.getOrDefault("timeout_per_sample", 5);

            log.info("detect_ambiguity invoked: grammar={} bytes, startRule={}, autoGenerate={}, samples={}",
                     grammarText.length(), startRule, autoGenerate,
                     sampleInputs != null ? sampleInputs.size() : 0);

            AmbiguityReport report;

            if (autoGenerate) {
                // Use auto-generation (Phase 2 Week 4 - placeholder)
                log.info("Auto-generating {} test inputs", numSamples);
                report = runtimeAmbiguityDetector.detectWithAutoGeneration(
                    grammarText, startRule, numSamples
                );
            } else {
                // Use provided sample inputs
                if (sampleInputs == null || sampleInputs.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Either provide 'sample_inputs' or set 'auto_generate' to true"
                    );
                }
                log.info("Parsing {} sample inputs", sampleInputs.size());
                report = runtimeAmbiguityDetector.detectWithSamples(
                    grammarText, startRule, sampleInputs, timeoutPerSample
                );
            }

            String jsonResult = objectMapper.writeValueAsString(report);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            try {
                AmbiguityReport errorReport = AmbiguityReport.error("Invalid arguments: " + e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(errorReport);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult(
                    "{\"hasAmbiguities\":false,\"ambiguities\":[],\"error\":\"Invalid arguments\"}",
                    true
                );
            }
        } catch (Exception e) {
            log.error("detect_ambiguity failed", e);
            try {
                AmbiguityReport errorReport = AmbiguityReport.error("Tool execution failed: " + e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(errorReport);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult(
                    "{\"hasAmbiguities\":false,\"ambiguities\":[],\"error\":\"Tool execution failed\"}",
                    true
                );
            }
        }
    }
}
