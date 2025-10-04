package com.github.sshailabh.antlr4mcp.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSuccessFactoryMethod() {
        ValidationResult result = ValidationResult.success("TestGrammar", 5, 8);

        assertTrue(result.isSuccess());
        assertEquals("TestGrammar", result.getGrammarName());
        assertEquals(5, result.getLexerRules());
        assertEquals(8, result.getParserRules());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testErrorFactoryMethod() {
        ValidationResult result = ValidationResult.error("Test error message");

        assertFalse(result.isSuccess());
        assertNull(result.getGrammarName());
        assertEquals(1, result.getErrors().size());
        assertEquals("validation_error", result.getErrors().get(0).getType());
        assertEquals("Test error message", result.getErrors().get(0).getMessage());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ValidationResult result = ValidationResult.success("MyGrammar", 3, 5);

        String json = objectMapper.writeValueAsString(result);

        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"grammarName\":\"MyGrammar\""));
        assertTrue(json.contains("\"lexerRules\":3"));
        assertTrue(json.contains("\"parserRules\":5"));
    }

    @Test
    void testErrorListNeverNull() {
        ValidationResult result = ValidationResult.builder()
            .success(true)
            .build();

        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }
}
