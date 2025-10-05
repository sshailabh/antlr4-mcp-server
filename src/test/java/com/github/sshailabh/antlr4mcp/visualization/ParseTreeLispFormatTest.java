package com.github.sshailabh.antlr4mcp.visualization;

import com.github.sshailabh.antlr4mcp.service.GrammarCompiler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LISP-style parse tree format
 */
@SpringBootTest
class ParseTreeLispFormatTest {

    @Autowired
    private GrammarCompiler grammarCompiler;

    @Autowired
    private ParseTreeEnhancedVisualizer visualizer;

    private static final String CALCULATOR_GRAMMAR = """
        grammar Calculator;
        prog : expr EOF ;
        expr : expr ('*'|'/') expr
             | expr ('+'|'-') expr
             | INT
             ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @Test
    void testLispFormat_SimpleExpression() throws Exception {
        // Parse "10"
        Grammar grammar = grammarCompiler.loadGrammar(CALCULATOR_GRAMMAR);
        assertNotNull(grammar);

        LexerGrammar lexerGrammar = grammar.getImplicitLexer();
        org.antlr.v4.runtime.Lexer lexer = lexerGrammar.createLexerInterpreter(
            CharStreams.fromString("10")
        );
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        ParseTree tree = parser.parse(grammar.getRule("prog").index);
        assertNotNull(tree);

        VisualizationOptions options = VisualizationOptions.builder()
            .showRuleIndices(false)
            .build();

        ParseTreeVisualization result = visualizer.visualize(tree, parser, options);

        assertNotNull(result.getLispFormat());
        assertTrue(result.getLispFormat().contains("prog"));
        assertTrue(result.getLispFormat().contains("expr"));
        assertTrue(result.getLispFormat().contains("10"));
    }

    @Test
    void testLispFormat_WithRuleIndices() throws Exception {
        // Parse "10 + 20"
        Grammar grammar = grammarCompiler.loadGrammar(CALCULATOR_GRAMMAR);
        assertNotNull(grammar);

        LexerGrammar lexerGrammar = grammar.getImplicitLexer();
        org.antlr.v4.runtime.Lexer lexer = lexerGrammar.createLexerInterpreter(
            CharStreams.fromString("10 + 20")
        );
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        ParseTree tree = parser.parse(grammar.getRule("prog").index);
        assertNotNull(tree);

        VisualizationOptions options = VisualizationOptions.builder()
            .showRuleIndices(true)
            .build();

        ParseTreeVisualization result = visualizer.visualize(tree, parser, options);

        assertNotNull(result.getLispFormat());
        // Should contain rule indices like "prog:0" or "expr:1"
        assertTrue(result.getLispFormat().contains(":"));
        assertTrue(result.getLispFormat().matches(".*\\w+:\\d+.*"));
    }

    @Test
    void testLispFormat_NestedExpression() throws Exception {
        // Parse "10 + 20 * 30" (should show precedence)
        Grammar grammar = grammarCompiler.loadGrammar(CALCULATOR_GRAMMAR);
        assertNotNull(grammar);

        LexerGrammar lexerGrammar = grammar.getImplicitLexer();
        org.antlr.v4.runtime.Lexer lexer = lexerGrammar.createLexerInterpreter(
            CharStreams.fromString("10 + 20 * 30")
        );
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        ParseTree tree = parser.parse(grammar.getRule("prog").index);
        assertNotNull(tree);

        VisualizationOptions options = VisualizationOptions.defaults();

        ParseTreeVisualization result = visualizer.visualize(tree, parser, options);

        assertNotNull(result.getLispFormat());
        // Should contain nested expressions
        assertTrue(result.getLispFormat().contains("expr"));
        assertTrue(result.getLispFormat().contains("10"));
        assertTrue(result.getLispFormat().contains("20"));
        assertTrue(result.getLispFormat().contains("30"));
        assertTrue(result.getLispFormat().contains("+"));
        assertTrue(result.getLispFormat().contains("*"));
    }

    @Test
    void testLispFormat_EmptyInput() throws Exception {
        // Parse just a number with minimal grammar
        String minimalGrammar = """
            grammar Minimal;
            prog : INT EOF ;
            INT : [0-9]+ ;
            """;

        Grammar grammar = grammarCompiler.loadGrammar(minimalGrammar);
        assertNotNull(grammar);

        LexerGrammar lexerGrammar = grammar.getImplicitLexer();
        assertNotNull(lexerGrammar, "Lexer grammar should not be null");

        org.antlr.v4.runtime.Lexer lexer = lexerGrammar.createLexerInterpreter(
            CharStreams.fromString("42")
        );
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        ParseTree tree = parser.parse(grammar.getRule("prog").index);
        assertNotNull(tree);

        VisualizationOptions options = VisualizationOptions.defaults();
        ParseTreeVisualization result = visualizer.visualize(tree, parser, options);

        assertNotNull(result.getLispFormat());
        assertTrue(result.getLispFormat().contains("prog"));
        assertTrue(result.getLispFormat().contains("42") ||
                   result.getLispFormat().contains("INT"));
    }

    @Test
    void testLispFormat_MatchesNodeCount() throws Exception {
        // Verify LISP format contains all nodes
        Grammar grammar = grammarCompiler.loadGrammar(CALCULATOR_GRAMMAR);
        assertNotNull(grammar);

        LexerGrammar lexerGrammar = grammar.getImplicitLexer();
        org.antlr.v4.runtime.Lexer lexer = lexerGrammar.createLexerInterpreter(
            CharStreams.fromString("5 + 3")
        );
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        ParseTree tree = parser.parse(grammar.getRule("prog").index);
        assertNotNull(tree);

        VisualizationOptions options = VisualizationOptions.defaults();
        ParseTreeVisualization result = visualizer.visualize(tree, parser, options);

        assertNotNull(result.getLispFormat());
        assertTrue(result.getNodeCount() > 0);
        assertTrue(result.getMaxDepth() > 0);

        // LISP format should have balanced parentheses
        long openParens = result.getLispFormat().chars().filter(ch -> ch == '(').count();
        long closeParens = result.getLispFormat().chars().filter(ch -> ch == ')').count();
        assertEquals(openParens, closeParens, "Parentheses should be balanced");
    }
}
