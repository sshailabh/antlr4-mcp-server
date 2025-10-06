package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.Ambiguity;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.AltAST;
import org.antlr.v4.tool.ast.GrammarAST;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Detects ambiguities in ANTLR4 grammars using static analysis.
 * Focuses on detecting common ambiguity patterns in grammar rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmbiguityDetector {

    private final GrammarCompiler grammarCompiler;

    /**
     * Analyzes a grammar for ambiguities using static pattern analysis.
     *
     * @param grammarText The complete ANTLR4 grammar text
     * @return AmbiguityReport containing detected ambiguities
     */
    public AmbiguityReport analyze(String grammarText) {
        log.info("Analyzing grammar for ambiguities (size: {} bytes)", grammarText.length());

        if (grammarText == null || grammarText.trim().isEmpty()) {
            throw new IllegalArgumentException("Grammar text cannot be null or empty");
        }

        try {
            // Load the grammar using ANTLR4 Tool
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                throw new IllegalStateException("Failed to load grammar for ambiguity analysis");
            }

            // Process grammar to get proper ATN
            grammar.tool.process(grammar, false);

            // Collect ambiguities using static analysis
            List<Ambiguity> ambiguities = new ArrayList<>();

            // Analyze each parser rule for potential ambiguities
            // Parser rules start with lowercase letters, lexer rules with uppercase
            for (Rule rule : grammar.rules.values()) {
                if (!rule.isFragment() && Character.isLowerCase(rule.name.charAt(0))) {
                    analyzeRule(rule, ambiguities);
                }
            }

            log.info("Ambiguity analysis complete: found {} ambiguities", ambiguities.size());

            return AmbiguityReport.builder()
                .hasAmbiguities(!ambiguities.isEmpty())
                .ambiguities(ambiguities)
                .build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid grammar for ambiguity analysis", e);
            throw e;
        } catch (Exception e) {
            log.error("Ambiguity detection failed", e);
            throw new RuntimeException("Ambiguity analysis error: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze a single rule for ambiguity patterns
     */
    private void analyzeRule(Rule rule, List<Ambiguity> ambiguities) {
        if (rule.ast == null || rule.numberOfAlts < 2) {
            // No ambiguity possible with less than 2 alternatives
            return;
        }

        try {
            // Check for left-recursion with multiple alternatives
            boolean hasLeftRecursion = detectLeftRecursion(rule);

            // Check for alternatives with common prefixes
            boolean hasCommonPrefixes = detectCommonPrefixes(rule);

            // Check for optional/star ambiguities
            boolean hasOptionalConflicts = detectOptionalConflicts(rule);

            if (hasLeftRecursion || hasCommonPrefixes || hasOptionalConflicts) {
                List<Integer> conflictingAlts = new ArrayList<>();
                for (int i = 1; i <= rule.numberOfAlts; i++) {
                    conflictingAlts.add(i);
                }

                String explanation = buildExplanation(rule, hasLeftRecursion, hasCommonPrefixes, hasOptionalConflicts);
                String suggestedFix = buildSuggestedFix(hasLeftRecursion, hasCommonPrefixes, hasOptionalConflicts);

                Ambiguity ambiguity = Ambiguity.builder()
                    .ruleName(rule.name)
                    .line(rule.ast != null ? rule.ast.getLine() : null)
                    .column(rule.ast != null ? rule.ast.getCharPositionInLine() : null)
                    .conflictingAlternatives(conflictingAlts)
                    .explanation(explanation)
                    .suggestedFix(suggestedFix)
                    .build();

                ambiguities.add(ambiguity);
                log.debug("Detected potential ambiguity in rule '{}'", rule.name);
            }

        } catch (Exception e) {
            log.warn("Failed to analyze rule '{}': {}", rule.name, e.getMessage());
        }
    }

    /**
     * Detect if rule has left-recursion
     */
    private boolean detectLeftRecursion(Rule rule) {
        if (rule.ast == null || rule.ast.getChildren() == null) {
            return false;
        }

        // Check if any alternative starts with a reference to the rule itself
        for (int i = 1; i <= rule.numberOfAlts; i++) {
            AltAST alt = rule.alt[i].ast;
            if (alt != null && alt.getChildCount() > 0) {
                GrammarAST firstElement = (GrammarAST) alt.getChild(0);
                if (firstElement != null) {
                    String text = firstElement.getText();
                    if (rule.name.equals(text)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Detect alternatives with common prefixes
     */
    private boolean detectCommonPrefixes(Rule rule) {
        if (rule.numberOfAlts < 2) {
            return false;
        }

        // Collect first tokens/rules from each alternative
        Set<String> firstElements = new HashSet<>();
        int matchCount = 0;

        for (int i = 1; i <= rule.numberOfAlts; i++) {
            AltAST alt = rule.alt[i].ast;
            if (alt != null && alt.getChildCount() > 0) {
                GrammarAST firstElement = (GrammarAST) alt.getChild(0);
                if (firstElement != null) {
                    String text = firstElement.getText();
                    if (!firstElements.add(text)) {
                        // Found duplicate first element
                        matchCount++;
                    }
                }
            }
        }

        return matchCount > 0;
    }

    /**
     * Detect optional/star conflicts
     */
    private boolean detectOptionalConflicts(Rule rule) {
        if (rule.ast == null) {
            return false;
        }

        String ruleText = rule.ast.toStringTree();

        // Look for patterns like (X)? followed by X, or X* followed by X
        boolean hasOptional = ruleText.contains("?") || ruleText.contains("*");
        boolean hasMultipleRefs = Collections.frequency(
            Arrays.asList(ruleText.split("\\s+")),
            rule.name
        ) > 1;

        return hasOptional && hasMultipleRefs;
    }

    /**
     * Build human-readable explanation
     */
    private String buildExplanation(Rule rule, boolean hasLeftRecursion,
                                   boolean hasCommonPrefixes, boolean hasOptionalConflicts) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Rule '").append(rule.name).append("' has ");

        List<String> issues = new ArrayList<>();
        if (hasLeftRecursion) {
            issues.add("left-recursion with multiple alternatives");
        }
        if (hasCommonPrefixes) {
            issues.add("alternatives with common starting elements");
        }
        if (hasOptionalConflicts) {
            issues.add("optional/star patterns that may conflict");
        }

        explanation.append(String.join(" and ", issues));
        explanation.append(". This may lead to ambiguous parsing where the parser ");
        explanation.append("cannot uniquely determine which alternative to choose.");

        return explanation.toString();
    }

    /**
     * Build suggested fix
     */
    private String buildSuggestedFix(boolean hasLeftRecursion, boolean hasCommonPrefixes,
                                    boolean hasOptionalConflicts) {
        if (hasLeftRecursion) {
            return "For left-recursive rules, ensure proper precedence levels are defined using ANTLR4's precedence syntax. "
                 + "Consider restructuring to make precedence explicit.";
        } else if (hasCommonPrefixes) {
            return "Use left-factoring to extract common prefixes into a separate rule or sub-expression. "
                 + "Consider reordering alternatives to prioritize more specific matches first.";
        } else if (hasOptionalConflicts) {
            return "Review optional (?) and star (*) patterns to ensure they don't create ambiguous matches. "
                 + "Consider using explicit alternatives or semantic predicates to disambiguate.";
        } else {
            return "Analyze the alternatives and consider adding semantic predicates or "
                 + "restructuring the grammar to make choices more distinct.";
        }
    }
}
