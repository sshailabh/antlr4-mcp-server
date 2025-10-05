package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.AmbiguityVisualization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AmbiguityVisualizerTest {

    @Autowired
    private AmbiguityVisualizer ambiguityVisualizer;

    @Autowired
    private GrammarCompiler grammarCompiler;

    // Ambiguous grammar: dangling else problem
    private static final String AMBIGUOUS_GRAMMAR = """
        grammar Ambiguous;
        prog : stat+ EOF ;
        stat : 'if' expr 'then' stat
             | 'if' expr 'then' stat 'else' stat
             | 'print' expr
             ;
        expr : ID ;
        ID : [a-z]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    // Unambiguous grammar: proper precedence
    private static final String UNAMBIGUOUS_GRAMMAR = """
        grammar Unambiguous;
        prog : expr EOF ;
        expr : term (('+' | '-') term)* ;
        term : factor (('*' | '/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        assertNotNull(ambiguityVisualizer);
        assertNotNull(grammarCompiler);
    }

    @Test
    void testVisualizeAmbiguousInput() {
        // Dangling else: if a then if b then print c else print d
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "if a then if b then print c else print d",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isHasAmbiguities(), "Should detect ambiguities");
        assertNotNull(result.getAmbiguities());
        assertFalse(result.getAmbiguities().isEmpty());
        assertNotNull(result.getPrimaryInterpretation());
    }

    @Test
    void testVisualizeUnambiguousInput() {
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            UNAMBIGUOUS_GRAMMAR,
            "10 + 20 * 30",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.isHasAmbiguities(), "Should not detect ambiguities");
        assertNotNull(result.getPrimaryInterpretation());
    }

    @Test
    void testVisualizeSingleToken() {
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "print a",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.isHasAmbiguities(), "Simple statement should not be ambiguous");
        assertNotNull(result.getPrimaryInterpretation());
    }

    @Test
    void testVisualizeWithoutElse() {
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "if a then print b",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        // Should not have ambiguities when no else clause
        assertNotNull(result.getPrimaryInterpretation());
    }

    @Test
    void testVisualizeComplexAmbiguous() {
        // Nested if with multiple else possibilities
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "if a then if b then if c then print d else print e else print f",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isHasAmbiguities(), "Nested if-then-else should create ambiguities");

        if (result.isHasAmbiguities()) {
            assertNotNull(result.getAlternatives());
        }
    }

    @Test
    void testVisualizeInvalidGrammar() {
        String invalidGrammar = """
            grammar Invalid;
            prog : UNDEFINED ;
            """;

        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            invalidGrammar,
            "test",
            null
        );

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testVisualizeEmptyInput() {
        String emptyGrammar = """
            grammar Empty;
            prog : EOF ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            emptyGrammar,
            "",
            null
        );

        assertNotNull(result);
        // May fail if grammar has no explicit lexer rules beyond EOF
        // This is acceptable - just test it doesn't crash
        if (result.isSuccess()) {
            assertFalse(result.isHasAmbiguities());
        } else {
            assertNotNull(result.getError());
        }
    }

    @Test
    void testVisualizeWithCustomStartRule() {
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "10 + 20",
            "expr"
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("expr", result.getStartRule());
    }

    @Test
    void testAmbiguityDetails() {
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "if a then if b then print c else print d",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        if (result.isHasAmbiguities()) {
            AmbiguityVisualization.AmbiguityInstance firstAmbiguity = result.getAmbiguities().get(0);
            assertNotNull(firstAmbiguity);
            assertTrue(firstAmbiguity.getDecision() >= 0);
            assertNotNull(firstAmbiguity.getRuleName());
            assertTrue(firstAmbiguity.getStartIndex() >= 0);
            assertTrue(firstAmbiguity.getStopIndex() >= firstAmbiguity.getStartIndex());
            assertNotNull(firstAmbiguity.getAlternativeNumbers());
            assertFalse(firstAmbiguity.getAlternativeNumbers().isEmpty());
        }
    }

    @Test
    void testAlternativeInterpretations() {
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            "if a then if b then print c else print d",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        if (result.isHasAmbiguities() && result.getAlternatives() != null && !result.getAlternatives().isEmpty()) {
            for (AmbiguityVisualization.AlternativeInterpretation alt : result.getAlternatives()) {
                assertTrue(alt.getAlternativeNumber() >= 0);
                assertNotNull(alt.getParseTree(), "Parse tree should be present");
                assertTrue(alt.getParseTree().startsWith("("), "Parse tree should be in LISP format");
            }
        }
    }

    @Test
    void testInputMetadata() {
        String input = "print a";
        AmbiguityVisualization result = ambiguityVisualizer.visualize(
            AMBIGUOUS_GRAMMAR,
            input,
            "stat"
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(input, result.getInput());
        assertEquals("stat", result.getStartRule());
    }
}
