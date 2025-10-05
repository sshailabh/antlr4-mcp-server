package com.github.sshailabh.antlr4mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.visualization.AtnVisualization;
import com.github.sshailabh.antlr4mcp.visualization.AtnVisualizer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.tool.Grammar;

import java.util.*;

/**
 * MCP Tool for visualizing ANTLR ATN (Augmented Transition Network) state machines
 */
@Slf4j
public class VisualizeAtnTool {

    private final GrammarCompiler grammarCompiler;
    private final AtnVisualizer atnVisualizer;
    private final ObjectMapper objectMapper;

    public VisualizeAtnTool(GrammarCompiler grammarCompiler,
                            AtnVisualizer atnVisualizer,
                            ObjectMapper objectMapper) {
        this.grammarCompiler = grammarCompiler;
        this.atnVisualizer = atnVisualizer;
        this.objectMapper = objectMapper;
    }

    public McpSchema.Tool toTool() {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> grammarText = new HashMap<>();
        grammarText.put("type", "string");
        grammarText.put("description", "The ANTLR4 grammar text");
        properties.put("grammarText", grammarText);

        Map<String, Object> ruleName = new HashMap<>();
        ruleName.put("type", "string");
        ruleName.put("description", "Name of the rule to visualize ATN for");
        properties.put("ruleName", ruleName);

        Map<String, Object> format = new HashMap<>();
        format.put("type", "string");
        format.put("enum", Arrays.asList("dot", "mermaid", "svg", "all"));
        format.put("default", "all");
        format.put("description", "Output format for ATN visualization");
        properties.put("format", format);

        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
            "object",
            properties,
            List.of("grammarText", "ruleName"),
            null,
            null,
            null
        );

        return McpSchema.Tool.builder()
            .name("visualize_atn")
            .description("Generate ATN (Augmented Transition Network) state machine visualization for a grammar rule. " +
                "The ATN is ANTLR's internal representation of grammar rules as state machines. Supports DOT, Mermaid, " +
                "and SVG output formats. Useful for understanding parser internals and debugging complex grammar rules.")
            .inputSchema(schema)
            .build();
    }

    public McpSchema.CallToolResult execute(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.arguments();

            String grammarText = (String) arguments.get("grammarText");
            String ruleName = (String) arguments.get("ruleName");
            String format = (String) arguments.getOrDefault("format", "all");

            log.info("Visualizing ATN for rule: {} in format: {}", ruleName, format);

            // Load grammar
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);

            // Validate rule exists
            if (grammar.getRule(ruleName) == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Rule not found: " + ruleName);
                error.put("availableRules", grammar.getRuleNames());
                String jsonResult = objectMapper.writeValueAsString(error);
                return new McpSchema.CallToolResult(jsonResult, true);
            }

            // Generate ATN visualization
            AtnVisualization visualization = atnVisualizer.visualize(grammar, ruleName);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ruleName", visualization.getRuleName());
            response.put("stateCount", visualization.getStateCount());
            response.put("transitionCount", visualization.getTransitionCount());

            // Include requested formats
            switch (format.toLowerCase()) {
                case "dot":
                    response.put("dot", visualization.getDotFormat());
                    break;
                case "mermaid":
                    response.put("mermaid", visualization.getMermaidFormat());
                    break;
                case "svg":
                    if (visualization.getSvgFormat() != null) {
                        response.put("svg", visualization.getSvgFormat());
                    } else {
                        response.put("warning", "SVG generation requires Graphviz to be installed");
                        response.put("dot", visualization.getDotFormat());
                    }
                    break;
                case "all":
                default:
                    response.put("dot", visualization.getDotFormat());
                    response.put("mermaid", visualization.getMermaidFormat());
                    if (visualization.getSvgFormat() != null) {
                        response.put("svg", visualization.getSvgFormat());
                    }
                    break;
            }

            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);

            return new McpSchema.CallToolResult(jsonResult, false);

        } catch (Exception e) {
            log.error("Failed to visualize ATN", e);
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", e.getMessage());
                String jsonResult = objectMapper.writeValueAsString(error);
                return new McpSchema.CallToolResult(jsonResult, true);
            } catch (Exception jsonError) {
                return new McpSchema.CallToolResult(
                    "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}",
                    true
                );
            }
        }
    }
}
