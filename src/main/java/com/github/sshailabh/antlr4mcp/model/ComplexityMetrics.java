package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Overall complexity metrics for an ANTLR4 grammar.
 * Provides aggregate statistics and per-rule breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplexityMetrics {
    /**
     * Total number of rules in the grammar
     */
    private int totalRules;

    /**
     * Number of parser rules (lowercase-starting rules)
     */
    private int parserRules;

    /**
     * Number of lexer rules (uppercase-starting rules, excluding fragments)
     */
    private int lexerRules;

    /**
     * Number of fragment rules
     */
    private int fragmentRules;

    /**
     * Average number of alternatives per rule
     */
    private double avgAlternativesPerRule;

    /**
     * Maximum depth in the call graph (longest call chain from start rule)
     */
    private int maxRuleDepth;

    /**
     * Total number of decision points across all rules
     */
    private int totalDecisionPoints;

    /**
     * Complexity metrics for each individual rule, keyed by rule name
     */
    @Builder.Default
    private Map<String, RuleComplexity> ruleMetrics = new HashMap<>();
}
