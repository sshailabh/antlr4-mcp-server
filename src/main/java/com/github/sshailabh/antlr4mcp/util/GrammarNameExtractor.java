package com.github.sshailabh.antlr4mcp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting grammar names from ANTLR4 grammar content.
 *
 * Compiler engineering best practice:
 * - Centralized grammar name extraction ensures consistency
 * - Single regex compilation shared across uses
 * - Handles lexer, parser, and combined grammars
 */
public final class GrammarNameExtractor {

    private static final Pattern GRAMMAR_NAME_PATTERN = Pattern.compile(
        "(?:lexer\\s+grammar|parser\\s+grammar|grammar)\\s+([A-Za-z][A-Za-z0-9_]*)\\s*;",
        Pattern.MULTILINE
    );

    private GrammarNameExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Extract grammar name from grammar content.
     *
     * @param grammarContent ANTLR4 grammar text
     * @return Grammar name or null if not found
     */
    public static String extractGrammarName(String grammarContent) {
        if (grammarContent == null || grammarContent.isBlank()) {
            return null;
        }

        Matcher matcher = GRAMMAR_NAME_PATTERN.matcher(grammarContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Validate grammar name format.
     * Grammar names must start with a letter and contain only letters, digits, and underscores.
     *
     * @param name Grammar name to validate
     * @return true if valid
     */
    public static boolean isValidGrammarName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return name.matches("[A-Za-z][A-Za-z0-9_]*");
    }
}
