package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a left-recursive rule in an ANTLR4 grammar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeftRecursiveRule {
    /**
     * Name of the left-recursive rule
     */
    private String ruleName;

    /**
     * Whether this is direct left-recursion (rule calls itself directly)
     */
    private boolean isDirect;

    /**
     * Whether ANTLR has automatically transformed this rule
     * (indicated by presence of precpred predicates)
     */
    private boolean isTransformed;

    /**
     * Precedence levels found in the transformed rule
     * (extracted from {precpred(_ctx, N)}? predicates)
     */
    @Builder.Default
    private List<Integer> precedenceLevels = new ArrayList<>();

    /**
     * Original rule text (if available)
     */
    private String originalForm;

    /**
     * Number of alternatives in the rule
     */
    private int alternatives;

    /**
     * Line number where the rule is defined
     */
    private Integer line;
}
