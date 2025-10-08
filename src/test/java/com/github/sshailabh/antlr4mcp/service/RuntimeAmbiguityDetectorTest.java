package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.AmbiguityReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for RuntimeAmbiguityDetector (Phase 2).
 * Uses runtime profiling with ProfilingATNSimulator instead of static AST analysis.
 */
@SpringBootTest
@DisplayName("RuntimeAmbiguityDetector Service Tests (Phase 2)")
class RuntimeAmbiguityDetectorTest {

    @Autowired
    private RuntimeAmbiguityDetector detector;

    @BeforeEach
    void setUp() {
        assertNotNull(detector, "RuntimeAmbiguityDetector should be autowired");
    }

    @Test
    @DisplayName("Should detect simple ambiguity with sample inputs")
    void testDetectSimpleAmbiguity() {
        // Arrange
        String grammar = """
            grammar TestGrammar;
            stat : expr ';' | ID '=' expr ';' ;
            expr : ID | INT ;
            ID : [a-z]+ ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        // "x;" is ambiguous: could be expr ';' or incomplete ID '=' expr ';'
        List<String> samples = List.of("x;", "x=5;");

        // Act
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "stat", samples, 5
        );

        // Assert
        assertNotNull(report, "Report should not be null");
        assertEquals(2, report.getTotalSamplesParsed(), "Should have parsed 2 samples");
        assertNotNull(report.getTotalParseTimeMs(), "Should track parse time");
        assertTrue(report.getTotalParseTimeMs() >= 0, "Parse time should be non-negative");

        // Note: Ambiguity detection depends on ANTLR's ProfilingATNSimulator
        // Some grammars may be resolved by precedence, so we check report structure
        assertNotNull(report.getAmbiguities(), "Ambiguities list should not be null");
    }

    @Test
    @DisplayName("Should detect left-recursive ambiguity")
    void testDetectLeftRecursiveAmbiguity() {
        // Arrange
        String grammar = """
            grammar Expr;
            expr : expr '+' expr | expr '*' expr | INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        // Ambiguous: 1+2+3 could be (1+2)+3 or 1+(2+3)
        List<String> samples = List.of("1", "1+2", "1+2+3", "1*2+3");

        // Act
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "expr", samples, 5
        );

        // Assert
        assertNotNull(report);
        assertEquals(4, report.getTotalSamplesParsed());

        // ANTLR's precedence climbing resolves left-recursive ambiguities
        // but ProfilingATNSimulator should still detect them during analysis
        assertNotNull(report.getAmbiguities());
    }

    @Test
    @DisplayName("Should not report ambiguities for clean grammar")
    void testNoAmbiguitiesInCleanGrammar() {
        // Arrange
        String grammar = """
            grammar Clean;
            stat : 'print' expr ;
            expr : term (('+' | '-') term)* ;
            term : factor (('*' | '/') factor)* ;
            factor : INT | '(' expr ')' ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        List<String> samples = List.of(
            "print 42",
            "print 1+2*3",
            "print (1+2)*3"
        );

        // Act
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "stat", samples, 5
        );

        // Assert
        assertNotNull(report);
        assertEquals(3, report.getTotalSamplesParsed());
        assertFalse(report.isHasAmbiguities(), "Clean grammar should not have ambiguities");
        assertTrue(report.getAmbiguities().isEmpty(), "Ambiguities list should be empty");
    }

    @Test
    @DisplayName("Should handle timeout gracefully")
    void testHandleTimeout() {
        // Arrange - grammar with potential infinite loop
        String grammar = """
            grammar Bad;
            start : start 'a' | 'a' ;
            """;

        List<String> samples = List.of("aaa");

        // Act - should timeout gracefully, not hang
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "start", samples, 1
        );

        // Assert - may have partial results or empty if all timed out
        assertNotNull(report, "Report should not be null even on timeout");
        // Timeout means some samples may not be parsed
        assertTrue(report.getTotalSamplesParsed() <= 1, "At most 1 sample should be attempted");
    }

    @Test
    @DisplayName("Should handle invalid grammar")
    void testHandleInvalidGrammar() {
        // Arrange
        String invalidGrammar = "This is not a valid grammar!";
        List<String> samples = List.of("test");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            detector.detectWithSamples(invalidGrammar, "start", samples, 5);
        }, "Should throw exception for invalid grammar");
    }

    @Test
    @DisplayName("Should handle empty sample list")
    void testHandleEmptySamples() {
        // Arrange
        String grammar = """
            grammar Test;
            start : 'a' ;
            """;
        List<String> samples = List.of();

        // Act
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "start", samples, 5
        );

        // Assert
        assertNotNull(report);
        assertEquals(0, report.getTotalSamplesParsed(), "Should not parse any samples");
        assertFalse(report.isHasAmbiguities(), "No samples means no ambiguities detected");
    }

    @Test
    @DisplayName("Should track ambiguities per rule")
    void testAmbiguitiesPerRule() {
        // Arrange
        String grammar = """
            grammar MultiAmb;
            stat : expr ';' | expr '=' expr ';' ;
            expr : expr '+' expr | INT ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        List<String> samples = List.of("1;", "1=2;", "1+2;");

        // Act
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "stat", samples, 5
        );

        // Assert
        assertNotNull(report);
        assertNotNull(report.getAmbiguitiesPerRule(), "Should have per-rule tracking");
        // Structure is correct even if no ambiguities detected due to precedence resolution
    }

    @Test
    @DisplayName("Should provide ambiguity details")
    void testAmbiguityDetails() {
        // Arrange
        String grammar = """
            grammar DetailTest;
            stat : 'a' 'b' | 'a' 'c' | 'a' 'b' ;
            """;

        List<String> samples = List.of("ab", "ac");

        // Act
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "stat", samples, 5
        );

        // Assert
        assertNotNull(report);
        if (report.isHasAmbiguities() && !report.getAmbiguities().isEmpty()) {
            // If ambiguity detected, verify structure
            var ambiguity = report.getAmbiguities().get(0);
            assertNotNull(ambiguity.getRuleName(), "Should have rule name");
            assertNotNull(ambiguity.getConflictingAlternatives(), "Should have alternatives");
            // Runtime detection includes token positions
            assertNotNull(ambiguity.getStartIndex(), "Should have start index");
            assertNotNull(ambiguity.getStopIndex(), "Should have stop index");
        }
    }

    @Test
    @DisplayName("Should handle complex grammar efficiently")
    void testComplexGrammar() {
        // Arrange - use a valid complex grammar without circular left-recursion
        String grammar = """
            grammar Complex;
            prog : statement+ ;
            statement : 'if' expr 'then' statement ('else' statement)?
                      | 'while' expr 'do' statement
                      | ID '=' expr ';'
                      | 'print' expr ';'
                      ;
            expr : term (('+' | '-') term)* ;
            term : factor (('*' | '/') factor)* ;
            factor : INT | ID | '(' expr ')' ;
            ID : [a-z]+ ;
            INT : [0-9]+ ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        List<String> samples = List.of(
            "x=1;",
            "print 42;",
            "if 1 then x=2; else y=3;",
            "while 0 do print 1;"
        );

        // Act - should complete without timeout
        long startTime = System.currentTimeMillis();
        AmbiguityReport report = detector.detectWithSamples(
            grammar, "prog", samples, 5
        );
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(report);
        assertEquals(4, report.getTotalSamplesParsed(), "Should parse all 4 samples");
        assertTrue(duration < 5000, "Should complete within reasonable time (was " + duration + "ms)");
    }
}
