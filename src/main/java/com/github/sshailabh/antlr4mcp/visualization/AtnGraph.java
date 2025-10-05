package com.github.sshailabh.antlr4mcp.visualization;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph representation of ATN (Augmented Transition Network)
 */
@Data
public class AtnGraph {
    private List<AtnNode> states = new ArrayList<>();
    private List<AtnEdge> transitions = new ArrayList<>();
    private AtnNode startState;

    public void addState(AtnNode node) {
        states.add(node);
        if (startState == null) {
            startState = node;
        }
    }

    public void addTransition(AtnEdge edge) {
        transitions.add(edge);
    }

    public int getStateCount() {
        return states.size();
    }

    public int getTransitionCount() {
        return transitions.size();
    }
}
