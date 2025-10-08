package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of generated test inputs for a grammar rule.
 * Provides sample inputs that exercise different paths through the grammar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratedTestInputs {
    /**
     * Name of the rule for which inputs were generated
     */
    private String ruleName;

    /**
     * Total number of test inputs generated
     */
    private int totalInputs;

    /**
     * The generated test inputs
     */
    @Builder.Default
    private List<TestInput> inputs = new ArrayList<>();

    /**
     * Number of alternatives covered
     */
    private Integer alternativesCovered;

    /**
     * Maximum recursion depth used in generation
     */
    @Builder.Default
    private int maxDepth = 0;

    /**
     * Whether generation was limited by constraints (max inputs, max depth, etc.)
     */
    @Builder.Default
    private boolean limited = false;

    /**
     * Any warnings or messages about the generation process
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
