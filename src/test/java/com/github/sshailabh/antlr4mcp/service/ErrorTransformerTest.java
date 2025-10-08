package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorTransformerTest {

    private ErrorTransformer errorTransformer;
    private ErrorSuggestions errorSuggestions;

    @BeforeEach
    void setUp() {
        errorSuggestions = new ErrorSuggestions();
        errorTransformer = new ErrorTransformer(errorSuggestions);
    }

    @Test
    void testTransformTimeoutException() {
        TimeoutException e = new TimeoutException("Operation timed out");

        ErrorResponse response = errorTransformer.transform(e, "test context");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("parse_timeout", response.getError().getType());
        assertEquals("parsing", response.getError().getCategory());
        assertTrue(response.getError().getMessage().contains("timeout"));
        assertNotNull(response.getError().getSuggestion());
        assertNotNull(response.getError().getDocumentation());
    }

    @Test
    void testTransformIllegalArgumentException() {
        IllegalArgumentException e = new IllegalArgumentException("Invalid grammar name");

        ErrorResponse response = errorTransformer.transform(e, "validate_grammar");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("invalid_input", response.getError().getType());
        assertEquals("input", response.getError().getCategory());
        assertEquals("Invalid grammar name", response.getError().getMessage());
        assertEquals("validate_grammar", response.getError().getContext());
    }

    @Test
    void testTransformGenericException() {
        Exception e = new RuntimeException("Something went wrong");

        ErrorResponse response = errorTransformer.transform(e, "internal operation");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("internal_error", response.getError().getType());
        assertEquals("internal", response.getError().getCategory());
        assertTrue(response.getError().getMessage().contains("Something went wrong"));
        assertEquals("internal operation", response.getError().getContext());
    }

    @Test
    void testTransformExceptionWithNullMessage() {
        Exception e = new RuntimeException((String) null);

        ErrorResponse response = errorTransformer.transform(e, null);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getError().getMessage().contains("Internal error"));
    }

    @Test
    void testErrorResponseIncludesSuggestion() {
        TimeoutException e = new TimeoutException();

        ErrorResponse response = errorTransformer.transform(e, null);

        assertNotNull(response.getError().getSuggestion());
        assertTrue(response.getError().getSuggestion().contains("infinite") ||
                response.getError().getSuggestion().contains("timeout"));
    }

    @Test
    void testErrorResponseIncludesDocumentation() {
        IllegalArgumentException e = new IllegalArgumentException("test");

        ErrorResponse response = errorTransformer.transform(e, null);

        assertNotNull(response.getError().getDocumentation());
        assertTrue(response.getError().getDocumentation().startsWith("https://"));
    }

    @Test
    void testErrorResponseIncludesCategory() {
        TimeoutException e = new TimeoutException();

        ErrorResponse response = errorTransformer.transform(e, null);

        assertNotNull(response.getError().getCategory());
        assertEquals("parsing", response.getError().getCategory());
    }

    @Test
    void testErrorResponseSeverityIsError() {
        Exception e = new RuntimeException("test");

        ErrorResponse response = errorTransformer.transform(e, null);

        assertEquals("error", response.getError().getSeverity());
    }

    @Test
    void testContextIsPreserved() {
        String context = "While validating grammar in tool xyz";
        Exception e = new IllegalArgumentException("Invalid input");

        ErrorResponse response = errorTransformer.transform(e, context);

        assertEquals(context, response.getError().getContext());
        assertTrue(response.getError().getSuggestion().contains(context));
    }
}
