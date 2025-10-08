package com.github.sshailabh.antlr4mcp.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Lightweight detector for embedded code in ANTLR grammars.
 * Provides fast boolean checks for semantic predicates and actions.
 *
 * Design rationale:
 * - Optimized for the common case: checking if code exists (not analyzing it)
 * - Uses compiled regex patterns for performance
 * - No allocations in hot path (no collections, no objects)
 * - Sufficient for interpreter mode warnings
 */
@Component
@Slf4j
public class EmbeddedCodeAnalyzer {

    // Compiled patterns for fast matching
    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "@(header|members|init|after)\\s*\\{[^}]+\\}",
        Pattern.DOTALL
    );

    private static final Pattern SEMANTIC_PREDICATE_PATTERN = Pattern.compile(
        "\\{[^}]+\\}\\?",
        Pattern.DOTALL
    );

    private static final Pattern INLINE_ACTION_PATTERN = Pattern.compile(
        "\\{[^}]+?\\}",
        Pattern.DOTALL
    );

    /**
     * Check if grammar has semantic predicates.
     * Semantic predicates are runtime conditions: {expression}?
     *
     * @param grammarText Grammar content to check
     * @return true if predicates are present
     */
    public boolean hasPredicates(String grammarText) {
        return SEMANTIC_PREDICATE_PATTERN.matcher(grammarText).find();
    }

    /**
     * Check if grammar has actions (including @actions and inline actions).
     * Actions are embedded code blocks that modify parser state.
     * This method explicitly excludes semantic predicates (which end with '?').
     *
     * @param grammarText Grammar content to check
     * @return true if actions are present
     */
    public boolean hasActions(String grammarText) {
        // Check for @actions first (these are always actions, never predicates)
        if (ACTION_PATTERN.matcher(grammarText).find()) {
            return true;
        }

        // For inline actions, we need to exclude predicates
        // Predicates are: { code }?
        // Actions are: { code } (without '?')
        // We need to check that there's a { } block that's NOT followed by '?'

        // Remove all predicates first, then check if any actions remain
        String withoutPredicates = SEMANTIC_PREDICATE_PATTERN.matcher(grammarText).replaceAll("");
        return INLINE_ACTION_PATTERN.matcher(withoutPredicates).find();
    }

    /**
     * Check if grammar has any embedded code (predicates or actions).
     *
     * @param grammarText Grammar content to check
     * @return true if any embedded code is present
     */
    public boolean hasEmbeddedCode(String grammarText) {
        return hasPredicates(grammarText) || hasActions(grammarText);
    }
}
