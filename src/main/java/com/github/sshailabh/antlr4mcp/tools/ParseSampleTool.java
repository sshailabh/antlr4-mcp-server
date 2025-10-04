package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ParseSampleTool {

    private final GrammarCompiler grammarCompiler;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        return McpSchema.Tool.builder()
            .name("parse_sample")
            .description("Parses sample input using the provided ANTLR4 grammar. " +
                       "Returns parse tree (LISP or JSON), tokens, and any parse errors. " +
                       "Optionally generates visual tree (not implemented in M1).")
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

        Map<String, Object> treeFormat = new HashMap<>();
        treeFormat.put("type", "string");
        treeFormat.put("description", "Parse tree format: 'tree' (LISP) or 'tokens' (default: tree)");
        treeFormat.put("enum", new String[]{"tree", "tokens"});
        properties.put("tree_format", treeFormat);

        Map<String, Object> visualizeTree = new HashMap<>();
        visualizeTree.put("type", "boolean");
        visualizeTree.put("description", "Generate visual tree (not implemented in M1)");
        properties.put("visualize_tree", visualizeTree);

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
            String treeFormat = (String) arguments.getOrDefault("tree_format", "tree");
            Object visualizeTreeObj = arguments.get("visualize_tree");
            boolean visualizeTree = visualizeTreeObj instanceof Boolean ? (Boolean) visualizeTreeObj : false;

            log.info("parse_sample invoked, grammar size: {}, input size: {}",
                grammarText.length(), sampleInput.length());

            ParseResult result = grammarCompiler.parse(grammarText, sampleInput, startRule,
                showTokens, treeFormat, visualizeTree);

            String jsonResult = objectMapper.writeValueAsString(result);
            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("parse_sample failed", e);
            try {
                ParseResult errorResult = ParseResult.builder()
                    .success(false)
                    .errors(java.util.List.of(com.github.sshailabh.antlr4mcp.model.GrammarError.builder()
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
