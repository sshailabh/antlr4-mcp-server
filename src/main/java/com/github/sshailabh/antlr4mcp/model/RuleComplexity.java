package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.sshailabh.antlr4mcp.analysis.CallGraphNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complexity metrics for an individual grammar rule.
 * Provides detailed analysis of a single rule's structural complexity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleComplexity {
    /**
     * Name of the rule
     */
    private String ruleName;

    /**
     * Number of alternatives in this rule (e.g., rule: alt1 | alt2 | alt3 has 3 alternatives)
     */
    private int alternatives;

    /**
     * Number of decision points in this rule's ATN (each decision represents a branching point)
     */
    private int decisionPoints;

    /**
     * Depth of this rule in the call graph (distance from start rule)
     */
    private int depth;

    /**
     * Fan-in: number of rules that call this rule
     */
    private int fanIn;

    /**
     * Fan-out: number of rules this rule calls
     */
    private int fanOut;

    /**
     * Whether this rule is involved in a recursive cycle
     */
    private boolean recursive;

    /**
     * Type of rule: PARSER, LEXER, or FRAGMENT
     */
    private CallGraphNode.RuleType type;
}
