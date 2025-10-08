package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorSuggestionsTest {

    private ErrorSuggestions errorSuggestions;

    @BeforeEach
    void setUp() {
        errorSuggestions = new ErrorSuggestions();
    }

    @Test
    void testGetSuggestionForLeftRecursion() {
        String suggestion = errorSuggestions.getSuggestion(ErrorType.LEFT_RECURSION, null);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("precedence"));
        assertTrue(suggestion.contains("ANTLR4"));
    }

    @Test
    void testGetSuggestionForSyntaxError() {
        String suggestion = errorSuggestions.getSuggestion(ErrorType.SYNTAX_ERROR, null);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("syntax"));
        assertTrue(suggestion.toLowerCase().contains("semicolon") ||
                        suggestion.toLowerCase().contains("parenthes") ||
                        suggestion.toLowerCase().contains("rule names"));
    }

    @Test
    void testGetSuggestionForUndefinedRule() {
        String suggestion = errorSuggestions.getSuggestion(ErrorType.UNDEFINED_RULE, null);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("Define"));
        assertTrue(suggestion.contains("lowercase") || suggestion.contains("uppercase"));
    }

    @Test
    void testGetSuggestionForAmbiguity() {
        String suggestion = errorSuggestions.getSuggestion(ErrorType.AMBIGUITY, null);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("Reorder") || suggestion.contains("predicate"));
    }

    @Test
    void testGetSuggestionForParseTimeout() {
        String suggestion = errorSuggestions.getSuggestion(ErrorType.PARSE_TIMEOUT, null);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("infinite") || suggestion.contains("timeout"));
    }

    @Test
    void testGetSuggestionWithContext() {
        String context = "In rule 'expr' at line 10";
        String suggestion = errorSuggestions.getSuggestion(ErrorType.SYNTAX_ERROR, context);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains(context));
        assertTrue(suggestion.contains("Context:"));
    }

    @Test
    void testGetSuggestionForUnknownErrorType() {
        String suggestion = errorSuggestions.getSuggestion(ErrorType.INTERNAL_ERROR, null);

        assertNotNull(suggestion);
        assertTrue(suggestion.contains("unexpected") || suggestion.contains("report"));
    }

    @Test
    void testGetExampleForLeftRecursion() {
        String example = errorSuggestions.getExample(ErrorType.LEFT_RECURSION);

        assertNotNull(example);
        assertTrue(example.contains("expr"));
        assertTrue(example.contains("*") || example.contains("+"));
    }

    @Test
    void testGetExampleForSyntaxError() {
        String example = errorSuggestions.getExample(ErrorType.SYNTAX_ERROR);

        assertNotNull(example);
        assertTrue(example.contains("statement") || example.contains(";"));
    }

    @Test
    void testGetExampleForUndefinedRule() {
        String example = errorSuggestions.getExample(ErrorType.UNDEFINED_RULE);

        assertNotNull(example);
        assertTrue(example.contains("term") || example.contains("expression"));
    }

    @Test
    void testGetExampleForTokenConflict() {
        String example = errorSuggestions.getExample(ErrorType.TOKEN_CONFLICT);

        assertNotNull(example);
        assertTrue(example.contains("IF") || example.contains("ID"));
    }

    @Test
    void testGetExampleReturnsNullForUnknownType() {
        String example = errorSuggestions.getExample(ErrorType.INTERNAL_ERROR);

        assertNull(example);
    }

    @Test
    void testGetDocumentationUrlForLeftRecursion() {
        String url = errorSuggestions.getDocumentationUrl(ErrorType.LEFT_RECURSION);

        assertNotNull(url);
        assertTrue(url.contains("github.com/antlr/antlr4"));
        assertTrue(url.contains("left-recursion.md"));
    }

    @Test
    void testGetDocumentationUrlForSyntaxError() {
        String url = errorSuggestions.getDocumentationUrl(ErrorType.SYNTAX_ERROR);

        assertNotNull(url);
        assertTrue(url.contains("github.com/antlr/antlr4"));
        assertTrue(url.contains("doc"));
    }

    @Test
    void testGetDocumentationUrlForAmbiguity() {
        String url = errorSuggestions.getDocumentationUrl(ErrorType.AMBIGUITY);

        assertNotNull(url);
        assertTrue(url.contains("github.com/antlr/antlr4"));
        assertTrue(url.contains("predicates.md"));
    }

    @Test
    void testGetDocumentationUrlForUnknownType() {
        String url = errorSuggestions.getDocumentationUrl(ErrorType.INTERNAL_ERROR);

        assertNotNull(url);
        assertTrue(url.contains("github.com/antlr/antlr4"));
        assertTrue(url.contains("index.md"));
    }

    @Test
    void testGetCategoryForGrammarErrors() {
        assertEquals("grammar", errorSuggestions.getCategory(ErrorType.LEFT_RECURSION));
        assertEquals("grammar", errorSuggestions.getCategory(ErrorType.SYNTAX_ERROR));
        assertEquals("grammar", errorSuggestions.getCategory(ErrorType.UNDEFINED_RULE));
        assertEquals("grammar", errorSuggestions.getCategory(ErrorType.TOKEN_CONFLICT));
        assertEquals("grammar", errorSuggestions.getCategory(ErrorType.AMBIGUITY));
        assertEquals("grammar", errorSuggestions.getCategory(ErrorType.SEMANTIC_ERROR));
    }

    @Test
    void testGetCategoryForParsingErrors() {
        assertEquals("parsing", errorSuggestions.getCategory(ErrorType.PARSE_TIMEOUT));
        assertEquals("parsing", errorSuggestions.getCategory(ErrorType.PARSE_ERROR));
    }

    @Test
    void testGetCategoryForInputErrors() {
        assertEquals("input", errorSuggestions.getCategory(ErrorType.INVALID_INPUT));
    }

    @Test
    void testGetCategoryForInternalErrors() {
        assertEquals("internal", errorSuggestions.getCategory(ErrorType.INTERNAL_ERROR));
    }

    @Test
    void testAllErrorTypesHaveSuggestions() {
        for (ErrorType errorType : ErrorType.values()) {
            String suggestion = errorSuggestions.getSuggestion(errorType, null);
            assertNotNull(suggestion, "Missing suggestion for " + errorType);
            assertFalse(suggestion.isEmpty(), "Empty suggestion for " + errorType);
        }
    }

    @Test
    void testAllErrorTypesHaveDocumentation() {
        for (ErrorType errorType : ErrorType.values()) {
            String url = errorSuggestions.getDocumentationUrl(errorType);
            assertNotNull(url, "Missing documentation URL for " + errorType);
            assertTrue(url.startsWith("https://"), "Invalid URL for " + errorType);
        }
    }

    @Test
    void testAllErrorTypesHaveCategory() {
        for (ErrorType errorType : ErrorType.values()) {
            String category = errorSuggestions.getCategory(errorType);
            assertNotNull(category, "Missing category for " + errorType);
            assertTrue(category.matches("grammar|parsing|input|internal"),
                    "Invalid category for " + errorType + ": " + category);
        }
    }
}
