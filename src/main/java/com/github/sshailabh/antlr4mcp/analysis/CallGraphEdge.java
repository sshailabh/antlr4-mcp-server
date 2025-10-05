package com.github.sshailabh.antlr4mcp.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an edge in the grammar call graph
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallGraphEdge {
    private String from;
    private String to;
    private int invocationCount; // Number of times 'from' calls 'to'
}
