package com.github.sshailabh.antlr4mcp.analysis;

import com.github.sshailabh.antlr4mcp.util.GrammarNameExtractor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.RuleAST;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Analyzes grammar call graphs to show rule dependencies and invocation hierarchy
 */
@Service
@Slf4j
public class CallGraphAnalyzer {

    /**
     * Analyze grammar and build call graph
     */
    public CallGraph analyzeGrammar(String grammarText) {
        log.info("Analyzing grammar call graph");

        try {
            // Parse grammar using ANTLR Tool
            Tool tool = new Tool();
            tool.errMgr.setFormat("antlr");

            Grammar grammar = loadGrammarFromText(grammarText, tool);
            if (grammar == null) {
                log.error("Failed to load grammar");
                return CallGraph.builder().build();
            }

            // Build call graph
            Map<String, CallGraphNode> nodeMap = new HashMap<>();
            List<CallGraphEdge> edges = new ArrayList<>();

            // Create nodes for all rules
            for (Rule rule : grammar.rules.values()) {
                CallGraphNode.RuleType ruleType = determineRuleType(rule);
                CallGraphNode node = CallGraphNode.builder()
                    .ruleName(rule.name)
                    .type(ruleType)
                    .build();
                nodeMap.put(rule.name, node);
            }

            // Extract call relationships
            for (Rule rule : grammar.rules.values()) {
                extractCallRelationships(rule, nodeMap, edges);
            }

            // Detect cycles
            List<String> cycles = detectCycles(nodeMap);

            // Mark recursive rules
            markRecursiveRules(nodeMap, cycles);

            // Calculate depths from start rule
            String startRule = findStartRule(grammar);
            Map<String, Integer> depths = calculateDepths(nodeMap, startRule);

            // Mark unused rules
            markUnusedRules(nodeMap, startRule);

            // Count statistics
            int unusedCount = (int) nodeMap.values().stream()
                .filter(CallGraphNode::isUnused)
                .count();

            return CallGraph.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .edges(edges)
                .cycles(cycles)
                .depths(depths)
                .startRule(startRule)
                .totalRules(nodeMap.size())
                .unusedRules(unusedCount)
                .build();

        } catch (Exception e) {
            log.error("Error analyzing call graph", e);
            return CallGraph.builder().build();
        }
    }

    /**
     * Load grammar from text using ANTLR Tool.
     * Note: Tool.loadGrammar() performs additional processing beyond Grammar.load().
     */
    private Grammar loadGrammarFromText(String grammarText, Tool tool) {
        Path tempDir = null;
        try {
            // Extract grammar name using centralized utility
            String grammarName = GrammarNameExtractor.extractGrammarName(grammarText);
            if (grammarName == null) {
                log.error("Could not extract grammar name from content");
                return null;
            }

            // Create temporary file for ANTLR processing
            tempDir = Files.createTempDirectory("antlr-callgraph-");
            Path tempFile = tempDir.resolve(grammarName + ".g4");
            Files.writeString(tempFile, grammarText);

            // Load and process grammar
            tool.outputDirectory = tempDir.toString();
            tool.inputDirectory = tempFile.getParent().toFile();
            Grammar grammar = tool.loadGrammar(tempFile.toString());

            if (grammar != null) {
                tool.process(grammar, false);
            }

            return grammar;
        } catch (Exception e) {
            log.error("Failed to load grammar for call graph analysis", e);
            return null;
        } finally {
            // Clean up temp directory using NIO for better error handling
            if (tempDir != null) {
                cleanupTempDirectory(tempDir);
            }
        }
    }

