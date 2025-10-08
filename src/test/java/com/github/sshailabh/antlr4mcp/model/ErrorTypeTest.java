package com.github.sshailabh.antlr4mcp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorTypeTest {

    @Test
    void testGetCode() {
        assertEquals("left_recursion", ErrorType.LEFT_RECURSION.getCode());
        assertEquals("syntax_error", ErrorType.SYNTAX_ERROR.getCode());
        assertEquals("parse_timeout", ErrorType.PARSE_TIMEOUT.getCode());
    }

    @Test
    void testGetDescription() {
        assertEquals("Left-recursive rule detected", ErrorType.LEFT_RECURSION.getDescription());
        assertEquals("Grammar syntax error", ErrorType.SYNTAX_ERROR.getDescription());
    }

    @Test
    void testFromCode() {
        assertEquals(ErrorType.LEFT_RECURSION, ErrorType.fromCode("left_recursion"));
        assertEquals(ErrorType.SYNTAX_ERROR, ErrorType.fromCode("syntax_error"));
        assertEquals(ErrorType.PARSE_TIMEOUT, ErrorType.fromCode("parse_timeout"));
    }

    @Test
    void testFromCodeUnknown() {
        assertEquals(ErrorType.INTERNAL_ERROR, ErrorType.fromCode("unknown_code"));
        assertEquals(ErrorType.INTERNAL_ERROR, ErrorType.fromCode("invalid"));
    }

    @Test
    void testAllErrorTypesHaveUniqueCode() {
        ErrorType[] values = ErrorType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i].getCode(), values[j].getCode(),
                        "Error types must have unique codes: " + values[i] + " and " + values[j]);
            }
        }
    }
}
