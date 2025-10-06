package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.GrammarError;
import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.model.VisualizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.Grammar;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visualizes parse trees for ANTLR4 grammars.
 * Supports multiple output formats: LISP, DOT, SVG.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TreeVisualizer {

    private final GrammarCompiler grammarCompiler;

    /**
     * Visualizes a parse tree for a grammar rule.
     *
     * @param grammarText  The complete ANTLR4 grammar text
     * @param ruleName     The start rule to parse
     * @param sampleInput  The input text to parse
     * @param format       Output format: "lisp", "dot", or "svg" (defaults to "lisp")
     * @return VisualizationResult containing the tree visualization
     */
    public VisualizationResult visualize(String grammarText, String ruleName,
                                        String sampleInput, String format) {
        log.info("Visualizing rule: {} in format: {}", ruleName, format);

        // Default to LISP format
        if (format == null || format.trim().isEmpty()) {
            format = "lisp";
        }

        try {
            // Validate inputs
            if (grammarText == null || grammarText.trim().isEmpty()) {
                return VisualizationResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("invalid_input")
                        .message("Grammar text cannot be null or empty")
                        .build()))
                    .build();
            }

            if (ruleName == null || ruleName.trim().isEmpty()) {
                return VisualizationResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("invalid_input")
                        .message("Rule name cannot be null or empty")
                        .build()))
                    .build();
            }

            if (sampleInput == null) {
                sampleInput = "";
            }

            // Load and process grammar
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return VisualizationResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("grammar_error")
                        .message("Failed to load grammar")
                        .build()))
                    .build();
            }

            grammar.tool.process(grammar, false);

            // Verify rule exists
            org.antlr.v4.tool.Rule rule = grammar.getRule(ruleName);
            if (rule == null) {
                return VisualizationResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("invalid_rule")
                        .message("Rule '" + ruleName + "' not found in grammar")
                        .build()))
                    .build();
            }

            // Create lexer and parser interpreters
            LexerInterpreter lexer = grammar.createLexerInterpreter(CharStreams.fromString(sampleInput));
            if (lexer == null) {
                return VisualizationResult.builder()
                    .success(false)
                    .errors(List.of(GrammarError.builder()
                        .type("lexer_error")
                        .message("Failed to create lexer for grammar")
                        .build()))
                    .build();
            }

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ParserInterpreter parser = grammar.createParserInterpreter(tokens);

            // Parse with the specified rule
            ParseTree tree = parser.parse(rule.index);

            // Generate visualization based on format
            String visualization;
            String actualFormat = format.toLowerCase();

            switch (actualFormat) {
                case "dot":
                    visualization = generateDotFormat(tree, grammar);
                    break;
                case "svg":
                    // SVG generation would require additional libraries
                    // For now, return DOT format with a note
                    visualization = generateDotFormat(tree, grammar);
                    actualFormat = "dot"; // Fallback to DOT
                    log.warn("SVG format not yet fully implemented, using DOT format");
                    break;
                case "lisp":
                default:
                    visualization = generateLispFormat(tree, grammar);
                    actualFormat = "lisp";
                    break;
            }

            return VisualizationResult.builder()
                .success(true)
                .format(actualFormat)
                .visualization(visualization)
                .ruleName(ruleName)
                .build();

        } catch (Exception e) {
            log.error("Visualization failed for rule '{}': {}", ruleName, e.getMessage(), e);
            return VisualizationResult.builder()
                .success(false)
                .errors(List.of(GrammarError.builder()
                    .type("visualization_error")
                    .message("Visualization error: " + e.getMessage())
                    .build()))
                .build();
        }
    }

    /**
     * Generate LISP-style tree representation
     */
    private String generateLispFormat(ParseTree tree, Grammar grammar) {
        // Use ANTLR's built-in Trees utility
        List<String> ruleNames = new ArrayList<>();
        for (org.antlr.v4.tool.Rule rule : grammar.rules.values()) {
            ruleNames.add(rule.name);
        }

        return Trees.toStringTree(tree, ruleNames);
    }

    /**
     * Generate DOT format for graph visualization
     */
    private String generateDotFormat(ParseTree tree, Grammar grammar) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ParseTree {\n");
        dot.append("  rankdir=TB;\n");
        dot.append("  node [shape=box];\n\n");

        int[] nodeCounter = {0};
        generateDotNodes(tree, grammar, dot, nodeCounter, -1);

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Recursively generate DOT nodes
     */
    private int generateDotNodes(ParseTree tree, Grammar grammar,
                                 StringBuilder dot, int[] nodeCounter, int parentId) {
        int currentId = nodeCounter[0]++;

        // Get node label
        String label;
        if (tree instanceof RuleContext) {
            RuleContext ruleContext = (RuleContext) tree;
            int ruleIndex = ruleContext.getRuleIndex();
            if (ruleIndex >= 0 && ruleIndex < grammar.rules.size()) {
                // Get rule name from grammar
                org.antlr.v4.tool.Rule[] rulesArray = grammar.rules.values().toArray(new org.antlr.v4.tool.Rule[0]);
                label = rulesArray[ruleIndex].name;
            } else {
                label = "rule_" + ruleIndex;
            }
        } else {
            // Terminal node
            label = tree.getText().replace("\"", "\\\"");
            if (label.length() > 20) {
                label = label.substring(0, 17) + "...";
            }
        }

        // Add node
        dot.append(String.format("  node%d [label=\"%s\"];\n", currentId, label));

        // Add edge from parent
        if (parentId >= 0) {
            dot.append(String.format("  node%d -> node%d;\n", parentId, currentId));
        }

        // Process children
        for (int i = 0; i < tree.getChildCount(); i++) {
            generateDotNodes(tree.getChild(i), grammar, dot, nodeCounter, currentId);
        }

        return currentId;
    }
}
