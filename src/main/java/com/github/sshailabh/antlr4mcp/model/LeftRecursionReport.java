package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Report of left recursion analysis in an ANTLR grammar.
 * 
 * Left recursion is a common pattern in grammar design where a rule
 * references itself (directly or indirectly) as its leftmost symbol.
 * ANTLR4 automatically transforms left-recursive rules but understanding
 * them helps optimize grammar performance and debug parsing issues.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeftRecursionReport {
    
    private boolean success;
    private String error;
    private boolean hasLeftRecursion;
    
    /** List of left-recursive rules with details */
    private List<RecursiveRule> recursiveRules;
    
    /** Detected recursion cycles */
    private List<RecursionCycle> cycles;
    
    /** Statistics */
    private int totalRules;
    private int leftRecursiveRuleCount;
    private int directLeftRecursionCount;
    private int indirectLeftRecursionCount;

    /**
     * Details about a left-recursive rule.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecursiveRule {
        private String ruleName;
        
        /** True if direct left recursion (A -> A α) */
        private boolean isDirect;
        
        /** Primary (non-recursive) alternative indices */
        private List<Integer> primaryAlts;
        
        /** Recursive operator alternative indices */
        private List<Integer> recursiveAlts;
        
        /** Original number of alternatives before transformation */
        private int originalAlternatives;
    }

    /**
     * A cycle of mutually recursive rules.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecursionCycle {
        /** Rules involved in the cycle */
        private List<String> rules;
        
        /** True if it's a direct cycle (single rule) */
        private boolean isDirect;
    }

    public static LeftRecursionReport error(String message) {
        return LeftRecursionReport.builder()
            .success(false)
            .error(message)
            .hasLeftRecursion(false)
            .recursiveRules(Collections.emptyList())
            .cycles(Collections.emptyList())
            .build();
    }
}

