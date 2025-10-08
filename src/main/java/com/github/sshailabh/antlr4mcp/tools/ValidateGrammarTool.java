package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.InterpreterResult;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.service.ErrorTransformer;
import com.github.sshailabh.antlr4mcp.service.GrammarInterpreter;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.tool.Grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class ValidateGrammarTool {

    private final GrammarInterpreter grammarInterpreter;
    private final ErrorTransformer errorTransformer;
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

            // Use interpreter instead of compiler for 10-100x performance improvement
            InterpreterResult interpreterResult = grammarInterpreter.createInterpreter(grammarText);
            Grammar grammar = interpreterResult.getGrammar();

            // Check for empty grammar (only validation needed since interpreter catches other issues)
            List<GrammarError> validationErrors = new ArrayList<>();
            if (grammar.rules.isEmpty() && (grammar.getImplicitLexer() == null || grammar.getImplicitLexer().rules.isEmpty())) {
                validationErrors.add(GrammarError.builder()
                    .type("empty_grammar")
                    .message("Grammar has no rules defined")
                    .suggestedFix("Add at least one parser or lexer rule to the grammar")
                    .build());
            }

            // Count rules based on grammar type
            int lexerRules;
            int parserRules;
            String grammarType = interpreterResult.getGrammarType();

            if ("lexer".equals(grammarType)) {
                // For lexer grammars, grammar.rules contains lexer rules
                lexerRules = grammar.rules.size();
                parserRules = 0;
            } else {
                // For parser/combined grammars, grammar.rules contains parser rules
                parserRules = grammar.rules.size();
                // Count only explicitly defined lexer rules (not implicit ones from string literals)
                lexerRules = countExplicitLexerRules(grammarText);
            }

            boolean success = validationErrors.isEmpty();

            ValidationResult result = ValidationResult.builder()
                .success(success)
                .grammarName(interpreterResult.getGrammarName())
                .grammarType(interpreterResult.getGrammarType())
                .lexerRules(lexerRules)
                .parserRules(parserRules)
                .warnings(interpreterResult.getWarnings())
                .errors(validationErrors)
                .build();

            // Check expected name if provided
            if (expectedName != null && !expectedName.equals(result.getGrammarName())) {
                result.setSuccess(false);
                result.getErrors().add(GrammarError.builder()
                    .type("name_mismatch")
                    .message(String.format("Expected grammar name '%s' but found '%s'",
                                          expectedName, result.getGrammarName()))
                    .suggestedFix("Update grammar declaration to match expected name")
                    .build());
            }

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("validate_grammar failed", e);
            try {
                // Return ValidationResult with error instead of ErrorResponse
                // to maintain consistent API
                ValidationResult errorResult = ValidationResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("grammar_load_error")
                        .message(e.getMessage() != null ? e.getMessage() : "Failed to load grammar")
                        .suggestedFix("Check grammar syntax and structure")
                        .build()))
                    .build();
                String jsonResult = objectMapper.writeValueAsString(errorResult);
                return new McpSchema.CallToolResult(jsonResult, false);
            } catch (Exception ex) {
                log.error("Failed to serialize error", ex);
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"errors\":[{\"type\":\"internal_error\",\"message\":\"" +
                    (ex.getMessage() != null ? ex.getMessage().replace("\"", "\\\"") : "Internal error") + "\"}]}",
                    true
                );
            }
        }
    }

    /**
     * Count only explicitly defined lexer rules by parsing grammar text.
     * This avoids issues with interpreter mode where implicit tokens from string literals
     * are mixed with explicit lexer rules in the Grammar object.
     */
    private int countExplicitLexerRules(String grammarText) {
        // Pattern matches explicit lexer rule definitions:
        // - Optional "fragment" keyword
        // - Uppercase identifier (lexer rules must start with uppercase)
        // - Followed by colon
        // Example matches: "NUMBER :", "WS :", "fragment ESC :"
        Pattern lexerRulePattern = Pattern.compile("^(fragment\\s+)?[A-Z][A-Z0-9_]*\\s*:", Pattern.MULTILINE);
        Matcher matcher = lexerRulePattern.matcher(grammarText);

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
