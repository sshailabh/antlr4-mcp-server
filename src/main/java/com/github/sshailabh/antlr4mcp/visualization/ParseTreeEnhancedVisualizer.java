package com.github.sshailabh.antlr4mcp.visualization;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Enhanced parse tree visualizer with multiple output formats and syntax highlighting
 */
@Component
@Slf4j
public class ParseTreeEnhancedVisualizer {

    /**
     * Generate parse tree visualization with multiple formats
     */
    public ParseTreeVisualization visualize(ParseTree tree, Parser parser, VisualizationOptions options) {
        log.info("Generating enhanced parse tree visualization");

        String asciiTree = generateAsciiTree(tree, parser, options);
        Map<String, Object> jsonTree = generateJsonTree(tree, parser);
        String dotTree = generateDotTree(tree, parser);
        String lispTree = generateLispTree(tree, parser, options.isShowRuleIndices());

        return ParseTreeVisualization.builder()
            .asciiFormat(asciiTree)
            .jsonFormat(jsonTree)
            .dotFormat(dotTree)
            .lispFormat(lispTree)
            .nodeCount(countNodes(tree))
            .maxDepth(calculateDepth(tree))
            .build();
    }

    /**
     * Generate ASCII tree with optional syntax highlighting
     */
    private String generateAsciiTree(ParseTree tree, Parser parser, VisualizationOptions options) {
        StringBuilder sb = new StringBuilder();
        generateAsciiTreeRecursive(tree, parser, "", true, sb, options);
        return sb.toString();
    }

