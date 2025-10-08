package com.github.sshailabh.antlr4mcp.visualization;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.DOTGenerator;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Visualizes ANTLR ATN (Augmented Transition Network) state machines.
 * Phase 1: Uses ANTLR's built-in DOTGenerator for simpler and more reliable visualization.
 */
@Component
@Slf4j
public class AtnVisualizer {

    /**
     * Generate ATN visualization for a specific rule using ANTLR's built-in DOTGenerator.
     * Phase 1: Simplified to use ANTLR's native DOT generation.
     */
    public AtnVisualization visualize(Grammar grammar, String ruleName) {
        log.info("Generating ATN visualization for rule: {} using DOTGenerator", ruleName);

        // Get the rule
        Rule rule = grammar.getRule(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleName);
        }

        ATNState startState = grammar.atn.ruleToStartState[rule.index];

        // Build graph for state/transition counts
        AtnGraph graph = buildGraph(startState, new HashSet<>());

        // Use ANTLR's built-in DOTGenerator
        DOTGenerator dotGen = new DOTGenerator(grammar);
        String dot = dotGen.getDOT(startState, true);  // true = show rule names

        // Phase 1: Generate only DOT format (mermaid and SVG deferred to later phases)
        String mermaid = null;
        String svg = null;

        return AtnVisualization.builder()
            .ruleName(ruleName)
            .stateCount(graph.getStateCount())
            .transitionCount(graph.getTransitionCount())
            .dotFormat(dot)
            .mermaidFormat(mermaid)
            .svgFormat(svg)
            .graph(graph)
            .build();
    }

    /**
     * Build ATN graph by traversing states
     */
    private AtnGraph buildGraph(ATNState startState, Set<Integer> visited) {
        AtnGraph graph = new AtnGraph();
        Queue<ATNState> queue = new LinkedList<>();
        queue.add(startState);

        while (!queue.isEmpty()) {
            ATNState state = queue.poll();

            if (visited.contains(state.stateNumber)) {
                continue;
            }
            visited.add(state.stateNumber);

            // Add state node
            AtnNode node = createNode(state);
            graph.addState(node);

            // Process transitions
            for (int i = 0; i < state.getNumberOfTransitions(); i++) {
                Transition transition = state.transition(i);
                ATNState target = transition.target;

                AtnEdge edge = AtnEdge.builder()
                    .from(state.stateNumber)
                    .to(target.stateNumber)
                    .label(getTransitionLabel(transition))
                    .type(getTransitionType(transition))
                    .build();

                graph.addTransition(edge);

                if (!visited.contains(target.stateNumber)) {
                    queue.add(target);
                }
            }
        }

        return graph;
    }

    /**
     * Create ATN node from state
     */
    private AtnNode createNode(ATNState state) {
        return AtnNode.builder()
            .stateNumber(state.stateNumber)
            .stateType(state.getStateType())
            .ruleIndex(state.ruleIndex)
            .isAcceptState(state instanceof RuleStopState)
            .label(getStateLabel(state))
            .build();
    }

    /**
     * Get human-readable label for state
     */
    private String getStateLabel(ATNState state) {
        if (state instanceof RuleStartState) {
            return "Start";
        } else if (state instanceof RuleStopState) {
            return "Stop";
        } else if (state instanceof BasicState) {
            return "Basic";
        } else if (state instanceof PlusBlockStartState) {
            return "+Block";
        } else if (state instanceof StarBlockStartState) {
            return "*Block";
        } else if (state instanceof PlusLoopbackState) {
            return "+Loop";
        } else if (state instanceof StarLoopbackState) {
            return "*Loop";
        }
        return "State";
    }

    /**
     * Get transition label
     */
    private String getTransitionLabel(Transition transition) {
        if (transition instanceof AtomTransition) {
            AtomTransition at = (AtomTransition) transition;
            int label = at.label;
            char c = (char) label;
            return Character.isLetterOrDigit(c) ? String.valueOf(c) : String.format("0x%02X", label);
        } else if (transition instanceof SetTransition) {
            SetTransition st = (SetTransition) transition;
            return st.set.toString();
        } else if (transition instanceof RuleTransition) {
            RuleTransition rt = (RuleTransition) transition;
            return "→rule[" + rt.ruleIndex + "]";
        } else if (transition instanceof EpsilonTransition) {
            return "ε";
        } else if (transition instanceof RangeTransition) {
            RangeTransition rt = (RangeTransition) transition;
            return "[" + (char)rt.from + "-" + (char)rt.to + "]";
        } else if (transition instanceof NotSetTransition) {
            return "~set";
        } else if (transition instanceof WildcardTransition) {
            return ".";
        } else if (transition instanceof PredicateTransition) {
            return "{pred}?";
        } else if (transition instanceof ActionTransition) {
            return "{action}";
        }
        return transition.getClass().getSimpleName();
    }

    /**
     * Get transition type
     */
    private String getTransitionType(Transition transition) {
        if (transition instanceof EpsilonTransition) {
            return "epsilon";
        } else if (transition instanceof AtomTransition) {
            return "atom";
        } else if (transition instanceof SetTransition) {
            return "set";
        } else if (transition instanceof RuleTransition) {
            return "rule";
        } else if (transition instanceof RangeTransition) {
            return "range";
        } else if (transition instanceof PredicateTransition) {
            return "predicate";
        } else if (transition instanceof ActionTransition) {
            return "action";
        }
        return "other";
    }
}
