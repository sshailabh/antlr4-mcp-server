package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.Ambiguity;
import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("AmbiguityDetector Service Tests")
class AmbiguityDetectorTest {

    @Autowired
    private AmbiguityDetector ambiguityDetector;

    // Test grammars
    private static final String AMBIGUOUS_GRAMMAR_SIMPLE = """
        grammar AmbiguousExpr;
        expr : expr '+' expr
             | expr '*' expr
             | INT
             ;
        INT : [0-9]+;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String NON_AMBIGUOUS_GRAMMAR = """
        grammar SimpleCalc;
        expr : term (('+' | '-') term)*;
        term : factor (('*' | '/') factor)*;
        factor : INT | '(' expr ')';
        INT : [0-9]+;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String LEFT_RECURSIVE_AMBIGUOUS = """
        grammar LeftRec;
        stat : expr ';' ;
        expr : expr '*' expr
             | expr '+' expr
             | INT
             ;
        INT : [0-9]+;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    private static final String COMPLEX_AMBIGUOUS = """
        grammar ComplexAmb;
        statement : ifStatement | otherStatement ;
        ifStatement : 'if' expr 'then' statement
                    | 'if' expr 'then' statement 'else' statement
                    ;
        otherStatement : 'print' expr ';' ;
        expr : INT ;
        INT : [0-9]+;
        WS : [ \\t\\r\\n]+ -> skip;
        """;

    @BeforeEach
    void setUp() {
        assertNotNull(ambiguityDetector, "AmbiguityDetector should be autowired");
    }

    @Test
    @DisplayName("Should detect simple ambiguity in expression grammar")
    void testDetectSimpleAmbiguity() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(AMBIGUOUS_GRAMMAR_SIMPLE);

        // Assert
        assertNotNull(report, "Report should not be null");
        assertTrue(report.isHasAmbiguities(), "Should detect ambiguities");
        assertFalse(report.getAmbiguities().isEmpty(), "Should have at least one ambiguity");

        // Verify ambiguity details
        Ambiguity ambiguity = report.getAmbiguities().get(0);
        assertNotNull(ambiguity.getRuleName(), "Ambiguity should have rule name");
        assertNotNull(ambiguity.getConflictingAlternatives(), "Should have conflicting alternatives");
        assertTrue(ambiguity.getConflictingAlternatives().size() >= 2,
            "Should have at least 2 conflicting alternatives");
    }

    @Test
    @DisplayName("Should not report ambiguities for clean grammar")
    void testNoAmbiguitiesInCleanGrammar() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(NON_AMBIGUOUS_GRAMMAR);

        // Assert
        assertNotNull(report, "Report should not be null");
        assertFalse(report.isHasAmbiguities(), "Should not detect ambiguities in clean grammar");
        assertTrue(report.getAmbiguities().isEmpty(), "Ambiguities list should be empty");
    }

    @Test
    @DisplayName("Should detect ambiguity in left-recursive grammar")
    void testDetectLeftRecursiveAmbiguity() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(LEFT_RECURSIVE_AMBIGUOUS);

        // Assert
        assertNotNull(report, "Report should not be null");
        assertTrue(report.isHasAmbiguities(), "Should detect ambiguities in left-recursive grammar");

        // Check for 'expr' rule ambiguity
        boolean hasExprAmbiguity = report.getAmbiguities().stream()
            .anyMatch(a -> "expr".equals(a.getRuleName()));
        assertTrue(hasExprAmbiguity, "Should detect ambiguity in 'expr' rule");
    }

    @Test
    @DisplayName("Should detect complex ambiguity (dangling else)")
    void testDetectComplexAmbiguity() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(COMPLEX_AMBIGUOUS);

        // Assert
        assertNotNull(report, "Report should not be null");
        assertTrue(report.isHasAmbiguities(), "Should detect dangling else ambiguity");

        // Check for ifStatement ambiguity
        boolean hasIfAmbiguity = report.getAmbiguities().stream()
            .anyMatch(a -> a.getRuleName() != null && a.getRuleName().contains("if"));
        assertTrue(hasIfAmbiguity, "Should detect ambiguity in if statement");
    }

    @Test
    @DisplayName("Should provide meaningful explanation for ambiguities")
    void testAmbiguityExplanations() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(AMBIGUOUS_GRAMMAR_SIMPLE);

        // Assert
        assertFalse(report.getAmbiguities().isEmpty(), "Should have ambiguities");

        Ambiguity ambiguity = report.getAmbiguities().get(0);
        assertNotNull(ambiguity.getExplanation(), "Should have explanation");
        assertFalse(ambiguity.getExplanation().isEmpty(), "Explanation should not be empty");
    }

    @Test
    @DisplayName("Should handle invalid grammar gracefully")
    void testHandleInvalidGrammar() {
        // Arrange
        String invalidGrammar = "This is not a valid grammar!";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            ambiguityDetector.analyze(invalidGrammar);
        }, "Should throw exception for invalid grammar");
    }

    @Test
    @DisplayName("Should handle empty grammar input")
    void testHandleEmptyGrammar() {
        // Arrange
        String emptyGrammar = "";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            ambiguityDetector.analyze(emptyGrammar);
        }, "Should throw exception for empty grammar");
    }

    @Test
    @DisplayName("Should report line and column for ambiguities")
    void testAmbiguityLocation() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(AMBIGUOUS_GRAMMAR_SIMPLE);

        // Assert
        assertFalse(report.getAmbiguities().isEmpty(), "Should have ambiguities");

        Ambiguity ambiguity = report.getAmbiguities().get(0);
        assertNotNull(ambiguity.getLine(), "Should have line number");
        assertNotNull(ambiguity.getColumn(), "Should have column number");
        assertTrue(ambiguity.getLine() > 0, "Line number should be positive");
        assertTrue(ambiguity.getColumn() >= 0, "Column number should be non-negative");
    }

    @Test
    @DisplayName("Should list conflicting alternatives correctly")
    void testConflictingAlternatives() {
        // Act
        AmbiguityReport report = ambiguityDetector.analyze(AMBIGUOUS_GRAMMAR_SIMPLE);

        // Assert
        assertFalse(report.getAmbiguities().isEmpty(), "Should have ambiguities");

        Ambiguity ambiguity = report.getAmbiguities().get(0);
        assertNotNull(ambiguity.getConflictingAlternatives(), "Should have conflicting alternatives");
        assertTrue(ambiguity.getConflictingAlternatives().size() >= 2,
            "Should have at least 2 conflicting alternatives");

        // Verify alternatives are different
        assertEquals(ambiguity.getConflictingAlternatives().size(),
            ambiguity.getConflictingAlternatives().stream().distinct().count(),
            "Conflicting alternatives should be unique");
    }

    @Test
    @DisplayName("Should handle large grammar without timeout")
    void testLargeGrammar() {
        // Arrange - Create a larger grammar
        StringBuilder largeGrammar = new StringBuilder();
        largeGrammar.append("grammar Large;\n");
        largeGrammar.append("start : expr ;\n");
        for (int i = 0; i < 50; i++) {
            largeGrammar.append("expr : expr '+' expr | expr '*' expr | INT ;\n");
        }
        largeGrammar.append("INT : [0-9]+;\n");
        largeGrammar.append("WS : [ \\t\\r\\n]+ -> skip;\n");

        // Act - Should complete within reasonable time
        assertDoesNotThrow(() -> {
            AmbiguityReport report = ambiguityDetector.analyze(largeGrammar.toString());
            assertNotNull(report, "Should return report for large grammar");
        }, "Should handle large grammar without throwing exception");
    }
}