    private void generateAsciiTreeRecursive(ParseTree tree, Parser parser,
                                           String prefix, boolean isTail,
                                           StringBuilder sb, VisualizationOptions options) {
        String nodeName = getNodeName(tree, parser);
        String nodeText = getNodeText(tree);

        // Apply ANSI colors for syntax highlighting
        if (options.isSyntaxHighlighting()) {
            nodeName = applyAnsiColors(nodeName, tree);
        }

        sb.append(prefix);
        sb.append(isTail ? "└── " : "├── ");
        sb.append(nodeName);

        if (options.isShowText() && !nodeText.isEmpty()) {
            sb.append(" \"").append(escapeText(nodeText)).append("\"");
        }

        if (options.isShowPosition() && tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();
            sb.append(String.format(" [%d:%d]", token.getLine(), token.getCharPositionInLine()));
        }

        sb.append("\n");

        int childCount = tree.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ParseTree child = tree.getChild(i);
            boolean isLast = (i == childCount - 1);
            String childPrefix = prefix + (isTail ? "    " : "│   ");
            generateAsciiTreeRecursive(child, parser, childPrefix, isLast, sb, options);
        }
    }

    /**
     * Generate JSON tree structure
     */
    private Map<String, Object> generateJsonTree(ParseTree tree, Parser parser) {
        Map<String, Object> node = new HashMap<>();

        node.put("name", getNodeName(tree, parser));
        node.put("text", getNodeText(tree));
        node.put("type", tree instanceof RuleContext ? "rule" : "terminal");

        if (tree instanceof RuleContext) {
            RuleContext ctx = (RuleContext) tree;
            node.put("ruleIndex", ctx.getRuleIndex());
            node.put("ruleName", parser.getRuleNames()[ctx.getRuleIndex()]);
        } else if (tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();
            node.put("tokenType", token.getType());
            node.put("tokenName", parser.getVocabulary().getSymbolicName(token.getType()));
            node.put("line", token.getLine());
            node.put("column", token.getCharPositionInLine());
        }

        if (tree.getChildCount() > 0) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (int i = 0; i < tree.getChildCount(); i++) {
                children.add(generateJsonTree(tree.getChild(i), parser));
            }
            node.put("children", children);
        }

        return node;
    }

    /**
     * Generate DOT format for Graphviz
     */
    private String generateDotTree(ParseTree tree, Parser parser) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ParseTree {\n");
        dot.append("  rankdir=TB;\n");
        dot.append("  node [shape=box, style=rounded, fontname=\"Arial\"];\n");
        dot.append("  edge [fontname=\"Arial\"];\n\n");

        java.util.concurrent.atomic.AtomicInteger nodeId = new java.util.concurrent.atomic.AtomicInteger(0);
        generateDotTreeRecursive(tree, parser, nodeId, dot);

        dot.append("}\n");
        return dot.toString();
    }

    private int generateDotTreeRecursive(ParseTree tree, Parser parser,
                                        java.util.concurrent.atomic.AtomicInteger nodeId,
                                        StringBuilder dot) {
        int currentId = nodeId.getAndIncrement();
        String nodeName = getNodeName(tree, parser);
        String nodeText = getNodeText(tree);

        String label = nodeName;
        if (!nodeText.isEmpty()) {
            label += "\\n\"" + escapeForDot(nodeText) + "\"";
        }

        String color = tree instanceof TerminalNode ? "lightblue" : "lightgreen";
        String shape = tree instanceof TerminalNode ? "ellipse" : "box";

        dot.append(String.format("  n%d [label=\"%s\", fillcolor=\"%s\", style=filled, shape=%s];\n",
            currentId, label, color, shape));

        for (int i = 0; i < tree.getChildCount(); i++) {
            int childId = generateDotTreeRecursive(tree.getChild(i), parser, nodeId, dot);
            dot.append(String.format("  n%d -> n%d;\n", currentId, childId));
        }

        return currentId;
    }

    /**
     * Get node name (rule name or token name)
     */
    private String getNodeName(ParseTree tree, Parser parser) {
        if (tree instanceof RuleContext) {
            RuleContext ctx = (RuleContext) tree;
            return parser.getRuleNames()[ctx.getRuleIndex()];
        } else if (tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();
            String symbolicName = parser.getVocabulary().getSymbolicName(token.getType());
            return symbolicName != null ? symbolicName : "<EOF>";
        }
        return tree.getClass().getSimpleName();
    }

    /**
     * Get node text content
     */
    private String getNodeText(ParseTree tree) {
        if (tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();
            String text = token.getText();
            return text != null ? text : "";
        }
        return "";
    }

    /**
     * Apply ANSI color codes for terminal syntax highlighting
     */
    private String applyAnsiColors(String nodeName, ParseTree tree) {
        if (tree instanceof RuleContext) {
            return "\u001B[32m" + nodeName + "\u001B[0m"; // Green for rules
        } else if (tree instanceof TerminalNode) {
            return "\u001B[34m" + nodeName + "\u001B[0m"; // Blue for terminals
        }
        return nodeName;
    }

    /**
     * Escape text for display
     */
    private String escapeText(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Escape text for DOT format
     */
    private String escapeForDot(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\\\n")
                   .replace("\r", "\\\\r")
                   .replace("\t", "\\\\t");
    }

    /**
     * Count total nodes in tree
     */
    private int countNodes(ParseTree tree) {
        int count = 1;
        for (int i = 0; i < tree.getChildCount(); i++) {
            count += countNodes(tree.getChild(i));
        }
        return count;
    }

    /**
     * Calculate tree depth
     */
    private int calculateDepth(ParseTree tree) {
        if (tree.getChildCount() == 0) {
            return 1;
        }

        int maxChildDepth = 0;
        for (int i = 0; i < tree.getChildCount(); i++) {
            maxChildDepth = Math.max(maxChildDepth, calculateDepth(tree.getChild(i)));
        }

        return 1 + maxChildDepth;
    }

    /**
     * Generate LISP-style S-expression tree
     * Format: (ruleName:ruleIndex child1 child2 ...)
     * Example: (prog:1 (expr:2 (expr:3 10) + (expr:1 (expr:3 20) * (expr:3 30))) <EOF>)
     */
    private String generateLispTree(ParseTree tree, Parser parser, boolean includeRuleIndices) {
        if (includeRuleIndices) {
            return generateLispTreeWithIndices(tree, parser);
        } else {
            // Use ANTLR's built-in toStringTree method
            return tree.toStringTree(parser);
        }
    }

    /**
     * Generate LISP tree with rule indices
     */
    private String generateLispTreeWithIndices(ParseTree tree, Parser parser) {
        if (tree instanceof TerminalNode) {
            TerminalNode terminal = (TerminalNode) tree;
            Token token = terminal.getSymbol();
            String text = token.getText();
            if (text == null || text.isEmpty()) {
                return "<EOF>";
            }
            // Escape special characters
            if (text.contains(" ") || text.contains("(") || text.contains(")")) {
                return "\"" + escapeText(text) + "\"";
            }
            return text;
        }

        if (tree instanceof RuleContext) {
            RuleContext ctx = (RuleContext) tree;
            StringBuilder sb = new StringBuilder();
            sb.append("(");

            // Add rule name with index
            String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];
            int ruleIndex = ctx.getRuleIndex();
            sb.append(ruleName).append(":").append(ruleIndex);

            // Add children
            for (int i = 0; i < tree.getChildCount(); i++) {
                sb.append(" ");
                sb.append(generateLispTreeWithIndices(tree.getChild(i), parser));
            }

            sb.append(")");
            return sb.toString();
        }

        return tree.toStringTree(parser);
    }
}
