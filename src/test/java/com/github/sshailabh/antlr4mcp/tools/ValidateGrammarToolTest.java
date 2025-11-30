package com.github.sshailabh.antlr4mcp.tools;

import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import com.github.sshailabh.antlr4mcp.support.AbstractToolTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static com.github.sshailabh.antlr4mcp.support.GrammarFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Production-grade tests for ValidateGrammarTool.
 */
@DisplayName("ValidateGrammarTool Tests")
class ValidateGrammarToolTest extends AbstractToolTest {

    private ValidateGrammarTool tool;
    private GrammarCompiler grammarCompiler;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        SecurityValidator securityValidator = new SecurityValidator();
        grammarCompiler = new GrammarCompiler(securityValidator);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        tool = new ValidateGrammarTool(grammarCompiler, objectMapper);
    }

    // ========== SCHEMA TESTS ==========

    @Test
    @DisplayName("Should have valid tool schema")
    void testToolSchema() {
        assertValidToolSchema(tool.toTool(), "validate_grammar", "grammar_text");
    }

    // ========== VALID GRAMMAR TESTS ==========

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "Simple hello, Hello, 1, 0",
        "Simple calc, SimpleCalc, 3, 2",
        "JSON grammar, Json, 5, 3"
    })
    @DisplayName("Should validate various grammar types")
    void testValidateVariousGrammars(String description, String expectedName,
                                     int expectedParserRules, int expectedLexerRules) throws Exception {
        String grammar = switch (expectedName) {
            case "Hello" -> SIMPLE_HELLO;
            case "SimpleCalc" -> SIMPLE_CALC;
            case "Json" -> JSON_INLINE;
            default -> throw new IllegalArgumentException("Unknown grammar: " + expectedName);
        };

        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(grammar)
                .build()));

        assertToolSuccess(result);

        ValidationResult validation = parseResult(result, ValidationResult.class);
        assertTrue(validation.isSuccess());
        assertEquals(expectedName, validation.getGrammarName());
        assertEquals(expectedParserRules, validation.getParserRules());
        assertTrue(validation.getLexerRules() >= expectedLexerRules);
    }

    @Test
    @DisplayName("Should validate lexer-only grammar")
    void testValidateLexerGrammar() throws Exception {
        String grammar = GrammarFile.COMMON_LEXER.load();

        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(grammar)
                .build()));

        assertToolSuccess(result);

        ValidationResult validation = parseResult(result, ValidationResult.class);
        assertTrue(validation.isSuccess());
        assertEquals("CommonLexer", validation.getGrammarName());
        assertEquals(0, validation.getParserRules());
        assertTrue(validation.getLexerRules() >= 5);
    }

    // ========== INVALID GRAMMAR TESTS ==========

    @ParameterizedTest(name = "Invalid: {0}")
    @ValueSource(strings = {
        UNDEFINED_RULE,
        MISSING_SEMICOLON,
        NO_GRAMMAR_DECLARATION,
        EMPTY_GRAMMAR
    })
    @DisplayName("Should reject invalid grammars")
    void testInvalidGrammars(String invalidGrammar) throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(invalidGrammar)
                .build()));

        assertToolSuccess(result);

        ValidationResult validation = parseResult(result, ValidationResult.class);
        assertFalse(validation.isSuccess());
    }

    // ========== GRAMMAR NAME VALIDATION ==========

    @Test
    @DisplayName("Should validate expected grammar name")
    void testGrammarNameValidation() throws Exception {
        // Test matching name
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(builder().named("Test").withStartRule("'hello'").build())
                .with("grammar_name", "Test")
                .build()));

        assertToolSuccess(result);
        ValidationResult validation = parseResult(result, ValidationResult.class);
        assertTrue(validation.isSuccess());

        // Test mismatched name
        result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(builder().named("Actual").withStartRule("'hello'").build())
                .with("grammar_name", "Expected")
                .build()));

        assertToolSuccess(result);
        validation = parseResult(result, ValidationResult.class);
        assertFalse(validation.isSuccess());
        assertTrue(validation.getErrors().get(0).getMessage().contains("Expected grammar name"));
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle left-recursive grammar")
    void testLeftRecursiveGrammar() throws Exception {
        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(PRECEDENCE_CALC)
                .build()));

        assertToolSuccess(result);

        ValidationResult validation = parseResult(result, ValidationResult.class);
        assertTrue(validation.isSuccess());
    }

    @Test
    @DisplayName("Should validate dynamically built grammar")
    void testDynamicGrammar() throws Exception {
        String grammar = builder()
            .named("Dynamic")
            .withStartRule("expr")
            .withRule("expr", "'hello'")
            .withWhitespace()
            .build();

        McpSchema.CallToolResult result = tool.execute(mockExchange,
            createRequest("validate_grammar", arguments()
                .withGrammar(grammar)
                .build()));

        assertToolSuccess(result);

        ValidationResult validation = parseResult(result, ValidationResult.class);
        assertTrue(validation.isSuccess());
        assertEquals("Dynamic", validation.getGrammarName());
    }
}
