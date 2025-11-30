package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.FirstFollowReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.LL1Analyzer;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for computing FIRST and FOLLOW sets in ANTLR grammars.
 * 
 * FIRST and FOLLOW sets are fundamental concepts in parsing theory:
 * - FIRST(A): Set of tokens that can begin strings derived from A
 * - FOLLOW(A): Set of tokens that can appear immediately after A
 * 
 * These sets are crucial for:
 * - Understanding grammar structure
 * - Debugging ambiguities
 * - Optimizing parser performance
 * - Building LL(1) parse tables
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FirstFollowAnalyzer {

    private final GrammarCompiler grammarCompiler;

    /**
     * Compute FIRST and FOLLOW sets for all rules in the grammar.
     */
    public FirstFollowReport analyze(String grammarText) {
        return analyze(grammarText, null);
    }

    /**
     * Compute FIRST and FOLLOW sets for a specific rule, or all rules if ruleName is null.
     */
    public FirstFollowReport analyze(String grammarText, String ruleName) {
        log.info("Computing FIRST/FOLLOW sets" + (ruleName != null ? " for rule: " + ruleName : ""));

        try {
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return FirstFollowReport.error("Failed to load grammar");
            }

            if (grammar.atn == null) {
                return FirstFollowReport.error("Grammar ATN not available");
            }

            ATN atn = grammar.atn;
            LL1Analyzer analyzer = new LL1Analyzer(atn);

            List<FirstFollowReport.RuleAnalysis> ruleAnalyses = new ArrayList<>();
            List<FirstFollowReport.DecisionAnalysis> decisionAnalyses = new ArrayList<>();

            // Analyze rules
            for (Rule rule : grammar.rules.values()) {
                if (ruleName != null && !rule.name.equals(ruleName)) {
                    continue;
                }
                
                // Skip lexer rules (uppercase names)
                if (Character.isUpperCase(rule.name.charAt(0))) {
                    continue;
                }

                RuleStartState startState = atn.ruleToStartState[rule.index];
                RuleStopState stopState = atn.ruleToStopState[rule.index];

                // Compute FIRST set
                IntervalSet firstSet = analyzer.LOOK(startState, null, null);
                List<String> firstTokens = intervalSetToTokenNames(firstSet, grammar);

                // Compute FOLLOW set (tokens that can follow this rule)
                IntervalSet followSet = analyzer.LOOK(stopState, null, null);
                List<String> followTokens = intervalSetToTokenNames(followSet, grammar);

                // Check for nullable
                boolean nullable = firstSet.contains(Token.EPSILON);

                // Check for potential LL(1) conflicts
                boolean hasConflict = hasLL1Conflict(startState, analyzer, grammar);

                ruleAnalyses.add(FirstFollowReport.RuleAnalysis.builder()
                    .ruleName(rule.name)
                    .firstSet(firstTokens)
                    .followSet(followTokens)
                    .nullable(nullable)
                    .hasLL1Conflict(hasConflict)
                    .alternativeCount(rule.numberOfAlts)
                    .build());
            }

            // Analyze decisions (for more detailed lookahead info)
            int numberOfDecisions = atn.getNumberOfDecisions();
            for (int i = 0; i < numberOfDecisions; i++) {
                DecisionState decisionState = atn.getDecisionState(i);
                if (decisionState == null) continue;

                Rule rule = grammar.getRule(decisionState.ruleIndex);
                if (ruleName != null && !rule.name.equals(ruleName)) {
                    continue;
                }

                IntervalSet[] lookaheadSets = analyzer.getDecisionLookahead(decisionState);
                if (lookaheadSets == null) continue;

                List<FirstFollowReport.AlternativeLookahead> altLookaheads = new ArrayList<>();
                for (int alt = 0; alt < lookaheadSets.length; alt++) {
                    IntervalSet lookahead = lookaheadSets[alt];
                    if (lookahead == null) {
                        altLookaheads.add(FirstFollowReport.AlternativeLookahead.builder()
                            .alternative(alt + 1)
                            .lookaheadTokens(Collections.singletonList("<predicate>"))
                            .hasPredicate(true)
                            .build());
                    } else {
                        altLookaheads.add(FirstFollowReport.AlternativeLookahead.builder()
                            .alternative(alt + 1)
                            .lookaheadTokens(intervalSetToTokenNames(lookahead, grammar))
                            .hasPredicate(false)
                            .build());
                    }
                }

                // Check for ambiguous lookahead
                boolean isAmbiguous = hasAmbiguousLookahead(lookaheadSets);

                decisionAnalyses.add(FirstFollowReport.DecisionAnalysis.builder()
                    .decisionNumber(i)
                    .ruleName(rule.name)
                    .stateNumber(decisionState.stateNumber)
                    .alternativeCount(lookaheadSets.length)
                    .alternatives(altLookaheads)
                    .hasAmbiguousLookahead(isAmbiguous)
                    .build());
            }

            // Compute statistics
            int totalRules = ruleAnalyses.size();
            int nullableCount = (int) ruleAnalyses.stream().filter(r -> r.isNullable()).count();
            int conflictCount = (int) ruleAnalyses.stream().filter(r -> r.isHasLL1Conflict()).count();
            int ambiguousDecisionCount = (int) decisionAnalyses.stream()
                .filter(d -> d.isHasAmbiguousLookahead()).count();

            return FirstFollowReport.builder()
                .success(true)
                .rules(ruleAnalyses)
                .decisions(decisionAnalyses)
                .totalParserRules(totalRules)
                .nullableRuleCount(nullableCount)
                .rulesWithConflicts(conflictCount)
                .totalDecisions(decisionAnalyses.size())
                .ambiguousDecisions(ambiguousDecisionCount)
                .build();

        } catch (Exception e) {
            log.error("Error computing FIRST/FOLLOW sets", e);
            return FirstFollowReport.error("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * Convert an IntervalSet to a list of human-readable token names.
     */
    private List<String> intervalSetToTokenNames(IntervalSet set, Grammar grammar) {
        List<String> tokens = new ArrayList<>();
        if (set == null) return tokens;

        for (int i = set.getMinElement(); i <= set.getMaxElement(); i++) {
            if (set.contains(i)) {
                if (i == Token.EOF) {
                    tokens.add("EOF");
                } else if (i == Token.EPSILON) {
                    tokens.add("ε");
                } else {
                    String name = grammar.getTokenDisplayName(i);
                    tokens.add(name != null ? name : String.valueOf(i));
                }
            }
        }
        return tokens;
    }

    /**
     * Check if a rule has LL(1) conflicts (overlapping FIRST sets in alternatives).
     */
    private boolean hasLL1Conflict(RuleStartState startState, LL1Analyzer analyzer, Grammar grammar) {
        ATNState firstDecision = startState.transition(0).target;
        if (!(firstDecision instanceof DecisionState)) {
            return false;
        }

        IntervalSet[] lookaheadSets = analyzer.getDecisionLookahead(firstDecision);
        return hasAmbiguousLookahead(lookaheadSets);
    }

    /**
     * Check if lookahead sets have overlapping tokens.
     */
    private boolean hasAmbiguousLookahead(IntervalSet[] lookaheadSets) {
        if (lookaheadSets == null || lookaheadSets.length < 2) {
            return false;
        }

        for (int i = 0; i < lookaheadSets.length; i++) {
            if (lookaheadSets[i] == null) continue;
            
            for (int j = i + 1; j < lookaheadSets.length; j++) {
                if (lookaheadSets[j] == null) continue;
                
                IntervalSet intersection = lookaheadSets[i].and(lookaheadSets[j]);
                if (!intersection.isNil()) {
                    return true;
                }
            }
        }
        return false;
    }
}

