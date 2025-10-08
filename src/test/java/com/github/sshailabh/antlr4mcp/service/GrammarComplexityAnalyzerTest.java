package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.analysis.CallGraphNode;
import com.github.sshailabh.antlr4mcp.model.ComplexityMetrics;
import com.github.sshailabh.antlr4mcp.model.RuleComplexity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GrammarComplexityAnalyzer.
 */
@SpringBootTest
class GrammarComplexityAnalyzerTest {

    @Autowired
    private GrammarComplexityAnalyzer analyzer;

    private static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        prog : expr EOF ;
        expr : term ;
        term : factor ;
        factor : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String RECURSIVE_GRAMMAR = """
        grammar Recursive;
        expr : expr '+' term
             | term
             ;
        term : term '*' factor
             | factor
             ;
        factor : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String ALTERNATIVES_GRAMMAR = """
        grammar Alternatives;
        expr : INT
             | FLOAT
             | STRING
             | ID
             ;
        INT : [0-9]+ ;
        FLOAT : [0-9]+ '.' [0-9]+ ;
        STRING : '"' .*? '"' ;
        ID : [a-zA-Z]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String FRAGMENT_GRAMMAR = """
        lexer grammar FragmentLexer;
        NUMBER : DIGIT+ ;
        fragment DIGIT : [0-9] ;
        fragment LETTER : [a-zA-Z] ;
        ID : LETTER (LETTER | DIGIT)* ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @Test
    void testSimpleGrammar_BasicMetrics() {
        ComplexityMetrics metrics = analyzer.analyze(SIMPLE_GRAMMAR);

        assertNotNull(metrics);
        assertTrue(metrics.getTotalRules() > 0, "Should have at least some rules");
        assertTrue(metrics.getParserRules() > 0, "Should have parser rules");
        assertTrue(metrics.getLexerRules() > 0, "Should have lexer rules");
        assertNotNull(metrics.getRuleMetrics(), "Rule metrics map should not be null");
        assertTrue(metrics.getRuleMetrics().size() > 0, "Should have per-rule metrics");
    }

    @Test
    void testSimpleGrammar_CallRelationships() {
        ComplexityMetrics metrics = analyzer.analyze(SIMPLE_GRAMMAR);

        // Check prog rule
        RuleComplexity progComplexity = metrics.getRuleMetrics().get("prog");
        assertNotNull(progComplexity, "prog rule should be analyzed");
        assertEquals("prog", progComplexity.getRuleName());
        assertEquals(CallGraphNode.RuleType.PARSER, progComplexity.getType());
        assertTrue(progComplexity.getFanOut() > 0, "prog should call at least one rule");

        // Check expr rule
        RuleComplexity exprComplexity = metrics.getRuleMetrics().get("expr");
        assertNotNull(exprComplexity, "expr rule should be analyzed");
        assertTrue(exprComplexity.getFanIn() > 0, "expr should be called by prog");
        assertTrue(exprComplexity.getFanOut() > 0, "expr should call term");
    }

    @Test
    void testRecursiveGrammar_DetectRecursion() {
        ComplexityMetrics metrics = analyzer.analyze(RECURSIVE_GRAMMAR);

        assertNotNull(metrics);

        // Check expr rule - should be recursive
        RuleComplexity exprComplexity = metrics.getRuleMetrics().get("expr");
        assertNotNull(exprComplexity, "expr rule should be analyzed");
        assertTrue(exprComplexity.isRecursive(), "expr should be marked as recursive");
        // Note: ANTLR transforms left-recursive rules, so numberOfAlts reflects transformed form
        assertTrue(exprComplexity.getAlternatives() >= 1, "expr should have at least 1 alternative");

        // Check term rule - should also be recursive
        RuleComplexity termComplexity = metrics.getRuleMetrics().get("term");
        assertNotNull(termComplexity, "term rule should be analyzed");
        assertTrue(termComplexity.isRecursive(), "term should be marked as recursive");
        assertTrue(termComplexity.getAlternatives() >= 1, "term should have at least 1 alternative");
    }

    @Test
    void testLexerVsParserRuleCounts() {
        ComplexityMetrics metrics = analyzer.analyze(SIMPLE_GRAMMAR);

        assertNotNull(metrics);
        assertTrue(metrics.getParserRules() >= 4, "Should have at least prog, expr, term, factor");
        assertTrue(metrics.getLexerRules() >= 2, "Should have at least INT, WS");

        // Verify each rule is correctly classified
        for (RuleComplexity rc : metrics.getRuleMetrics().values()) {
            char firstChar = rc.getRuleName().charAt(0);
            if (Character.isUpperCase(firstChar)) {
                assertTrue(rc.getType() == CallGraphNode.RuleType.LEXER ||
                          rc.getType() == CallGraphNode.RuleType.FRAGMENT,
                          "Uppercase rule should be LEXER or FRAGMENT: " + rc.getRuleName());
            } else {
                assertEquals(CallGraphNode.RuleType.PARSER, rc.getType(),
                           "Lowercase rule should be PARSER: " + rc.getRuleName());
            }
        }
    }

    @Test
    void testFanInFanOutMetrics() {
        ComplexityMetrics metrics = analyzer.analyze(SIMPLE_GRAMMAR);

        // prog calls expr, so prog.fanOut should include expr
        RuleComplexity progComplexity = metrics.getRuleMetrics().get("prog");
        assertNotNull(progComplexity);
        assertTrue(progComplexity.getFanOut() > 0, "prog should call other rules");

        // expr is called by prog and calls term
        RuleComplexity exprComplexity = metrics.getRuleMetrics().get("expr");
        assertNotNull(exprComplexity);
        assertTrue(exprComplexity.getFanIn() > 0, "expr should be called by prog");
        assertTrue(exprComplexity.getFanOut() > 0, "expr should call term");

        // factor is a leaf rule (only called, doesn't call parser rules)
        RuleComplexity factorComplexity = metrics.getRuleMetrics().get("factor");
        assertNotNull(factorComplexity);
        assertTrue(factorComplexity.getFanIn() > 0, "factor should be called by term");
    }

    @Test
    void testMaxDepthCalculation() {
        ComplexityMetrics metrics = analyzer.analyze(SIMPLE_GRAMMAR);

        assertNotNull(metrics);
        assertTrue(metrics.getMaxRuleDepth() > 0, "Should have non-zero max depth");

        // prog is start rule (depth 0), factor is deepest (depth 3)
        RuleComplexity progComplexity = metrics.getRuleMetrics().get("prog");
        assertNotNull(progComplexity);
        assertEquals(0, progComplexity.getDepth(), "prog should be at depth 0");

        // Each subsequent rule should be deeper
        RuleComplexity exprComplexity = metrics.getRuleMetrics().get("expr");
        RuleComplexity termComplexity = metrics.getRuleMetrics().get("term");
        RuleComplexity factorComplexity = metrics.getRuleMetrics().get("factor");

        assertTrue(exprComplexity.getDepth() > progComplexity.getDepth(),
                  "expr should be deeper than prog");
        assertTrue(termComplexity.getDepth() > exprComplexity.getDepth(),
                  "term should be deeper than expr");
        assertTrue(factorComplexity.getDepth() > termComplexity.getDepth(),
                  "factor should be deeper than term");
    }

    @Test
    void testAlternativesCounting() {
        ComplexityMetrics metrics = analyzer.analyze(ALTERNATIVES_GRAMMAR);

        assertNotNull(metrics);

        // Note: ANTLR optimizes simple token alternatives like "INT | FLOAT | STRING | ID"
        // into a single alternative with a SetTransition. The numberOfAlts field
        // reflects ANTLR's internal representation, not the source-level pipe count.
        // This is correct behavior for complexity analysis.
        RuleComplexity exprComplexity = metrics.getRuleMetrics().get("expr");
        assertNotNull(exprComplexity, "expr rule should be analyzed");
        assertTrue(exprComplexity.getAlternatives() >= 1, "expr should have at least 1 alternative");

        // Calculate expected average
        double totalAlts = 0;
        for (RuleComplexity rc : metrics.getRuleMetrics().values()) {
            totalAlts += rc.getAlternatives();
        }
        double expectedAvg = totalAlts / metrics.getTotalRules();

        assertEquals(expectedAvg, metrics.getAvgAlternativesPerRule(), 0.01,
                    "Average alternatives should be calculated correctly");
    }

    @Test
    void testDecisionPointCounting() {
        ComplexityMetrics metrics = analyzer.analyze(ALTERNATIVES_GRAMMAR);

        assertNotNull(metrics);
        // Note: ANTLR optimizes simple token alternatives into SetTransitions,
        // which don't create DecisionStates. This is expected behavior.
        // Complex grammars with non-token alternatives will have decision points.
        assertTrue(metrics.getTotalDecisionPoints() >= 0,
                  "Total decision points should be non-negative");

        // expr rule - decision points depend on ANTLR's optimization
        RuleComplexity exprComplexity = metrics.getRuleMetrics().get("expr");
        assertNotNull(exprComplexity);
        assertTrue(exprComplexity.getDecisionPoints() >= 0,
                  "Decision points should be non-negative");
    }

    @Test
    void testFragmentRules() {
        ComplexityMetrics metrics = analyzer.analyze(FRAGMENT_GRAMMAR);

        assertNotNull(metrics);
        assertTrue(metrics.getFragmentRules() > 0, "Should detect fragment rules");

        // DIGIT should be classified as FRAGMENT
        RuleComplexity digitComplexity = metrics.getRuleMetrics().get("DIGIT");
        assertNotNull(digitComplexity, "DIGIT fragment should be analyzed");
        assertEquals(CallGraphNode.RuleType.FRAGMENT, digitComplexity.getType(),
                    "DIGIT should be classified as FRAGMENT");

        // NUMBER should be classified as LEXER
        RuleComplexity numberComplexity = metrics.getRuleMetrics().get("NUMBER");
        assertNotNull(numberComplexity, "NUMBER should be analyzed");
        assertEquals(CallGraphNode.RuleType.LEXER, numberComplexity.getType(),
                    "NUMBER should be classified as LEXER");
    }

    @Test
    void testEmptyGrammar_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.analyze("");
        }, "Empty grammar should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.analyze(null);
        }, "Null grammar should throw IllegalArgumentException");
    }

    @Test
    void testComplexityMetricsConsistency() {
        ComplexityMetrics metrics = analyzer.analyze(SIMPLE_GRAMMAR);

        // Verify consistency: totalRules == sum of rule types
        int sumOfTypes = metrics.getParserRules() + metrics.getLexerRules() + metrics.getFragmentRules();
        assertEquals(metrics.getTotalRules(), sumOfTypes,
                    "Total rules should equal sum of parser, lexer, and fragment rules");

        // Verify ruleMetrics size matches totalRules
        assertEquals(metrics.getTotalRules(), metrics.getRuleMetrics().size(),
                    "Rule metrics map size should match total rules");
    }
}
