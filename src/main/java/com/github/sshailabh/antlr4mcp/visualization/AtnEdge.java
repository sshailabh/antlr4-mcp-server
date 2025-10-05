package com.github.sshailabh.antlr4mcp.visualization;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a transition edge in the ATN
 */
@Data
@Builder
public class AtnEdge {
    private int from;
    private int to;
    private String label;
    private String type; // epsilon, atom, set, rule, etc.
}
