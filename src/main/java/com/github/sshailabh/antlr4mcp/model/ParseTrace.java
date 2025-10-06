package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parse trace with step-by-step events during parsing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseTrace {
    private boolean success;

    @Builder.Default
    private List<TraceEvent> events = new ArrayList<>();

    @Builder.Default
    private List<GrammarError> errors = new ArrayList<>();

    private String grammarName;
    private String startRule;
    private String input;
    private Integer totalSteps;
}