package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single decision point in an ANTLR grammar rule.
 * Decision points are where the parser must choose between multiple alternatives.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecisionPoint {
    /**
     * Rule name containing this decision
     */
    private String ruleName;

    /**
     * Decision number (unique within the grammar)
     */
    private int decisionNumber;

    /**
     * State number of the decision state in the ATN
     */
    private int stateNumber;

    /**
     * Number of alternatives at this decision point
     */
    private int alternativeCount;

    /**
     * DOT format visualization of this decision and its alternatives
     */
    private String dotFormat;

    /**
     * Number of states in the decision subgraph
     */
    private int stateCount;

    /**
     * Number of transitions in the decision subgraph
     */
    private int transitionCount;

    /**
     * Labels describing each alternative (if available)
     */
    @Builder.Default
    private List<String> alternativeLabels = new ArrayList<>();
}
