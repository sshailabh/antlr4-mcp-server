package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.DecisionPoint;
import com.github.sshailabh.antlr4mcp.model.DecisionVisualization;
import com.github.sshailabh.antlr4mcp.util.GrammarLoader;
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
 * Visualizes decision points in ANTLR4 grammar rules.
 * Decision points are locations where the parser must choose between multiple alternatives.
 */
@Slf4j
@Service
public class DecisionVisualizer {

    /**
     * Visualize decision points in a grammar rule.
     *
     * @param grammarText The complete ANTLR4 grammar text
     * @param ruleName    Name of the rule to visualize
     * @return DecisionVisualization containing all decision points
     */
    public DecisionVisualization visualize(String grammarText, String ruleName) {
        log.info("Visualizing decision points for rule: {} (grammar size: {} bytes)",
                ruleName, grammarText != null ? grammarText.length() : 0);

        if (grammarText == null || grammarText.trim().isEmpty()) {
            throw new IllegalArgumentException("Grammar text cannot be null or empty");
        }

        if (ruleName == null || ruleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name cannot be null or empty");
        }

        try {
            // Load grammar
            Grammar grammar = GrammarLoader.loadGrammar(grammarText);
            if (grammar == null) {
                throw new IllegalStateException("Failed to load grammar");
            }

            // Get rule
            Rule rule = grammar.getRule(ruleName);
            if (rule == null) {
                throw new IllegalArgumentException("Rule not found: " + ruleName);
            }

            // Find decision points
            List<DecisionPoint> decisions = findDecisionPoints(rule, grammar);

            DecisionVisualization visualization = DecisionVisualization.builder()
                    .ruleName(ruleName)
                    .totalDecisions(decisions.size())
                    .decisions(decisions)
                    .build();

            log.info("Found {} decision points in rule '{}'", decisions.size(), ruleName);
            return visualization;

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for decision visualization", e);
            throw e;
        } catch (Exception e) {
            log.error("Decision visualization failed", e);
            throw new RuntimeException("Decision visualization error: " + e.getMessage(), e);
        }
    }

    /**
     * Find all decision points in a rule by walking its ATN.
     */
    private List<DecisionPoint> findDecisionPoints(Rule rule, Grammar grammar) {
        List<DecisionPoint> decisions = new ArrayList<>();

        // Use ANTLR's built-in decision-to-state mapping
        // This is more reliable than BFS traversal
        if (grammar.atn.decisionToState != null) {
            for (int i = 0; i < grammar.atn.decisionToState.size(); i++) {
                DecisionState ds = grammar.atn.decisionToState.get(i);

                // Check if this decision belongs to our rule
                if (ds != null && ds.ruleIndex == rule.index) {
                    log.debug("Found decision {} in rule '{}': state {} ({})",
                             ds.decision, rule.name, ds.stateNumber, ds.getClass().getSimpleName());

                    DecisionPoint dp = createDecisionPoint(ds, rule, grammar);
                    decisions.add(dp);
                }
            }
        }

        log.info("Found {} decision points in rule '{}' using decisionToState", decisions.size(), rule.name);
        return decisions;
    }

    /**
     * Create DecisionPoint for a DecisionState.
     */
    private DecisionPoint createDecisionPoint(DecisionState ds, Rule rule, Grammar grammar) {
        // Count alternatives (number of outgoing transitions)
        int alternativeCount = ds.getNumberOfTransitions();

        // Generate DOT visualization for this decision
        String dotFormat = generateDecisionDot(ds, rule, grammar);

        // Count states and transitions in decision subgraph
        Map<String, Integer> counts = countDecisionGraph(ds, rule.index);

        return DecisionPoint.builder()
                .ruleName(rule.name)
                .decisionNumber(ds.decision)
                .stateNumber(ds.stateNumber)
                .alternativeCount(alternativeCount)
                .dotFormat(dotFormat)
                .stateCount(counts.get("states"))
                .transitionCount(counts.get("transitions"))
                .build();
    }

    /**
     * Generate DOT format for a single decision point.
     */
    private String generateDecisionDot(DecisionState ds, Rule rule, Grammar grammar) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph Decision_").append(ds.decision).append(" {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box, style=rounded];\n\n");

        // Decision state itself
        dot.append("  s").append(ds.stateNumber)
           .append(" [label=\"Decision ").append(ds.decision)
           .append("\", shape=diamond, style=filled, fillcolor=yellow];\n\n");

        // Show alternatives
        Set<Integer> visited = new HashSet<>();
        Queue<ATNState> queue = new LinkedList<>();
        queue.add(ds);

        while (!queue.isEmpty()) {
            ATNState state = queue.poll();

            if (visited.contains(state.stateNumber)) {
                continue;
            }
            visited.add(state.stateNumber);

            // Add transitions (only show a few levels to keep it manageable)
            for (int i = 0; i < state.getNumberOfTransitions(); i++) {
                Transition transition = state.transition(i);
                ATNState target = transition.target;

                String label = getTransitionLabel(transition, grammar);

                dot.append("  s").append(state.stateNumber)
                   .append(" -> s").append(target.stateNumber)
                   .append(" [label=\"").append(label).append("\"];\n");

                // Only explore a few levels deep
                if (visited.size() < 20 && target.ruleIndex == rule.index) {
                    queue.add(target);
                }
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Get transition label for visualization.
     */
    private String getTransitionLabel(Transition transition, Grammar grammar) {
        if (transition instanceof AtomTransition) {
            AtomTransition at = (AtomTransition) transition;
            return grammar.getTokenDisplayName(at.label);
        } else if (transition instanceof SetTransition) {
            SetTransition st = (SetTransition) transition;
            return st.label().toString();
        } else if (transition instanceof RuleTransition) {
            RuleTransition rt = (RuleTransition) transition;
            Rule targetRule = grammar.getRule(rt.ruleIndex);
            return targetRule != null ? targetRule.name : "rule" + rt.ruleIndex;
        } else if (transition instanceof EpsilonTransition) {
            return "ε";
        } else if (transition instanceof PredicateTransition) {
            return "pred";
        } else {
            return transition.getClass().getSimpleName();
        }
    }

    /**
     * Count states and transitions in decision subgraph.
     */
    private Map<String, Integer> countDecisionGraph(DecisionState ds, int ruleIndex) {
        Set<Integer> visitedStates = new HashSet<>();
        int transitionCount = 0;

        Queue<ATNState> queue = new LinkedList<>();
        queue.add(ds);

        while (!queue.isEmpty()) {
            ATNState state = queue.poll();

            if (visitedStates.contains(state.stateNumber)) {
                continue;
            }
            visitedStates.add(state.stateNumber);

            for (int i = 0; i < state.getNumberOfTransitions(); i++) {
                transitionCount++;
                Transition transition = state.transition(i);
                ATNState target = transition.target;

                if (!visitedStates.contains(target.stateNumber) &&
                    target.ruleIndex == ruleIndex &&
                    visitedStates.size() < 20) {
                    queue.add(target);
                }
            }
        }

        Map<String, Integer> counts = new HashMap<>();
        counts.put("states", visitedStates.size());
        counts.put("transitions", transitionCount);
        return counts;
    }




}
