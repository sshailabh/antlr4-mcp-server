package com.github.sshailabh.antlr4mcp.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grammar call graph representation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallGraph {
    @Builder.Default
    private List<CallGraphNode> nodes = new ArrayList<>();

    @Builder.Default
    private List<CallGraphEdge> edges = new ArrayList<>();

    @Builder.Default
    private List<String> cycles = new ArrayList<>();

    @Builder.Default
    private Map<String, Integer> depths = new HashMap<>();

    private String startRule;
    private int totalRules;
    private int unusedRules;
}
