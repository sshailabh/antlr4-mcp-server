package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single generated test input for a grammar rule.
 * Test inputs are strings that should be valid according to the grammar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestInput {
    /**
     * The generated input string
     */
    private String input;

    /**
     * Description of what this input tests (e.g., "simple integer", "nested expression")
     */
    private String description;

    /**
     * Path description through the grammar that generated this input
     */
    private String path;

    /**
     * Alternative number (for rules with multiple alternatives)
     */
    private Integer alternative;

    /**
     * Complexity score (1=simple, 2=moderate, 3=complex)
     */
    @Builder.Default
    private int complexity = 1;
}
