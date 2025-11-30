package com.github.sshailabh.antlr4mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of profiling a grammar against sample input.
 * Uses ANTLR4's ProfilingATNSimulator to gather decision statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResult {
    
    private boolean success;
    private String grammarName;
    
    // Aggregate statistics
    private long totalTimeNanos;
    private long totalSLLLookahead;
    private long totalLLLookahead;
    private long totalATNTransitions;
    private int totalDFAStates;
    
    // Per-decision details
    private List<DecisionProfile> decisions;
    
    // Summary insights
    private List<String> insights;
    private List<String> optimizationHints;
    
    // Errors if any
    private List<GrammarError> errors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionProfile {
        private int decisionNumber;
        private String ruleName;
        private long invocations;
        private long timeNanos;
        
        // SLL (Strong LL) statistics - fast path
        private long sllTotalLook;
        private long sllMinLook;
        private long sllMaxLook;
        private long sllATNTransitions;
        private long sllDFATransitions;
        
        // LL (Full) statistics - slow path fallback
        private long llFallback;
        private long llTotalLook;
        private long llMinLook;
        private long llMaxLook;
        private long llATNTransitions;
        private long llDFATransitions;
        
        // Problem indicators
        private int ambiguityCount;
        private int contextSensitivityCount;
        private int errorCount;
        
        // DFA state count
        private int dfaStates;
    }
}

