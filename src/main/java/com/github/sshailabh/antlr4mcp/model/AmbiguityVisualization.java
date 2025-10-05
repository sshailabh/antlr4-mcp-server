package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of ambiguity visualization operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmbiguityVisualization {
    private boolean success;
    private String input;
    private String startRule;
    private boolean hasAmbiguities;
    private List<AmbiguityInstance> ambiguities;
    private String primaryInterpretation;  // LISP format
    private List<AlternativeInterpretation> alternatives;
    private String error;  // Single error message for failures

    /**
     * Details of a specific ambiguity instance
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AmbiguityInstance {
        private int decision;
        private String ruleName;
        private int startIndex;
        private int stopIndex;
        private String ambiguousText;
        private List<Integer> alternativeNumbers;
        private boolean fullContext;
        private String explanation;
    }

    /**
     * Alternative parse interpretation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlternativeInterpretation {
        private int alternativeNumber;
        private String parseTree;  // LISP format
        private List<String> differences;  // Descriptions of differences from primary
    }
}
