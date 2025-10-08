package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.LeftRecursionAnalysis;
import com.github.sshailabh.antlr4mcp.service.GrammarInterpreter;
import com.github.sshailabh.antlr4mcp.model.InterpreterResult;
import com.github.sshailabh.antlr4mcp.util.GrammarNameExtractor;
import com.github.sshailabh.antlr4mcp.util.FileSystemUtils;
import org.antlr.v4.Tool;
import java.nio.file.Files;
import java.nio.file.Path;
import com.github.sshailabh.antlr4mcp.model.LeftRecursiveRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.Tool;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes ANTLR4 grammars for left-recursion patterns.
 * Detects direct left-recursion and identifies rules transformed by ANTLR.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeftRecursionAnalyzer {

    private final GrammarInterpreter grammarInterpreter;

    /**
     * Analyze left-recursion in the grammar.
     *
     * @param grammarText The complete ANTLR4 grammar text
     * @return LeftRecursionAnalysis containing all findings
     */
    public LeftRecursionAnalysis analyze(String grammarText) {
        log.info("Analyzing grammar for left-recursion (size: {} bytes)", grammarText.length());

        if (grammarText == null || grammarText.trim().isEmpty()) {
            throw new IllegalArgumentException("Grammar text cannot be null or empty");
        }

        try {
            // Try using GrammarInterpreter first for 10-100x performance improvement
            Grammar grammar;
            try {
                InterpreterResult interpreterResult = grammarInterpreter.createInterpreter(grammarText);
                grammar = interpreterResult.getGrammar();
                log.debug("Using GrammarInterpreter for left-recursion analysis (fast path)");
            } catch (Exception e) {
                log.warn("GrammarInterpreter failed, falling back to full compilation: {}", e.getMessage());
                // Fallback to full compilation if interpreter fails
                grammar = loadGrammarWithFullCompilation(grammarText);
                if (grammar == null) {
                    throw new IllegalStateException("Failed to load grammar for left-recursion analysis");
                }
            }

            // Build analysis
            LeftRecursionAnalysis analysis = LeftRecursionAnalysis.builder().build();
            int totalParserRules = 0;

            // Analyze each parser rule
            for (Rule rule : grammar.rules.values()) {
                if (Character.isLowerCase(rule.name.charAt(0))) {
                    totalParserRules++;

                    LeftRecursiveRule lrr = analyzeRule(rule, grammar);
                    if (lrr != null) {
                        analysis.getLeftRecursiveRules().add(lrr);

                        if (lrr.isTransformed()) {
                            analysis.getTransformedRules().add(rule.name);
                        }
                    }
                }
            }

            analysis.setTotalRules(totalParserRules);
            analysis.setLeftRecursiveCount(analysis.getLeftRecursiveRules().size());
            analysis.setTransformedCount(analysis.getTransformedRules().size());

            log.info("Left-recursion analysis complete: {} left-recursive rules found ({} transformed)",
                    analysis.getLeftRecursiveCount(), analysis.getTransformedCount());

            return analysis;

        } catch (IllegalArgumentException e) {
            log.error("Invalid grammar for left-recursion analysis", e);
            throw e;
        } catch (Exception e) {
            log.error("Left-recursion analysis failed", e);
            throw new RuntimeException("Left-recursion analysis error: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze a single rule for left-recursion.
     */
    private LeftRecursiveRule analyzeRule(Rule rule, Grammar grammar) {
        // Check for transformation markers (precpred predicates)
        boolean isTransformed = isTransformed(rule);

        // Check for direct left-recursion
        boolean isDirect = isDirectLeftRecursive(rule, grammar);

        // Only report if left-recursive
        if (!isTransformed && !isDirect) {
            return null;
        }

        // Extract precedence levels if transformed
        List<Integer> precedenceLevels = new ArrayList<>();
        if (isTransformed) {
            precedenceLevels = extractPrecedenceLevels(rule);
        }

        return LeftRecursiveRule.builder()
                .ruleName(rule.name)
                .isDirect(isDirect)
                .isTransformed(isTransformed)
                .precedenceLevels(precedenceLevels)
                .alternatives(rule.numberOfAlts)
                .line(rule.ast != null ? rule.ast.getLine() : null)
                .build();
    }

    /**
     * Check if rule has been transformed by ANTLR (indicated by precpred predicates).
     */
    private boolean isTransformed(Rule rule) {
        if (rule.ast != null) {
            String astString = rule.ast.toStringTree();
            return astString.contains("precpred");
        }
        return false;
    }

    /**
     * Check if rule is directly left-recursive (first alternative starts with self-call).
     */
    private boolean isDirectLeftRecursive(Rule rule, Grammar grammar) {
        if (rule.index < 0 || rule.index >= grammar.atn.ruleToStartState.length) {
            return false;
        }

        RuleStartState startState = grammar.atn.ruleToStartState[rule.index];
        if (startState == null || startState.getNumberOfTransitions() == 0) {
            return false;
        }

        // Check first transition
        Transition firstTransition = startState.transition(0);
        ATNState target = firstTransition.target;

        // Follow epsilon transitions to find the first real transition
        while (target != null && target instanceof BasicState &&
               target.getNumberOfTransitions() > 0) {
            Transition nextTransition = target.transition(0);

            if (nextTransition instanceof RuleTransition) {
                RuleTransition rt = (RuleTransition) nextTransition;
                // Check if it calls itself
                return rt.ruleIndex == rule.index;
            }

            // For non-rule transitions, not left-recursive
            if (!(nextTransition instanceof EpsilonTransition)) {
                return false;
            }

            target = nextTransition.target;
        }

        return false;
    }

    /**
     * Extract precedence levels from precpred predicates.
     * Predicates have format: {precpred(_ctx, N)}?
     */
    private List<Integer> extractPrecedenceLevels(Rule rule) {
        List<Integer> levels = new ArrayList<>();

        if (rule.ast == null) {
            return levels;
        }

        String astString = rule.ast.toStringTree();
        Pattern pattern = Pattern.compile("precpred\\([^,]+,\\s*(\\d+)\\s*\\)");
        Matcher matcher = pattern.matcher(astString);

        while (matcher.find()) {
            try {
                int level = Integer.parseInt(matcher.group(1));
                if (!levels.contains(level)) {
                    levels.add(level);
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse precedence level: {}", matcher.group(1));
            }
        }

        Collections.sort(levels);
        return levels;
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
            Path tempDir = Files.createTempDirectory("antlr-grammar-leftrecursion-");
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
}
