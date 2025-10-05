package com.github.sshailabh.antlr4mcp.visualization;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Visualizes ANTLR ATN (Augmented Transition Network) state machines
 */
@Component
@Slf4j
public class AtnVisualizer {

    /**
     * Generate ATN visualization for a specific rule
     */
    public AtnVisualization visualize(Grammar grammar, String ruleName) {
        log.info("Generating ATN visualization for rule: {}", ruleName);

        // Try to get parser rule first
        Rule rule = grammar.getRule(ruleName);
        ATNState startState = null;

        if (rule != null) {
            // Parser rule
            startState = grammar.atn.ruleToStartState[rule.index];
        } else {
            // Try lexer rule
            for (Rule r : grammar.rules.values()) {
                if (r.name.equals(ruleName)) {
                    startState = grammar.atn.ruleToStartState[r.index];
                    break;
                }
            }
        }

        if (startState == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleName);
        }

        // Build graph
        AtnGraph graph = buildGraph(startState, new HashSet<>());

        // Generate visualizations
        String dot = generateDot(graph, ruleName);
        String mermaid = generateMermaid(graph, ruleName);
        String svg = renderSvg(dot);

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

    /**
     * Generate DOT format
     */
    private String generateDot(AtnGraph graph, String ruleName) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ATN_").append(ruleName).append(" {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=circle, fontname=\"Arial\", fontsize=10];\n");
        dot.append("  edge [fontname=\"Arial\", fontsize=9];\n\n");

        // States
        for (AtnNode node : graph.getStates()) {
            String shape = node.isAcceptState() ? "doublecircle" : "circle";
            String color = node.getStateNumber() == graph.getStartState().getStateNumber() ?
                "fillcolor=lightgreen, style=filled" : "";

            dot.append(String.format("  s%d [label=\"%d\\n%s\", shape=%s, %s];\n",
                node.getStateNumber(), node.getStateNumber(), node.getLabel(), shape, color));
        }

        dot.append("\n");

        // Transitions
        for (AtnEdge edge : graph.getTransitions()) {
            String color = edge.getType().equals("epsilon") ? "color=gray" :
                          edge.getType().equals("rule") ? "color=blue" : "";

            dot.append(String.format("  s%d -> s%d [label=\"%s\", %s];\n",
                edge.getFrom(), edge.getTo(),
                escapeLabel(edge.getLabel()), color));
        }

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Generate Mermaid format
     */
    private String generateMermaid(AtnGraph graph, String ruleName) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("stateDiagram-v2\n");
        mermaid.append("    [*] --> s").append(graph.getStartState().getStateNumber()).append("\n");

        for (AtnEdge edge : graph.getTransitions()) {
            String label = edge.getLabel().replace("\"", "");
            mermaid.append(String.format("    s%d --> s%d: %s\n",
                edge.getFrom(), edge.getTo(), label));
        }

        // Mark accept states
        for (AtnNode node : graph.getStates()) {
            if (node.isAcceptState()) {
                mermaid.append("    s").append(node.getStateNumber()).append(" --> [*]\n");
            }
        }

        return mermaid.toString();
    }

    /**
     * Render DOT to SVG using Graphviz (if available)
     */
    private String renderSvg(String dot) {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tsvg");
            Process process = pb.start();

            try (OutputStream os = process.getOutputStream()) {
                os.write(dot.getBytes(StandardCharsets.UTF_8));
            }

            String svg = new String(process.getInputStream().readAllBytes(),
                                   StandardCharsets.UTF_8);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Graphviz rendering failed with exit code {}", exitCode);
                return null;
            }

            return svg;
        } catch (IOException | InterruptedException e) {
            log.warn("Graphviz not available or failed to render SVG: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Escape special characters in DOT labels
     */
    private String escapeLabel(String label) {
        return label.replace("\"", "\\\"")
                    .replace("\n", "\\n");
    }
}
