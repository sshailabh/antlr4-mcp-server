package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.analysis.CallGraph;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphAnalyzer;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphNode;
import com.github.sshailabh.antlr4mcp.model.ComplexityMetrics;
import com.github.sshailabh.antlr4mcp.model.RuleComplexity;
import com.github.sshailabh.antlr4mcp.service.GrammarInterpreter;
import com.github.sshailabh.antlr4mcp.model.InterpreterResult;
import com.github.sshailabh.antlr4mcp.util.GrammarNameExtractor;
import com.github.sshailabh.antlr4mcp.util.FileSystemUtils;
import org.antlr.v4.Tool;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes ANTLR4 grammars to compute complexity metrics.
 * Provides both aggregate metrics and per-rule complexity analysis.
 *
 * <p>Complexity metrics include:
 * <ul>
 *   <li>Rule counts (parser, lexer, fragment)</li>
 *   <li>Alternatives per rule</li>
 *   <li>Decision points (branching complexity)</li>
 *   <li>Call graph depth</li>
 *   <li>Fan-in/fan-out (coupling metrics)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrammarComplexityAnalyzer {

    private final CallGraphAnalyzer callGraphAnalyzer;
    private final GrammarInterpreter grammarInterpreter;

    /**
     * Analyze grammar complexity metrics.
     *
     * @param grammarText The complete ANTLR4 grammar text
     * @return ComplexityMetrics containing all analysis results
     */
    public ComplexityMetrics analyze(String grammarText) {
        if (grammarText == null || grammarText.trim().isEmpty()) {
            throw new IllegalArgumentException("Grammar text cannot be null or empty");
        }

        log.info("Analyzing grammar complexity (size: {} bytes)", grammarText.length());

        try {
            // Get call graph for dependency metrics (fan-in/fan-out/depth/recursion)
            CallGraph callGraph = callGraphAnalyzer.analyzeGrammar(grammarText);
            if (callGraph == null) {
                throw new IllegalStateException("Failed to analyze call graph");
            }

            // Try using GrammarInterpreter first for 10-100x performance improvement
            Grammar grammar;
            try {
                InterpreterResult interpreterResult = grammarInterpreter.createInterpreter(grammarText);
                grammar = interpreterResult.getGrammar();
                log.debug("Using GrammarInterpreter for complexity analysis (fast path)");
            } catch (Exception e) {
                log.warn("GrammarInterpreter failed, falling back to full compilation: {}", e.getMessage());
                // Fallback to full compilation if interpreter fails
                grammar = loadGrammarWithFullCompilation(grammarText);
                if (grammar == null) {
                    throw new IllegalStateException("Failed to load grammar for complexity analysis");
                }
            }

            // Build metrics
            ComplexityMetrics metrics = ComplexityMetrics.builder().build();

            // Analyze parser rules
            for (Rule rule : grammar.rules.values()) {
                RuleComplexity ruleComplexity = analyzeRule(rule, grammar, callGraph);
                metrics.getRuleMetrics().put(rule.name, ruleComplexity);
            }

            // Analyze lexer rules if this is a combined grammar
            if (grammar.implicitLexer != null && grammar.implicitLexer.rules != null) {
                for (Rule rule : grammar.implicitLexer.rules.values()) {
                    RuleComplexity ruleComplexity = analyzeRule(rule, grammar.implicitLexer, callGraph);
                    metrics.getRuleMetrics().put(rule.name, ruleComplexity);
                }
            }

            // Calculate aggregate metrics
            calculateAggregates(metrics);

            log.info("Complexity analysis complete: {} total rules, {} parser rules, {} lexer rules",
                    metrics.getTotalRules(), metrics.getParserRules(), metrics.getLexerRules());

            return metrics;

        } catch (IllegalArgumentException e) {
            log.error("Invalid grammar for complexity analysis", e);
            throw e;
        } catch (Exception e) {
            log.error("Complexity analysis failed", e);
            throw new RuntimeException("Complexity analysis error: " + e.getMessage(), e);
        }
    }





    /**
     * Analyze complexity metrics for a single rule.
     */
    private RuleComplexity analyzeRule(Rule rule, Grammar grammar, CallGraph callGraph) {
        // Find corresponding call graph node
        CallGraphNode node = callGraph.getNodes().stream()
                .filter(n -> n.getRuleName().equals(rule.name))
                .findFirst()
                .orElse(null);

        // Count decision points in this rule's ATN
        int decisionPoints = countDecisionStates(rule, grammar);

        // Count alternatives more accurately
        int alternativesCount = countAlternatives(rule);

        // Determine rule type
        CallGraphNode.RuleType ruleType = node != null ? node.getType() : determineRuleType(rule);

        // Build rule complexity
        RuleComplexity.RuleComplexityBuilder builder = RuleComplexity.builder()
                .ruleName(rule.name)
                .alternatives(alternativesCount)
                .decisionPoints(decisionPoints)
                .type(ruleType);

        // Add call graph metrics if node exists
        if (node != null) {
            builder.fanIn(node.getCalledBy().size())
                    .fanOut(node.getCalls().size())
                    .depth(node.getDepth())
                    .recursive(node.isRecursive());
        } else {
            builder.fanIn(0).fanOut(0).depth(0).recursive(false);
        }

        return builder.build();
    }

    /**
     * Count decision states in a rule's ATN using BFS traversal.
     */
    private int countDecisionStates(Rule rule, Grammar grammar) {
        if (rule.index < 0 || rule.index >= grammar.atn.ruleToStartState.length) {
            log.warn("Invalid rule index for {}: {}", rule.name, rule.index);
            return 0;
        }

        RuleStartState startState = grammar.atn.ruleToStartState[rule.index];
        if (startState == null) {
            return 0;
        }

        int count = 0;
        Set<Integer> visited = new HashSet<>();
        Queue<ATNState> queue = new LinkedList<>();
        queue.add(startState);

        while (!queue.isEmpty()) {
            ATNState state = queue.poll();

            if (visited.contains(state.stateNumber)) {
                continue;
            }
            visited.add(state.stateNumber);

            // Count if this is a decision state
            if (state instanceof DecisionState) {
                count++;
            }

            // Only traverse within this rule (don't follow RuleTransitions to other rules)
            // RuleStopState indicates end of this rule
            if (state.ruleIndex != rule.index) {
                continue;
            }

            // Add all transitions to queue
            for (int i = 0; i < state.getNumberOfTransitions(); i++) {
                Transition transition = state.transition(i);
                ATNState target = transition.target;

                if (!visited.contains(target.stateNumber) && target.ruleIndex == rule.index) {
                    queue.add(target);
                }
            }
        }

        return count;
    }

    /**
     * Fallback method for full grammar compilation when GrammarInterpreter fails.
     * This uses the same approach as the old GrammarLoader.loadGrammar().
     */
    private Grammar loadGrammarWithFullCompilation(String grammarText) {
        try {
            String grammarName = GrammarNameExtractor.extractGrammarName(grammarText);
            if (grammarName == null) {
                log.error("Could not extract grammar name from grammar text");
                return null;
            }

            // Create temporary file for ANTLR processing
            Path tempDir = Files.createTempDirectory("antlr-grammar-complexity-");
            Path tempFile = tempDir.resolve(grammarName + ".g4");
            Files.writeString(tempFile, grammarText);

            try {
                // Load grammar using ANTLR Tool (full compilation)
                Tool tool = new Tool();
                tool.errMgr.setFormat("antlr");
                tool.outputDirectory = tempDir.toString();
                tool.inputDirectory = tempFile.getParent().toFile();
                Grammar grammar = tool.loadGrammar(tempFile.toString());

                if (grammar != null) {
                    // Process grammar for complete analysis
                    tool.process(grammar, false);
                    log.debug("Successfully loaded and processed grammar with full compilation: {}", grammarName);
                } else {
                    log.error("Failed to load grammar with full compilation: {}", grammarName);
                }

                return grammar;

            } finally {
                // Cleanup temporary files
                FileSystemUtils.deleteDirectoryRecursively(tempDir.toFile());
            }

        } catch (Exception e) {
            log.error("Failed to load grammar with full compilation", e);
            return null;
        }
    }

    /**
     * Count alternatives in a rule, handling both simple and complex cases.
     *
     * ANTLR's numberOfAlts represents the structure from the grammar source.
     * For complexity metrics, numberOfAlts is the authoritative count.
     */
    private int countAlternatives(Rule rule) {
        // ANTLR's numberOfAlts is always correct for the grammar structure
        return rule.numberOfAlts > 0 ? rule.numberOfAlts : 1;
    }

    /**
     * Determine rule type (PARSER, LEXER, FRAGMENT) from rule characteristics.
     */
    private CallGraphNode.RuleType determineRuleType(Rule rule) {
        // Lexer rules start with uppercase
        if (Character.isUpperCase(rule.name.charAt(0))) {
            return rule.isFragment() ? CallGraphNode.RuleType.FRAGMENT : CallGraphNode.RuleType.LEXER;
        }
        return CallGraphNode.RuleType.PARSER;
    }

    /**
     * Calculate aggregate metrics from per-rule data.
     */
    private void calculateAggregates(ComplexityMetrics metrics) {
        Map<String, RuleComplexity> ruleMetrics = metrics.getRuleMetrics();

        if (ruleMetrics.isEmpty()) {
            metrics.setTotalRules(0);
            metrics.setParserRules(0);
            metrics.setLexerRules(0);
            metrics.setFragmentRules(0);
            metrics.setAvgAlternativesPerRule(0.0);
            metrics.setMaxRuleDepth(0);
            metrics.setTotalDecisionPoints(0);
            return;
        }

        int parserCount = 0;
        int lexerCount = 0;
        int fragmentCount = 0;
        int totalAlternatives = 0;
        int maxDepth = 0;
        int totalDecisions = 0;

        for (RuleComplexity rc : ruleMetrics.values()) {
            // Count by type
            if (rc.getType() == CallGraphNode.RuleType.PARSER) {
                parserCount++;
            } else if (rc.getType() == CallGraphNode.RuleType.LEXER) {
                lexerCount++;
            } else if (rc.getType() == CallGraphNode.RuleType.FRAGMENT) {
                fragmentCount++;
            }

            // Sum metrics
            totalAlternatives += rc.getAlternatives();
            totalDecisions += rc.getDecisionPoints();
            maxDepth = Math.max(maxDepth, rc.getDepth());
        }

        metrics.setTotalRules(ruleMetrics.size());
        metrics.setParserRules(parserCount);
        metrics.setLexerRules(lexerCount);
        metrics.setFragmentRules(fragmentCount);
        metrics.setAvgAlternativesPerRule((double) totalAlternatives / ruleMetrics.size());
        metrics.setMaxRuleDepth(maxDepth);
        metrics.setTotalDecisionPoints(totalDecisions);
    }
}
