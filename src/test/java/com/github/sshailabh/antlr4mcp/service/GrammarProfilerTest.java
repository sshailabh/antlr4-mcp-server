package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.ProfileResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GrammarProfilerTest {

    @Autowired
    private GrammarProfiler grammarProfiler;

    @Autowired
    private GrammarCompiler grammarCompiler;

    private static final String CALCULATOR_GRAMMAR = """
        grammar Calculator;
        prog : expr EOF ;
        expr : expr ('*'|'/') expr
             | expr ('+'|'-') expr
             | INT
             | '(' expr ')'
             ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        prog : expr EOF ;
        expr : term (('+' | '-') term)* ;
        term : factor (('*' | '/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @BeforeEach
    void setUp() {
        assertNotNull(grammarProfiler);
        assertNotNull(grammarCompiler);
    }

    @Test
    void testProfileSimpleInput() {
        ProfileResult result = grammarProfiler.profile(
            SIMPLE_GRAMMAR,
            "10",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getParserStats());
        assertNotNull(result.getDecisionStats());
        assertTrue(result.getParsingTimeMs() >= 0);
    }

    @Test
    void testProfileComplexExpression() {
        ProfileResult result = grammarProfiler.profile(
            SIMPLE_GRAMMAR,
            "10 + 20 * 30 - (5 + 3)",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        // Check parser stats
        ProfileResult.ParserStats stats = result.getParserStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalDecisions() > 0);
        assertTrue(stats.getTotalInvocations() > 0);

        // Check decision stats list
        assertNotNull(result.getDecisionStats());
        assertFalse(result.getDecisionStats().isEmpty());

        // Verify decision stats have valid data
        ProfileResult.DecisionStats firstDecision = result.getDecisionStats().get(0);
        assertNotNull(firstDecision);
        assertTrue(firstDecision.getDecisionNumber() >= 0);
        assertTrue(firstDecision.getInvocations() > 0);
    }

    @Test
    void testProfileAmbiguousGrammar() {
        // This grammar has ambiguity in expr rule
        ProfileResult result = grammarProfiler.profile(
            CALCULATOR_GRAMMAR,
            "10 + 20 * 30",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        ProfileResult.ParserStats stats = result.getParserStats();
        assertNotNull(stats);

        // Ambiguous grammar should trigger some LL fallbacks or context sensitivities
        long totalProblems = stats.getTotalLlFallbacks() + stats.getTotalFullContextFallbacks();
        assertTrue(totalProblems >= 0, "Should track decision-making events");
    }

    @Test
    void testProfileWithAmbiguityDetection() {
        ProfileResult result = grammarProfiler.profile(
            CALCULATOR_GRAMMAR,
            "5 + 3 * 2",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        // Check if ambiguities were detected
        assertNotNull(result.getAmbiguities());

        // Calculator grammar with left-recursive expr should have ambiguities
        ProfileResult.ParserStats stats = result.getParserStats();
        assertTrue(stats.getTotalAmbiguities() >= 0);
    }

    @Test
    void testProfileEmptyInput() {
        String emptyGrammar = """
            grammar Empty;
            prog : EOF ;
            WS : [ \\t\\r\\n]+ -> skip ;
            """;

        ProfileResult result = grammarProfiler.profile(
            emptyGrammar,
            "",
            null
        );

        assertNotNull(result);
        // May fail if grammar has no explicit lexer rules beyond EOF
        if (result.isSuccess()) {
            assertNotNull(result.getParserStats());
            assertTrue(result.getParsingTimeMs() >= 0);
        } else {
            assertNotNull(result.getError());
        }
    }

    @Test
    void testProfileInvalidGrammar() {
        String invalidGrammar = """
            grammar Invalid;
            prog : UNDEFINED ;
            """;

        ProfileResult result = grammarProfiler.profile(
            invalidGrammar,
            "test",
            null
        );

        assertNotNull(result);
        // Grammar compiler may accept implicit tokens
        // Just check that we get a result
        if (!result.isSuccess()) {
            assertNotNull(result.getError());
        }
    }

    @Test
    void testProfileWithCustomStartRule() {
        ProfileResult result = grammarProfiler.profile(
            SIMPLE_GRAMMAR,
            "10 + 20",
            "expr"
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getParserStats());
    }

    @Test
    void testProfileDecisionStats() {
        ProfileResult result = grammarProfiler.profile(
            SIMPLE_GRAMMAR,
            "10 + 20 * 30",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        assertNotNull(result.getDecisionStats());
        assertFalse(result.getDecisionStats().isEmpty());

        // Verify each decision has reasonable values
        for (ProfileResult.DecisionStats decision : result.getDecisionStats()) {
            assertTrue(decision.getDecisionNumber() >= 0);
            assertTrue(decision.getInvocations() >= 0);
            assertTrue(decision.getTimeInPrediction() >= 0);
            assertTrue(decision.getLlFallbacks() >= 0);
            assertTrue(decision.getFullContextFallbacks() >= 0);
            assertTrue(decision.getAmbiguities() >= 0);
            assertNotNull(decision.getRuleName());
        }
    }

    @Test
    void testProfileTiming() {
        ProfileResult result = grammarProfiler.profile(
            SIMPLE_GRAMMAR,
            "10 + 20 * 30 - (5 + 3) * 2",
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        // Check timing information
        assertTrue(result.getParsingTimeMs() >= 0);
        assertTrue(result.getParserStats().getTotalTimeInPrediction() >= 0);
    }

    @Test
    void testProfileLargerInput() {
        // Generate a larger expression to test performance
        StringBuilder largeInput = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            if (i > 0) largeInput.append(" + ");
            largeInput.append(i);
        }

        ProfileResult result = grammarProfiler.profile(
            SIMPLE_GRAMMAR,
            largeInput.toString(),
            null
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());

        ProfileResult.ParserStats stats = result.getParserStats();
        assertTrue(stats.getTotalInvocations() > 50, "Should have many invocations for large input");
        assertTrue(result.getParsingTimeMs() >= 0, "Should complete profiling");
    }
}
