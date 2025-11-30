package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ProfileResult;
import com.github.sshailabh.antlr4mcp.service.GrammarProfiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tool for profiling grammar performance.
 * 
 * Analyzes parsing decisions and provides insights for grammar optimization:
 * - Decision complexity (number of alternatives)
 * - Lookahead requirements (SLL vs LL predictions)
 * - Performance bottlenecks
 * - Optimization recommendations
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProfileGrammarTool {

    private final GrammarProfiler grammarProfiler;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("profile_grammar")
            .description("Profile grammar performance by parsing sample input. " +
                "Analyzes decision complexity, lookahead requirements, and provides optimization hints.")
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
        sampleInput.put("description", "Sample input to parse for profiling");
        properties.put("sample_input", sampleInput);

        Map<String, Object> startRule = new HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Start rule for parsing");
        properties.put("start_rule", startRule);

        return new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammar_text", "sample_input", "start_rule"),
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

            log.info("profile_grammar invoked, grammar size: {}, input size: {}",
                grammarText.length(), sampleInput.length());

            ProfileResult result = grammarProfiler.profile(grammarText, sampleInput, startRule);

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, !result.isSuccess());

        } catch (Exception e) {
            log.error("profile_grammar failed", e);
            try {
                ProfileResult errorResult = ProfileResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
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
