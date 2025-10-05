package com.github.sshailabh.antlr4mcp.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the grammar call graph
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallGraphNode {
    private String ruleName;
    private RuleType type; // PARSER, LEXER, FRAGMENT

    @Builder.Default
    private List<String> calls = new ArrayList<>();

    @Builder.Default
    private List<String> calledBy = new ArrayList<>();

    private boolean recursive;
    private int depth;
    private boolean unused;

    public enum RuleType {
        PARSER,
        LEXER,
        FRAGMENT
    }
}
