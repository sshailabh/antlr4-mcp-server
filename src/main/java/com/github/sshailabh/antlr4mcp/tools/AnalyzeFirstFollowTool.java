package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.FirstFollowReport;
import com.github.sshailabh.antlr4mcp.service.FirstFollowAnalyzer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool for computing FIRST and FOLLOW sets in ANTLR4 grammars.
 * 
 * FIRST and FOLLOW sets are fundamental concepts in parsing theory:
 * - FIRST(A): Set of terminals that can begin strings derived from A
 * - FOLLOW(A): Set of terminals that can appear immediately after A
 * 
 * This analysis is critical for:
 * - Understanding grammar structure and token flow
 * - Debugging LL(k) conflicts and ambiguities
 * - Optimizing parser lookahead decisions
 * - Building and verifying parse tables
 * 
 * Essential for compiler engineers building and optimizing parsers.
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeFirstFollowTool {

    private final FirstFollowAnalyzer firstFollowAnalyzer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar text to analyze");
        properties.put("grammar_text", grammarText);

        Map<String, Object> ruleName = new HashMap<>();
        ruleName.put("type", "string");
        ruleName.put("description", "Optional: Specific rule name to analyze (analyzes all rules if not specified)");
        properties.put("rule_name", ruleName);

        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammar_text"),
            null,
            null,
            null
        );

        return McpSchema.Tool.builder()
            .name("analyze_first_follow")
            .description("Compute FIRST and FOLLOW sets for ANTLR4 grammar rules. " +
                "FIRST sets show what tokens can start a rule's derivation. " +
                "FOLLOW sets show what tokens can appear after a rule. " +
                "Also analyzes decision points for lookahead conflicts. " +
                "Returns nullable rules, LL(1) conflicts, and ambiguous decisions. " +
                "Essential for understanding parser behavior and debugging conflicts.")
            .inputSchema(schema)
            .build();
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammar_text");
            String ruleName = arguments.containsKey("rule_name") ? 
                (String) arguments.get("rule_name") : null;

            log.info("analyze_first_follow invoked, grammar size: {} bytes, rule: {}", 
                grammarText.length(), ruleName != null ? ruleName : "all");

            FirstFollowReport report = firstFollowAnalyzer.analyze(grammarText, ruleName);

            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(report);
            return new McpSchema.CallToolResult(jsonResult, !report.isSuccess());

        } catch (Exception e) {
            log.error("analyze_first_follow failed", e);
            try {
                FirstFollowReport errorReport = FirstFollowReport.error("Tool execution failed: " + e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(errorReport);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception ex) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"error\":\"Internal error: " + ex.getMessage() + "\"}",
                    true
                );
            }
        }
    }
}

