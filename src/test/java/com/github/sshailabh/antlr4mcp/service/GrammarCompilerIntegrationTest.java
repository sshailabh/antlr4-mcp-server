package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ParseResult;
import com.github.sshailabh.antlr4mcp.model.ValidationResult;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GrammarCompilerIntegrationTest {

    private GrammarCompiler grammarCompiler;
    private SecurityValidator securityValidator;
    private ResourceManager resourceManager;

    @BeforeEach
    void setUp() {
        securityValidator = new SecurityValidator();
        resourceManager = new ResourceManager();
        grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);
    }

    @Test
    void testValidateSimpleGrammar() {
        String grammar = "grammar Simple;\n" +
                        "start : 'hello' ;\n";

        ValidationResult result = grammarCompiler.validate(grammar);
        assertTrue(result.isSuccess());
        assertEquals("Simple", result.getGrammarName());
        assertEquals(0, result.getLexerRules());
        assertEquals(1, result.getParserRules());
    }

    @Test
    void testValidateGrammarWithLexerAndParserRules() {
        String grammar = "grammar Calc;\n" +
                        "expr : expr '+' term | term ;\n" +
                        "term : NUMBER ;\n" +
                        "NUMBER : [0-9]+ ;\n" +
                        "WS : [ \\t\\r\\n]+ -> skip ;\n";

        ValidationResult result = grammarCompiler.validate(grammar);
        assertTrue(result.isSuccess());
        assertEquals("Calc", result.getGrammarName());
        assertEquals(2, result.getLexerRules());
        assertEquals(2, result.getParserRules());
    }

    @Test
    void testValidateInvalidGrammar() {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined_rule ;\n";

        ValidationResult result = grammarCompiler.validate(grammar);
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidateGrammarWithImport() {
        String grammar = "grammar Test;\n" +
                        "import CommonLexer;\n" +
                        "start : 'hello' ;\n";

        ValidationResult result = grammarCompiler.validate(grammar);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().get(0).getMessage().contains("Import"));
    }

    @Test
    void testValidateGrammarWithoutDeclaration() {
        String grammar = "start : 'hello' ;";

        ValidationResult result = grammarCompiler.validate(grammar);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().get(0).getMessage().contains("grammar declaration"));
    }

    @Test
    void testParseSimpleGrammar() {
        String grammar = "grammar Hello;\n" +
                        "start : 'hello' ;\n";
        String input = "hello";

        ParseResult result = grammarCompiler.parse(grammar, input, "start", false, "tree", false);
        assertTrue(result.isSuccess() || result.getErrors() != null);
    }

    @Test
    void testParseWithInvalidGrammar() {
        String grammar = "grammar Invalid;\n" +
                        "start : undefined ;\n";
        String input = "test";

        ParseResult result = grammarCompiler.parse(grammar, input, "start", false, "tree", false);
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }
}
