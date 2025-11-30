package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ambiguity {
    private Integer line;
    private Integer column;
    private String ruleName;
    private List<Integer> conflictingAlternatives;
    private String explanation;
    private String suggestedFix;
    private String sampleInput;
}
