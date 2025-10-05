package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of grammar profiling operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResult {
    private boolean success;
    private List<DecisionStats> decisionStats;
    private ParserStats parserStats;
    private List<AmbiguityInfo> ambiguities;
    private List<GrammarError> errors;
    private String error;  // Single error message for failures
    private long parsingTimeMs;  // Total parsing time

    /**
     * Statistics for a single decision point
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DecisionStats {
        private int decisionNumber;
        private String ruleName;
        private long invocations;
        private long timeInPrediction;
        private long llFallbacks;
        private long fullContextFallbacks;
        private long ambiguities;
        private long maxLook;
        private double avgLook;
        private long totalLook;
        private long minLook;
        private long maxAlt;
        private long minAlt;
        private List<Integer> conflictingAlts;
    }

    /**
     * Overall parser statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParserStats {
        private long totalDecisions;
        private long totalInvocations;
        private long totalTimeInPrediction;
        private long totalAmbiguities;
        private long totalLlFallbacks;
        private long totalFullContextFallbacks;
        private long parseTimeMs;
        private String inputSize;
    }

    /**
     * Information about detected ambiguity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AmbiguityInfo {
        private int decision;
        private String ruleName;
        private int startIndex;
        private int stopIndex;
        private List<Integer> ambigAlts;
        private String ambigText;
        private boolean fullContext;
    }
}
