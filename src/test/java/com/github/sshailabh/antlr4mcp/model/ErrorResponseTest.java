package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testOfWithTypeAndMessage() {
        ErrorResponse response = ErrorResponse.of("syntax_error", "Invalid syntax");

        assertFalse(response.isSuccess());
        assertEquals("syntax_error", response.getError().getType());
        assertEquals("Invalid syntax", response.getError().getMessage());
    }

    @Test
    void testOfWithErrorType() {
        ErrorResponse response = ErrorResponse.of(ErrorType.LEFT_RECURSION, "Left recursion detected in rule");

        assertFalse(response.isSuccess());
        assertEquals("left_recursion", response.getError().getType());
        assertEquals("Left recursion detected in rule", response.getError().getMessage());
    }

    @Test
    void testOfWithLocation() {
        ErrorLocation location = ErrorLocation.of(5, 10, "expr");
        ErrorResponse response = ErrorResponse.of(ErrorType.SYNTAX_ERROR, "Parse error", location);

        assertFalse(response.isSuccess());
        assertEquals("syntax_error", response.getError().getType());
        assertEquals("Parse error", response.getError().getMessage());
        assertNotNull(response.getError().getLocation());
        assertEquals(5, response.getError().getLocation().getLine());
        assertEquals(10, response.getError().getLocation().getColumn());
        assertEquals("expr", response.getError().getLocation().getRuleName());
    }

    @Test
    void testBuilder() {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .type("ambiguity")
                        .category("grammar")
                        .message("Ambiguity detected")
                        .suggestion("Reorder alternatives")
                        .documentation("https://example.com/doc")
                        .severity("warning")
                        .build())
                .build();

        assertFalse(response.isSuccess());
        assertEquals("ambiguity", response.getError().getType());
        assertEquals("grammar", response.getError().getCategory());
        assertEquals("Ambiguity detected", response.getError().getMessage());
        assertEquals("Reorder alternatives", response.getError().getSuggestion());
        assertEquals("https://example.com/doc", response.getError().getDocumentation());
        assertEquals("warning", response.getError().getSeverity());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ErrorLocation location = ErrorLocation.builder()
                .line(12)
                .column(5)
                .ruleName("expr")
                .build();

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .type("syntax_error")
                        .category("grammar")
                        .message("Syntax error detected")
                        .location(location)
                        .suggestion("Fix the syntax")
                        .severity("error")
                        .build())
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertNotNull(json);
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("\"type\":\"syntax_error\""));
        assertTrue(json.contains("\"category\":\"grammar\""));
        assertTrue(json.contains("\"line\":12"));
        assertTrue(json.contains("\"column\":5"));
        assertTrue(json.contains("\"ruleName\":\"expr\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
                {
                    "success": false,
                    "error": {
                        "type": "parse_timeout",
                        "category": "parsing",
                        "message": "Parsing timeout exceeded",
                        "suggestion": "Check for infinite loops",
                        "severity": "error"
                    }
                }
                """;

        ErrorResponse response = objectMapper.readValue(json, ErrorResponse.class);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("parse_timeout", response.getError().getType());
        assertEquals("parsing", response.getError().getCategory());
        assertEquals("Parsing timeout exceeded", response.getError().getMessage());
        assertEquals("Check for infinite loops", response.getError().getSuggestion());
    }

    @Test
    void testNullFieldsNotSerializedInJson() throws Exception {
        ErrorResponse response = ErrorResponse.of(ErrorType.INVALID_INPUT, "Invalid input");

        String json = objectMapper.writeValueAsString(response);

        assertNotNull(json);
        assertFalse(json.contains("\"location\""));
        assertFalse(json.contains("\"suggestion\""));
        assertFalse(json.contains("\"documentation\""));
        assertFalse(json.contains("\"example\""));
    }
}
