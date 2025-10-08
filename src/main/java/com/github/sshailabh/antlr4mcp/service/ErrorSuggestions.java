package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service that provides fix suggestions and documentation for different error types.
 * Optimized for LLM consumption with actionable recommendations.
 */
@Slf4j
@Service
public class ErrorSuggestions {

    private static final Map<ErrorType, String> SUGGESTIONS = Map.ofEntries(
            Map.entry(ErrorType.LEFT_RECURSION,
                    "Use ANTLR4's precedence climbing for direct left-recursion. " +
                    "For example: expr : expr '*' expr | expr '+' expr | NUMBER ; " +
                    "ANTLR4 handles this automatically with proper precedence."),

            Map.entry(ErrorType.SYNTAX_ERROR,
                    "Check grammar syntax near the error location. " +
                    "Common issues: missing semicolons, unmatched parentheses, invalid rule names. " +
                    "Rule names must start with lowercase, lexer rules with uppercase."),

            Map.entry(ErrorType.UNDEFINED_RULE,
                    "Define the referenced rule or check for typos in rule names. " +
                    "Parser rule names must start with lowercase (e.g., 'expr'). " +
                    "Lexer rule names must start with uppercase (e.g., 'NUMBER')."),

            Map.entry(ErrorType.TOKEN_CONFLICT,
                    "Reorder lexer rules - more specific patterns should come first. " +
                    "Or use lexer modes to separate conflicting token definitions. " +
                    "ANTLR matches the first rule that succeeds."),

            Map.entry(ErrorType.AMBIGUITY,
                    "Reorder alternatives in your rules, with more specific patterns first. " +
                    "Consider using semantic predicates to disambiguate. " +
                    "Or adjust grammar to eliminate the ambiguity structurally."),

            Map.entry(ErrorType.PARSE_TIMEOUT,
                    "Check for infinite loops or left-recursive rules without proper handling. " +
                    "Simplify complex rules or increase the timeout if the grammar is legitimately complex. " +
                    "Use profile_grammar tool to identify problematic rules."),

            Map.entry(ErrorType.PARSE_ERROR,
                    "Verify that the input matches the grammar's expected format. " +
                    "Check the start rule is correct. " +
                    "Use parse_with_trace tool to see where parsing fails."),

            Map.entry(ErrorType.GRAMMAR_LOAD_ERROR,
                    "Ensure the grammar syntax is valid and all referenced rules are defined. " +
                    "Check for circular dependencies or invalid grammar structure."),

            Map.entry(ErrorType.IMPORT_ERROR,
                    "Verify that imported grammars exist and are accessible. " +
                    "Check the import path and grammar names are correct. " +
                    "Use build_language_context tool for multi-file grammars."),

            Map.entry(ErrorType.INVALID_INPUT,
                    "Check that all required parameters are provided and have valid values. " +
                    "Verify input size doesn't exceed configured limits."),

            Map.entry(ErrorType.SEMANTIC_ERROR,
                    "Review semantic predicates and actions in your grammar. " +
                    "Ensure runtime semantic checks are correctly implemented."),

            Map.entry(ErrorType.INTERNAL_ERROR,
                    "An unexpected error occurred during processing. " +
                    "Please report this issue with the grammar and input that caused it.")
    );

    private static final Map<ErrorType, String> EXAMPLES = Map.ofEntries(
            Map.entry(ErrorType.LEFT_RECURSION,
                    "// Direct left-recursion (ANTLR4 handles this)\n" +
                    "expr\n" +
                    "    : expr '*' expr\n" +
                    "    | expr '+' expr\n" +
                    "    | NUMBER\n" +
                    "    ;"),

            Map.entry(ErrorType.SYNTAX_ERROR,
                    "// Correct syntax\n" +
                    "statement\n" +
                    "    : assignment ';'\n" +
                    "    | ifStatement\n" +
                    "    ;"),

            Map.entry(ErrorType.UNDEFINED_RULE,
                    "// Define missing rule\n" +
                    "expression : term (('+' | '-') term)* ;\n" +
                    "term : NUMBER ;  // Define the missing 'term' rule"),

            Map.entry(ErrorType.TOKEN_CONFLICT,
                    "// Lexer rule ordering\n" +
                    "IF : 'if' ;           // Specific keyword first\n" +
                    "ID : [a-z]+ ;         // Generic identifier after"),

            Map.entry(ErrorType.AMBIGUITY,
                    "// Disambiguate with rule ordering\n" +
                    "statement\n" +
                    "    : 'if' expr 'then' statement 'else' statement  // More specific first\n" +
                    "    | 'if' expr 'then' statement                    // Less specific after\n" +
                    "    ;")
    );

