package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ErrorResponse;
import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.InterpreterResult;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.service.ErrorTransformer;
import com.github.sshailabh.antlr4mcp.service.GrammarInterpreter;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ParseSampleTool {

    private final GrammarInterpreter grammarInterpreter;
    private final ErrorTransformer errorTransformer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("parse_sample")
            .description("Parses sample input using the provided ANTLR4 grammar. " +
                       "Returns parse tree (LISP or JSON), tokens, and any parse errors. " +
                       "Supports LISP and JSON tree formats for comprehensive analysis.")
            .inputSchema(getInputSchema())
            .build();
    }

    private McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar");
        properties.put("grammar_text", grammarText);

        Map<String, Object> sampleInput = new HashMap<>();
        sampleInput.put("type", "string");
        sampleInput.put("description", "Input text to parse");
        properties.put("sample_input", sampleInput);

        Map<String, Object> startRule = new HashMap<>();
        startRule.put("type", "string");
        startRule.put("description", "Parser rule to start from");
        properties.put("start_rule", startRule);

        Map<String, Object> showTokens = new HashMap<>();
        showTokens.put("type", "boolean");
        showTokens.put("description", "Include token stream in output (default: false)");
        properties.put("show_tokens", showTokens);

        // NOTE: Phase 1 simplification - only LISP format supported
        // tree_format and visualize_tree parameters removed

        return new McpSchema.JsonSchema(
            "object",
            properties,
            java.util.List.of("grammar_text", "sample_input", "start_rule"),
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
            Object showTokensObj = arguments.get("show_tokens");
            boolean showTokens = showTokensObj instanceof Boolean ? (Boolean) showTokensObj : false;

            log.info("parse_sample invoked, grammar size: {}, input size: {}",
                grammarText.length(), sampleInput.length());

            // Use interpreter for 10-100x performance improvement
            InterpreterResult interpreterResult = grammarInterpreter.createInterpreter(grammarText);

            // Parse using interpreter
            ParseTree tree = grammarInterpreter.parse(
                interpreterResult.getGrammar(),
                sampleInput,
                startRule
            );

            // Generate LISP format (only format supported in Phase 1)
            String treeString = tree.toStringTree(interpreterResult.getGrammar().createParserInterpreter(null));

            // Generate tokens if requested
            String tokensString = null;
            if (showTokens) {
                CharStream charStream = CharStreams.fromString(sampleInput);
                LexerInterpreter lexer = grammarInterpreter.createLexerInterpreter(
                    interpreterResult.getGrammar(), charStream
                );
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                tokens.fill();
                StringBuilder sb = new StringBuilder();
                for (Token token : tokens.getTokens()) {
                    sb.append(token.toString()).append("\n");
                }
                tokensString = sb.toString();
            }

            ParseResult result = ParseResult.builder()
                .success(true)
                .parseTree(treeString)
                .tokens(tokensString)
                .warnings(interpreterResult.getWarnings())
                .build();

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("parse_sample failed", e);
            try {
                // Return ParseResult with error instead of ErrorResponse for consistency
                ParseResult errorResult = ParseResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("parse_error")
                        .message(e.getMessage() != null ? e.getMessage() : "Failed to parse input")
                        .suggestedFix("Check grammar syntax and input validity")
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
}
