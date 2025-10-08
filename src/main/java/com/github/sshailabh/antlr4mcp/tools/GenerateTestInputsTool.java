package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.GeneratedTestInputs;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.service.TestInputGenerator;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tool for generating test inputs for ANTLR4 grammar rules.
 * Provides sample inputs that can be used for testing and validation.
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateTestInputsTool {

    private final TestInputGenerator testInputGenerator;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("generate_test_inputs")
            .description("Generates sample test inputs for an ANTLR4 grammar rule. " +
                       "Creates example strings that should be valid according to the grammar. " +
                       "Useful for testing parsers and understanding what inputs a rule accepts.")
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
        ruleName.put("description", "Name of the rule to generate test inputs for");
        properties.put("rule_name", ruleName);

        Map<String, Object> maxInputs = new HashMap<>();
        maxInputs.put("type", "integer");
        maxInputs.put("description", "Maximum number of test inputs to generate (default: 5)");
        maxInputs.put("default", 5);
        properties.put("max_inputs", maxInputs);

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
            Object maxInputsObj = arguments.getOrDefault("max_inputs", 5);
            int maxInputs = maxInputsObj instanceof Integer ? (Integer) maxInputsObj : 5;

            // Validate required arguments
            if (grammarText == null || grammarText.trim().isEmpty()) {
                return createErrorResult("grammar_text is required and cannot be empty", true);
            }

            if (ruleName == null || ruleName.trim().isEmpty()) {
                return createErrorResult("rule_name is required and cannot be empty", true);
            }

            log.info("generate_test_inputs invoked for rule: {} (max: {})", ruleName, maxInputs);

            // Generate test inputs
            GeneratedTestInputs result = testInputGenerator.generate(grammarText, ruleName, maxInputs);

            // Convert to JSON
            String jsonResult = objectMapper.writeValueAsString(result);

            log.info("generate_test_inputs completed: generated {} inputs for rule '{}'",
                    result.getTotalInputs(), ruleName);

            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (IllegalArgumentException e) {
            log.warn("generate_test_inputs validation error", e);
            return createErrorResult(e.getMessage(), true);

        } catch (Exception e) {
            log.error("generate_test_inputs failed", e);
            return createErrorResult("Test generation failed: " + e.getMessage(), true);
        }
    }

    private McpSchema.CallToolResult createErrorResult(String message, boolean isError) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", message);
            errorResponse.put("errors", List.of(
                GrammarError.builder()
                    .type("generation_error")
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
