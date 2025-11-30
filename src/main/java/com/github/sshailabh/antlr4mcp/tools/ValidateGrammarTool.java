package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ValidateGrammarTool {

    private final GrammarCompiler grammarCompiler;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("validate_grammar")
            .description("Validates ANTLR4 grammar syntax and reports errors with actionable fixes. " +
                       "Checks for syntax errors, undefined rules, and basic structural issues. ")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar content");
        properties.put("grammar_text", grammarText);

        Map<String, Object> grammarName = new HashMap<>();
        grammarName.put("type", "string");
        grammarName.put("description", "Optional: Expected grammar name for validation");
        properties.put("grammar_name", grammarName);

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
            String expectedName = (String) arguments.get("grammar_name");

            log.info("validate_grammar invoked, grammar size: {} bytes", grammarText.length());

            ValidationResult result = grammarCompiler.validate(grammarText);

            if (expectedName != null && result.isSuccess()) {
                if (!expectedName.equals(result.getGrammarName())) {
                    result.setSuccess(false);
                    result.getErrors().add(com.github.sshailabh.antlr4mcp.model.GrammarError.builder()
                        .type("name_mismatch")
                        .message(String.format("Expected grammar name '%s' but found '%s'",
                                              expectedName, result.getGrammarName()))
                        .suggestedFix("Update grammar declaration to match expected name")
                        .build());
                }
            }

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("validate_grammar failed", e);
            try {
                ValidationResult errorResult = ValidationResult.error("Tool execution failed: " + e.getMessage());
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
