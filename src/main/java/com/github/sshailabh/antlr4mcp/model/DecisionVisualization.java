package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Visualization of all decision points in an ANTLR grammar rule.
 * Shows where the parser makes choices between alternatives.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecisionVisualization {
    /**
     * Name of the rule being visualized
     */
    private String ruleName;

    /**
     * Total number of decision points in this rule
     */
    private int totalDecisions;

    /**
     * Individual decision points
     */
    @Builder.Default
    private List<DecisionPoint> decisions = new ArrayList<>();
}
