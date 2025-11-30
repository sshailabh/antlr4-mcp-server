package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.LeftRecursionReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LeftRecursiveRule;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for analyzing left recursion in ANTLR grammars.
 * 
 * Left recursion is a critical concept in parser design:
 * - Direct left recursion: A -> A α | β
 * - Indirect left recursion: A -> B α, B -> A β
 * 
 * ANTLR4 handles left recursion automatically by transforming it,
 * but understanding the patterns helps with grammar optimization.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeftRecursionAnalyzer {

    private final GrammarCompiler grammarCompiler;

    /**
     * Analyze left recursion patterns in the grammar.
     */
    public LeftRecursionReport analyze(String grammarText) {
        log.info("Analyzing left recursion in grammar");

        try {
            Grammar grammar = grammarCompiler.loadGrammar(grammarText);
            if (grammar == null) {
                return LeftRecursionReport.error("Failed to load grammar");
            }

            List<LeftRecursionReport.RecursiveRule> recursiveRules = new ArrayList<>();
            List<LeftRecursionReport.RecursionCycle> cycles = new ArrayList<>();

            // Find all left-recursive rules
            for (Rule rule : grammar.rules.values()) {
                if (rule instanceof LeftRecursiveRule) {
                    LeftRecursiveRule lrRule = (LeftRecursiveRule) rule;
                    
                    LeftRecursionReport.RecursiveRule ruleInfo = LeftRecursionReport.RecursiveRule.builder()
                        .ruleName(rule.name)
                        .isDirect(isDirectLeftRecursive(grammar, rule))
                        .primaryAlts(toList(lrRule.getPrimaryAlts()))
                        .recursiveAlts(toList(lrRule.getRecursiveOpAlts()))
                        .originalAlternatives(rule.numberOfAlts)
                        .build();
                    
                    recursiveRules.add(ruleInfo);
                }
            }

            // Detect recursion cycles using ATN traversal
            if (grammar.atn != null) {
                List<Set<String>> detectedCycles = detectRecursionCycles(grammar);
                for (Set<String> cycle : detectedCycles) {
                    cycles.add(LeftRecursionReport.RecursionCycle.builder()
                        .rules(new ArrayList<>(cycle))
                        .isDirect(cycle.size() == 1)
                        .build());
                }
            }

            // Calculate statistics
            int totalRules = grammar.rules.size();
            int leftRecursiveCount = recursiveRules.size();
            int directCount = (int) recursiveRules.stream().filter(r -> r.isDirect()).count();
            int indirectCount = leftRecursiveCount - directCount;

            return LeftRecursionReport.builder()
                .success(true)
                .hasLeftRecursion(!recursiveRules.isEmpty())
                .recursiveRules(recursiveRules)
                .cycles(cycles)
                .totalRules(totalRules)
                .leftRecursiveRuleCount(leftRecursiveCount)
                .directLeftRecursionCount(directCount)
                .indirectLeftRecursionCount(indirectCount)
                .build();

        } catch (Exception e) {
            log.error("Error analyzing left recursion", e);
            return LeftRecursionReport.error("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * Check if a rule is directly left-recursive (A -> A α)
     */
    private boolean isDirectLeftRecursive(Grammar grammar, Rule rule) {
        if (grammar.atn == null) return false;
        
        ATN atn = grammar.atn;
        RuleStartState startState = atn.ruleToStartState[rule.index];
        
        // Check if any transition from start leads directly back to the same rule
        Set<ATNState> visited = new HashSet<>();
        Queue<ATNState> queue = new LinkedList<>();
        queue.add(startState);
        
        while (!queue.isEmpty()) {
            ATNState state = queue.poll();
            if (visited.contains(state)) continue;
            visited.add(state);
            
            for (int i = 0; i < state.getNumberOfTransitions(); i++) {
                Transition t = state.transition(i);
                
                if (t instanceof RuleTransition) {
                    RuleTransition rt = (RuleTransition) t;
                    if (rt.ruleIndex == rule.index) {
                        return true; // Direct self-reference
                    }
                } else if (t.isEpsilon()) {
                    // Only follow epsilon transitions (skip optional elements)
                    queue.add(t.target);
                }
                // Stop at non-epsilon terminals - those would make it non-left-recursive
            }
        }
        
        return false;
    }

    /**
     * Detect recursion cycles in the grammar.
     */
    private List<Set<String>> detectRecursionCycles(Grammar grammar) {
        List<Set<String>> cycles = new ArrayList<>();
        ATN atn = grammar.atn;
        
        for (RuleStartState startState : atn.ruleToStartState) {
            if (startState == null) continue;
            
            Set<RuleStartState> rulesVisited = new HashSet<>();
            rulesVisited.add(startState);
            
            Set<String> cycleRules = new LinkedHashSet<>();
            if (checkForCycle(grammar, startState, rulesVisited, cycleRules, new HashSet<>())) {
                if (!cycleRules.isEmpty() && !containsCycle(cycles, cycleRules)) {
                    cycles.add(cycleRules);
                }
            }
        }
        
        return cycles;
    }

    private boolean checkForCycle(Grammar grammar, ATNState state, 
                                   Set<RuleStartState> rulesVisited,
                                   Set<String> cycleRules,
                                   Set<ATNState> statesVisited) {
        if (state instanceof RuleStopState) return false;
        if (statesVisited.contains(state)) return false;
        statesVisited.add(state);
        
        boolean foundCycle = false;
        
        for (int i = 0; i < state.getNumberOfTransitions(); i++) {
            Transition t = state.transition(i);
            
            if (t instanceof RuleTransition) {
                RuleTransition rt = (RuleTransition) t;
                RuleStartState targetStart = (RuleStartState) t.target;
                
                if (rulesVisited.contains(targetStart)) {
                    // Found a cycle
                    cycleRules.add(grammar.getRule(rt.ruleIndex).name);
                    foundCycle = true;
                } else {
                    rulesVisited.add(targetStart);
                    if (checkForCycle(grammar, t.target, rulesVisited, cycleRules, new HashSet<>())) {
                        cycleRules.add(grammar.getRule(state.ruleIndex).name);
                        foundCycle = true;
                    }
                    rulesVisited.remove(targetStart);
                }
            } else if (t.isEpsilon()) {
                if (checkForCycle(grammar, t.target, rulesVisited, cycleRules, statesVisited)) {
                    foundCycle = true;
                }
            }
        }
        
        return foundCycle;
    }

    private boolean containsCycle(List<Set<String>> cycles, Set<String> newCycle) {
        for (Set<String> existing : cycles) {
            if (existing.equals(newCycle)) return true;
        }
        return false;
    }

    private List<Integer> toList(int[] array) {
        if (array == null) return Collections.emptyList();
        List<Integer> list = new ArrayList<>();
        for (int i : array) {
            list.add(i);
        }
        return list;
    }
}

