package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.model.LeftRecursionReport;
import com.github.sshailabh.antlr4mcp.service.LeftRecursionAnalyzer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool for analyzing left recursion in ANTLR4 grammars.
 * 
 * Left recursion is a common pattern where a rule references itself
 * as its leftmost symbol. ANTLR4 automatically transforms left-recursive
 * rules, but understanding these patterns helps with:
 * - Grammar debugging
 * - Performance optimization
 * - Understanding precedence handling
 * 
 * This tool detects both direct (A -> A α) and indirect (A -> B, B -> A)
 * left recursion patterns.
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeLeftRecursionTool {

    private final LeftRecursionAnalyzer leftRecursionAnalyzer;
    private final ObjectMapper objectMapper;

    public McpSchema.Tool toTool() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "Complete ANTLR4 grammar text to analyze for left recursion");
        properties.put("grammar_text", grammarText);

        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammar_text"),
            null,
            null,
            null
        );

        return McpSchema.Tool.builder()
            .name("analyze_left_recursion")
            .description("Analyze left recursion patterns in an ANTLR4 grammar. " +
                "Detects direct left recursion (A -> A α) and indirect cycles. " +
                "Returns information about how ANTLR4 transforms recursive rules, " +
                "which helps understand precedence handling and optimize grammar performance. " +
                "Essential for compiler engineers debugging expression grammars.")
            .inputSchema(schema)
            .build();
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammar_text");

            log.info("analyze_left_recursion invoked, grammar size: {} bytes", grammarText.length());

            LeftRecursionReport report = leftRecursionAnalyzer.analyze(grammarText);

            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(report);
            return new McpSchema.CallToolResult(jsonResult, !report.isSuccess());

        } catch (Exception e) {
            log.error("analyze_left_recursion failed", e);
            try {
                LeftRecursionReport errorReport = LeftRecursionReport.error("Tool execution failed: " + e.getMessage());
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