    private static final Map<ErrorType, String> DOCUMENTATION_URLS = Map.ofEntries(
            Map.entry(ErrorType.LEFT_RECURSION,
                    "https://github.com/antlr/antlr4/blob/master/doc/left-recursion.md"),

            Map.entry(ErrorType.SYNTAX_ERROR,
                    "https://github.com/antlr/antlr4/blob/master/doc/index.md"),

            Map.entry(ErrorType.UNDEFINED_RULE,
                    "https://github.com/antlr/antlr4/blob/master/doc/index.md"),

            Map.entry(ErrorType.TOKEN_CONFLICT,
                    "https://github.com/antlr/antlr4/blob/master/doc/lexer-rules.md"),

            Map.entry(ErrorType.AMBIGUITY,
                    "https://github.com/antlr/antlr4/blob/master/doc/predicates.md"),

            Map.entry(ErrorType.PARSE_TIMEOUT,
                    "https://github.com/antlr/antlr4/blob/master/doc/left-recursion.md"),

            Map.entry(ErrorType.PARSE_ERROR,
                    "https://github.com/antlr/antlr4/blob/master/doc/index.md"),

            Map.entry(ErrorType.GRAMMAR_LOAD_ERROR,
                    "https://github.com/antlr/antlr4/blob/master/doc/grammars.md"),

            Map.entry(ErrorType.IMPORT_ERROR,
                    "https://github.com/antlr/antlr4/blob/master/doc/grammars.md#importing-grammars"),

            Map.entry(ErrorType.INTERNAL_ERROR,
                    "https://github.com/antlr/antlr4/blob/master/doc/index.md")
    );

    /**
     * Get suggestion for the given error type
     *
     * @param errorType The type of error
     * @param context   Additional context (e.g., rule names, similar alternatives)
     * @return Actionable fix suggestion
     */
    public String getSuggestion(ErrorType errorType, String context) {
        String baseSuggestion = SUGGESTIONS.getOrDefault(errorType,
                "Check ANTLR4 documentation for guidance on this error type.");

        if (context != null && !context.isEmpty()) {
            return baseSuggestion + "\n\nContext: " + context;
        }

        return baseSuggestion;
    }

    /**
     * Get example code demonstrating the fix
     *
     * @param errorType The type of error
     * @return Example code or null if no example available
     */
    public String getExample(ErrorType errorType) {
        return EXAMPLES.get(errorType);
    }

    /**
     * Get documentation URL for the error type
     *
     * @param errorType The type of error
     * @return Documentation URL
     */
    public String getDocumentationUrl(ErrorType errorType) {
        return DOCUMENTATION_URLS.getOrDefault(errorType,
                "https://github.com/antlr/antlr4/blob/master/doc/index.md");
    }

    /**
     * Get the error category based on error type
     *
     * @param errorType The type of error
     * @return Category string ("grammar", "parsing", "internal")
     */
    public String getCategory(ErrorType errorType) {
        return switch (errorType) {
            case LEFT_RECURSION, SYNTAX_ERROR, UNDEFINED_RULE, TOKEN_CONFLICT,
                    AMBIGUITY, SEMANTIC_ERROR, GRAMMAR_LOAD_ERROR, IMPORT_ERROR -> "grammar";
            case PARSE_TIMEOUT, PARSE_ERROR -> "parsing";
            case INVALID_INPUT -> "input";
            case INTERNAL_ERROR -> "internal";
        };
    }
}
