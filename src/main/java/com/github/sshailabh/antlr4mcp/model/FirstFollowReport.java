package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Report of FIRST and FOLLOW set analysis for ANTLR grammars.
 * 
 * FIRST and FOLLOW sets are fundamental to understanding parser behavior:
 * - FIRST(α): Set of terminals that begin strings derived from α
 * - FOLLOW(A): Set of terminals that can appear immediately to the right of A
 * 
 * This analysis is essential for:
 * - Understanding grammar structure and token flow
 * - Debugging ambiguities and conflicts
 * - Optimizing parser lookahead
 * - Verifying LL(k) properties
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirstFollowReport {

    private boolean success;
    private String error;
    
    /** Analysis for each parser rule */
    private List<RuleAnalysis> rules;
    
    /** Analysis for each decision point */
    private List<DecisionAnalysis> decisions;
    
    /** Statistics */
    private int totalParserRules;
    private int nullableRuleCount;
    private int rulesWithConflicts;
    private int totalDecisions;
    private int ambiguousDecisions;

    /**
     * FIRST and FOLLOW set analysis for a single rule.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RuleAnalysis {
        private String ruleName;
        
        /** Tokens that can begin this rule (FIRST set) */
        private List<String> firstSet;
        
        /** Tokens that can follow this rule (FOLLOW set) */
        private List<String> followSet;
        
        /** True if rule can derive empty string */
        private boolean nullable;
        
        /** True if rule has LL(1) prediction conflicts */
        private boolean hasLL1Conflict;
        
        /** Number of alternatives in the rule */
        private int alternativeCount;
    }

    /**
     * Lookahead analysis for a decision point.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DecisionAnalysis {
        private int decisionNumber;
        private String ruleName;
        private int stateNumber;
        private int alternativeCount;
        
        /** Lookahead sets for each alternative */
        private List<AlternativeLookahead> alternatives;
        
        /** True if alternatives have overlapping lookahead */
        private boolean hasAmbiguousLookahead;
    }

    /**
     * Lookahead information for a single alternative.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlternativeLookahead {
        private int alternative;
        
        /** Tokens in the lookahead set for this alternative */
        private List<String> lookaheadTokens;
        
        /** True if lookahead depends on a semantic predicate */
        private boolean hasPredicate;
    }

    public static FirstFollowReport error(String message) {
        return FirstFollowReport.builder()
            .success(false)
            .error(message)
            .rules(Collections.emptyList())
            .decisions(Collections.emptyList())
            .build();
    }
}