    /**
     * Clean up temporary directory using NIO for proper error handling.
     * Uses reverse-ordered deletion to handle nested directories.
     */
    private void cleanupTempDirectory(Path tempDir) {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         log.warn("Failed to delete temp file: {}", path, e);
                     }
                 });
        } catch (IOException e) {
            log.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    /**
     * Determine rule type (PARSER, LEXER, FRAGMENT)
     */
    private CallGraphNode.RuleType determineRuleType(Rule rule) {
        // Lexer rules start with uppercase
        if (Character.isUpperCase(rule.name.charAt(0))) {
            // Check if fragment using Rule.isFragment() method (more reliable)
            if (rule.isFragment()) {
                return CallGraphNode.RuleType.FRAGMENT;
            }
            return CallGraphNode.RuleType.LEXER;
        }
        return CallGraphNode.RuleType.PARSER;
    }

    /**
     * Extract call relationships from rule
     */
    private void extractCallRelationships(Rule rule, Map<String, CallGraphNode> nodeMap,
                                          List<CallGraphEdge> edges) {
        Set<String> calledRules = new HashSet<>();

        if (rule.ast != null) {
            extractRuleReferences(rule.ast, calledRules);
        }

        CallGraphNode callerNode = nodeMap.get(rule.name);
        if (callerNode != null) {
            for (String calledRule : calledRules) {
                // Add to caller's calls list
                if (!callerNode.getCalls().contains(calledRule)) {
                    callerNode.getCalls().add(calledRule);
                }

                // Add to callee's calledBy list
                CallGraphNode calleeNode = nodeMap.get(calledRule);
                if (calleeNode != null && !calleeNode.getCalledBy().contains(rule.name)) {
                    calleeNode.getCalledBy().add(rule.name);
                }

                // Create edge
                CallGraphEdge edge = CallGraphEdge.builder()
                    .from(rule.name)
                    .to(calledRule)
                    .invocationCount(1)
                    .build();
                edges.add(edge);
            }
        }
    }

    /**
     * Extract rule references from AST
     */
    private void extractRuleReferences(GrammarAST ast, Set<String> references) {
        if (ast == null) return;

        // Check if this node is a rule reference
        if (ast.getType() == org.antlr.v4.parse.ANTLRParser.RULE_REF) {
            references.add(ast.getText());
        }

        // Recursively check children
        if (ast.getChildren() != null) {
            for (Object child : ast.getChildren()) {
                if (child instanceof GrammarAST) {
                    extractRuleReferences((GrammarAST) child, references);
                }
            }
        }
    }

    /**
     * Detect cycles in call graph using DFS
     */
    private List<String> detectCycles(Map<String, CallGraphNode> nodeMap) {
        List<String> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String ruleName : nodeMap.keySet()) {
            if (!visited.contains(ruleName)) {
                detectCyclesDFS(ruleName, nodeMap, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }

        return cycles;
    }

    private void detectCyclesDFS(String current, Map<String, CallGraphNode> nodeMap,
                                 Set<String> visited, Set<String> recursionStack,
                                 List<String> path, List<String> cycles) {
        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        CallGraphNode node = nodeMap.get(current);
        if (node != null) {
            for (String calledRule : node.getCalls()) {
                if (!visited.contains(calledRule)) {
                    detectCyclesDFS(calledRule, nodeMap, visited, recursionStack, path, cycles);
                } else if (recursionStack.contains(calledRule)) {
                    // Found a cycle
                    int cycleStart = path.indexOf(calledRule);
                    List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycle.add(calledRule); // Complete the cycle
                    String cycleStr = String.join(" -> ", cycle);
                    if (!cycles.contains(cycleStr)) {
                        cycles.add(cycleStr);
                    }
                }
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(current);
    }

    /**
     * Mark recursive rules based on detected cycles
     */
    private void markRecursiveRules(Map<String, CallGraphNode> nodeMap, List<String> cycles) {
        Set<String> recursiveRules = new HashSet<>();

        for (String cycle : cycles) {
            String[] rules = cycle.split(" -> ");
            Collections.addAll(recursiveRules, rules);
        }

        for (String ruleName : recursiveRules) {
            CallGraphNode node = nodeMap.get(ruleName);
            if (node != null) {
                node.setRecursive(true);
            }
        }
    }

    /**
     * Find the start rule (first parser rule)
     */
    private String findStartRule(Grammar grammar) {
        for (Rule rule : grammar.rules.values()) {
            // First lowercase rule is typically the start rule
            if (Character.isLowerCase(rule.name.charAt(0))) {
                return rule.name;
            }
        }
        return grammar.rules.isEmpty() ? null : grammar.rules.values().iterator().next().name;
    }

    /**
     * Calculate depths from start rule using BFS
     */
    private Map<String, Integer> calculateDepths(Map<String, CallGraphNode> nodeMap, String startRule) {
        Map<String, Integer> depths = new HashMap<>();

        if (startRule == null || !nodeMap.containsKey(startRule)) {
            return depths;
        }

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(startRule);
        visited.add(startRule);
        depths.put(startRule, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depths.get(current);
            CallGraphNode node = nodeMap.get(current);

            if (node != null) {
                node.setDepth(currentDepth);

                for (String calledRule : node.getCalls()) {
                    if (!visited.contains(calledRule)) {
                        visited.add(calledRule);
                        depths.put(calledRule, currentDepth + 1);
                        queue.offer(calledRule);
                    }
                }
            }
        }

        return depths;
    }

    /**
     * Mark rules that are not reachable from start rule
     */
    private void markUnusedRules(Map<String, CallGraphNode> nodeMap, String startRule) {
        if (startRule == null) return;

        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.offer(startRule);
        reachable.add(startRule);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            CallGraphNode node = nodeMap.get(current);

            if (node != null) {
                for (String calledRule : node.getCalls()) {
                    if (!reachable.contains(calledRule)) {
                        reachable.add(calledRule);
                        queue.offer(calledRule);
                    }
                }
            }
        }

        // Mark unreachable rules as unused
        for (CallGraphNode node : nodeMap.values()) {
            if (!reachable.contains(node.getRuleName())) {
                node.setUnused(true);
            }
        }
    }

    /**
     * Generate DOT format for Graphviz
     */
    public String toDOT(CallGraph graph) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph CallGraph {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box, style=rounded, fontname=\"Arial\"];\n\n");

        // Add nodes
        for (CallGraphNode node : graph.getNodes()) {
            String color = getNodeColor(node);
            String shape = getNodeShape(node.getType());
            String style = node.isUnused() ? "dashed,filled" : "filled";

            dot.append(String.format("  \"%s\" [fillcolor=\"%s\", shape=%s, style=\"%s\"];\n",
                node.getRuleName(), color, shape, style));
        }

        dot.append("\n");

        // Add edges
        for (CallGraphEdge edge : graph.getEdges()) {
            dot.append(String.format("  \"%s\" -> \"%s\";\n", edge.getFrom(), edge.getTo()));
        }

        dot.append("}\n");
        return dot.toString();
    }

    private String getNodeColor(CallGraphNode node) {
        if (node.isUnused()) return "lightgray";
        if (node.isRecursive()) return "lightyellow";

        switch (node.getType()) {
            case PARSER: return "lightblue";
            case LEXER: return "lightgreen";
            case FRAGMENT: return "lightyellow";
            default: return "white";
        }
    }

    private String getNodeShape(CallGraphNode.RuleType type) {
        switch (type) {
            case PARSER: return "box";
            case LEXER: return "ellipse";
            case FRAGMENT: return "diamond";
            default: return "box";
        }
    }

    /**
     * Generate Mermaid format for diagrams
     */
    public String toMermaid(CallGraph graph) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("graph LR\n");

        // Add nodes and edges
        for (CallGraphEdge edge : graph.getEdges()) {
            CallGraphNode fromNode = graph.getNodes().stream()
                .filter(n -> n.getRuleName().equals(edge.getFrom()))
                .findFirst()
                .orElse(null);

            CallGraphNode toNode = graph.getNodes().stream()
                .filter(n -> n.getRuleName().equals(edge.getTo()))
                .findFirst()
                .orElse(null);

            if (fromNode != null && toNode != null) {
                String fromLabel = formatMermaidNode(fromNode);
                String toLabel = formatMermaidNode(toNode);
                mermaid.append(String.format("  %s --> %s\n", fromLabel, toLabel));
            }
        }

        return mermaid.toString();
    }

    private String formatMermaidNode(CallGraphNode node) {
        String prefix = "";
        if (node.isRecursive()) prefix += "⟳ ";
        if (node.isUnused()) prefix += "✗ ";

        return String.format("%s[\"%s%s\"]",
            node.getRuleName().replaceAll("[^a-zA-Z0-9]", "_"),
            prefix,
            node.getRuleName());
    }
}
