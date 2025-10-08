package com.github.sshailabh.antlr4mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of creating an interpreter from a grammar.
 * Contains the Grammar object and metadata about actions/predicates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterpreterResult {
    /**
     * The loaded Grammar object
     */
    private Grammar grammar;

    /**
     * Lexer grammar (if separate or implicit)
     */
    private LexerGrammar lexerGrammar;

    /**
     * Whether the grammar contains actions {...}
     */
    @Builder.Default
    private boolean hasActions = false;

    /**
     * Whether the grammar contains semantic predicates {...}?
     */
    @Builder.Default
    private boolean hasPredicates = false;

    /**
     * Warnings about the grammar (e.g., actions will be ignored)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Grammar name
     */
    private String grammarName;

    /**
     * Grammar type (lexer, parser, combined)
     */
    private String grammarType;

    /**
     * Create successful result
     */
    public static InterpreterResult success(
            Grammar grammar,
            String grammarName,
            String grammarType
    ) {
        return InterpreterResult.builder()
                .grammar(grammar)
                .grammarName(grammarName)
                .grammarType(grammarType)
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * Add a warning to this result
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
}
