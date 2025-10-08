package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Coverage information for grammar testing (Phase 2).
 * Tracks which rules and alternatives were exercised during parsing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoverageInfo {

    /**
     * Set of rule names that were visited during parsing
     */
    @Builder.Default
    private Set<String> visitedRules = new HashSet<>();

    /**
     * Map of rule name to set of alternative numbers that were taken
     */
    @Builder.Default
    private Map<String, Set<Integer>> visitedAlternatives = new HashMap<>();

    /**
     * Total number of unique rules visited
     */
    @JsonIgnore
    public int getRuleCoverageCount() {
        return visitedRules != null ? visitedRules.size() : 0;
    }

    /**
     * Total number of alternatives visited across all rules
     */
    @JsonIgnore
    public int getAlternativeCoverageCount() {
        if (visitedAlternatives == null) {
            return 0;
        }
        return visitedAlternatives.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Add a visited rule
     */
    public void addVisitedRule(String ruleName) {
        if (visitedRules == null) {
            visitedRules = new HashSet<>();
        }
        visitedRules.add(ruleName);
    }

    /**
     * Add a visited alternative
     */
    public void addVisitedAlternative(String ruleName, int alternativeNumber) {
        if (visitedAlternatives == null) {
            visitedAlternatives = new HashMap<>();
        }
        visitedAlternatives.computeIfAbsent(ruleName, k -> new HashSet<>())
            .add(alternativeNumber);

        // Also mark rule as visited
        addVisitedRule(ruleName);
    }

    /**
     * Check if a rule was visited
     */
    public boolean wasRuleVisited(String ruleName) {
        return visitedRules != null && visitedRules.contains(ruleName);
    }

    /**
     * Check if a specific alternative was visited
     */
    public boolean wasAlternativeVisited(String ruleName, int alternativeNumber) {
        if (visitedAlternatives == null) {
            return false;
        }
        Set<Integer> alternatives = visitedAlternatives.get(ruleName);
        return alternatives != null && alternatives.contains(alternativeNumber);
    }
}
