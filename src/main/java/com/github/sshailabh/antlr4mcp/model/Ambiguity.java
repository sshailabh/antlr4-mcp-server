package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ambiguity detected in grammar (Phase 1 static + Phase 2 runtime).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ambiguity {
    private Integer line;
    private Integer column;
    private String ruleName;
    private List<Integer> conflictingAlternatives;
    private String explanation;
    private String suggestedFix;
    private String sampleInput;

    // Phase 2: Additional fields for runtime profiling
    /**
     * Start index in input where ambiguity was detected
     */
    private Integer startIndex;

    /**
     * Stop index in input where ambiguity was detected
     */
    private Integer stopIndex;

    /**
     * Whether this was detected in full context (LL) or SLL mode
     */
    private Boolean isFullContext;

    /**
     * Text from input that triggered the ambiguity
     */
    private String inputText;
}
