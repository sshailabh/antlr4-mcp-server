package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.VisualizationResult;
import com.github.sshailabh.antlr4mcp.security.ResourceManager;
import com.github.sshailabh.antlr4mcp.security.SecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TreeVisualizer Service Tests")
class TreeVisualizerTest {

    private TreeVisualizer treeVisualizer;
    private GrammarCompiler grammarCompiler;

    // Test grammars
    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        start : 'hello' 'world' ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String EXPR_GRAMMAR = """
        grammar Expr;
        expr : expr '+' term
             | term
             ;
        term : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String NESTED_GRAMMAR = """
        grammar Nested;
        statement : 'if' expr 'then' statement
                  | 'print' expr
                  ;
        expr : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    @BeforeEach
    void setUp() {
        SecurityValidator securityValidator = new SecurityValidator();
        ResourceManager resourceManager = new ResourceManager();
        grammarCompiler = new GrammarCompiler(securityValidator, resourceManager);
        ReflectionTestUtils.setField(grammarCompiler, "maxGrammarSizeMb", 10);

        treeVisualizer = new TreeVisualizer(grammarCompiler);
    }

    @Test
    @DisplayName("Should visualize simple parse tree in LISP format")
    void testVisualizeSimpleTreeLisp() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            SIMPLE_GRAMMAR, "start", sampleInput, "lisp");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Visualization should succeed");
        assertNotNull(result.getVisualization());
        assertEquals("lisp", result.getFormat());
        assertEquals("start", result.getRuleName());

        // LISP format should contain rule name and tokens
        assertTrue(result.getVisualization().contains("start"));
        assertTrue(result.getVisualization().contains("hello"));
        assertTrue(result.getVisualization().contains("world"));
    }

    @Test
    @DisplayName("Should visualize expression tree")
    void testVisualizeExpressionTree() {
        // Arrange
        String sampleInput = "1 + 2";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            EXPR_GRAMMAR, "expr", sampleInput, "lisp");

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getVisualization());
        assertTrue(result.getVisualization().contains("expr"));
        assertTrue(result.getVisualization().contains("+"));
    }

    @Test
    @DisplayName("Should visualize nested parse tree")
    void testVisualizeNestedTree() {
        // Arrange
        String sampleInput = "if 1 then print 2";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            NESTED_GRAMMAR, "statement", sampleInput, "lisp");

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getVisualization());
        assertTrue(result.getVisualization().contains("statement"));
        assertTrue(result.getVisualization().contains("if"));
    }

    @Test
    @DisplayName("Should default to LISP format when format not specified")
    void testDefaultFormat() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            SIMPLE_GRAMMAR, "start", sampleInput, null);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("lisp", result.getFormat());
        assertNotNull(result.getVisualization());
    }

    @Test
    @DisplayName("Should handle DOT format")
    void testDotFormat() {
        // Arrange
        String sampleInput = "hello world";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            SIMPLE_GRAMMAR, "start", sampleInput, "dot");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("dot", result.getFormat());
        assertNotNull(result.getVisualization());
        // DOT format should have graph structure
        assertTrue(result.getVisualization().contains("digraph") ||
                  result.getVisualization().contains("->"));
    }

    @Test
    @DisplayName("Should handle empty input")
    void testEmptyInput() {
        // Arrange
        String grammar = """
            grammar Empty;
            start : item* ;
            item : INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            grammar, "start", "", "lisp");

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getVisualization());
    }

    @Test
    @DisplayName("Should handle invalid grammar gracefully")
    void testInvalidGrammar() {
        // Arrange
        String invalidGrammar = "This is not valid!";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            invalidGrammar, "start", "test", "lisp");

        // Assert
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should handle invalid rule name")
    void testInvalidRuleName() {
        // Act
        VisualizationResult result = treeVisualizer.visualize(
            SIMPLE_GRAMMAR, "nonexistent", "hello world", "lisp");

        // Assert
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrors());
    }

    @Test
    @DisplayName("Should handle parse errors in input")
    void testParseErrors() {
        // Arrange
        String sampleInput = "hello"; // Missing 'world'

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            SIMPLE_GRAMMAR, "start", sampleInput, "lisp");

        // Assert - Even with errors, should return tree with error nodes
        assertNotNull(result);
        // Either success with error nodes OR failure with errors
        assertTrue(result.isSuccess() || !result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should include rule name in result")
    void testRuleNameInResult() {
        // Act
        VisualizationResult result = treeVisualizer.visualize(
            SIMPLE_GRAMMAR, "start", "hello world", "lisp");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("start", result.getRuleName());
    }

    @Test
    @DisplayName("Should handle large input efficiently")
    void testLargeInput() {
        // Arrange
        StringBuilder largeInput = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeInput.append("hello world ");
        }

        String grammar = """
            grammar Large;
            start : ('hello' 'world')* ;
            WS : [ \\t\\r\\n]+ -> skip;
            """;

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            grammar, "start", largeInput.toString().trim(), "lisp");

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getVisualization());
    }

    @Test
    @DisplayName("Should handle complex nested structures")
    void testComplexNesting() {
        // Arrange
        String sampleInput = "if 1 then if 2 then print 3";

        // Act
        VisualizationResult result = treeVisualizer.visualize(
            NESTED_GRAMMAR, "statement", sampleInput, "lisp");

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getVisualization());
        // Should show nested structure
        assertTrue(result.getVisualization().contains("statement"));
    }
}
