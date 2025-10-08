package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Results of left-recursion analysis on an ANTLR4 grammar.
 * Identifies rules that use left-recursion and provides details
 * about ANTLR's automatic transformation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeftRecursionAnalysis {
    /**
     * Total number of parser rules analyzed
     */
    private int totalRules;

    /**
     * Number of left-recursive rules found
     */
    private int leftRecursiveCount;

    /**
     * Number of rules that ANTLR has transformed
     */
    private int transformedCount;

    /**
     * Detailed information about each left-recursive rule
     */
    @Builder.Default
    private List<LeftRecursiveRule> leftRecursiveRules = new ArrayList<>();

    /**
     * Names of rules that have been transformed by ANTLR
     */
    @Builder.Default
    private List<String> transformedRules = new ArrayList<>();
}
