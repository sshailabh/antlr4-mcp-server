package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.model.DecisionPoint;
import com.github.sshailabh.antlr4mcp.model.DecisionVisualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for DecisionVisualizer.
 * These tests define the expected behavior before implementation.
 */
@SpringBootTest
class DecisionVisualizerTest {

    @Autowired
    private DecisionVisualizer visualizer;

    private static final String SIMPLE_DECISION_GRAMMAR = """
        grammar SimpleDecision;
        expr : INT | FLOAT ;
        INT : [0-9]+ ;
        FLOAT : [0-9]+ '.' [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String MULTIPLE_DECISIONS_GRAMMAR = """
        grammar MultipleDecisions;
        expr : term (('+' | '-') term)* ;
        term : factor (('*' | '/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    private static final String NO_DECISION_GRAMMAR = """
        grammar NoDecision;
        expr : INT ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    @Test
    void testVisualizeSimpleDecision() {
        // Note: Simple token alternations like "INT | FLOAT" are optimized by ANTLR
        // into SetTransitions, not DecisionStates. So this grammar has 0 decisions.
        DecisionVisualization viz = visualizer.visualize(SIMPLE_DECISION_GRAMMAR, "expr");

        assertNotNull(viz, "Visualization should not be null");
        assertEquals("expr", viz.getRuleName(), "Rule name should match");
        assertEquals(0, viz.getTotalDecisions(), "Simple token alternation has no decision points");
        assertTrue(viz.getDecisions().isEmpty(), "Should have no decision points for optimized token alternation");
    }

    @Test
    void testVisualizeMultipleDecisions() {
        DecisionVisualization viz = visualizer.visualize(MULTIPLE_DECISIONS_GRAMMAR, "expr");

        assertNotNull(viz);
        assertEquals("expr", viz.getRuleName());
        assertTrue(viz.getTotalDecisions() > 0, "expr rule should have decisions");

        // expr has decisions for '+' | '-'
        assertFalse(viz.getDecisions().isEmpty());
    }

    @Test
    void testVisualizeTermRule() {
        DecisionVisualization viz = visualizer.visualize(MULTIPLE_DECISIONS_GRAMMAR, "term");

        assertNotNull(viz);
        assertEquals("term", viz.getRuleName());
        assertTrue(viz.getTotalDecisions() > 0, "term rule should have decisions for '*' | '/'");
    }

    @Test
    void testVisualizeFactorRule() {
        DecisionVisualization viz = visualizer.visualize(MULTIPLE_DECISIONS_GRAMMAR, "factor");

        assertNotNull(viz);
        assertEquals("factor", viz.getRuleName());
        assertTrue(viz.getTotalDecisions() > 0, "factor has decision: INT | '(' expr ')'");

        DecisionPoint decision = viz.getDecisions().get(0);
        assertEquals(2, decision.getAlternativeCount(), "factor has 2 alternatives");
    }

    @Test
    void testVisualizeNoDecision() {
        DecisionVisualization viz = visualizer.visualize(NO_DECISION_GRAMMAR, "expr");

        assertNotNull(viz);
        assertEquals("expr", viz.getRuleName());
        // Rule with single alternative may have 0 decisions
        assertTrue(viz.getTotalDecisions() >= 0, "Should handle rules with no decisions");
    }

    @Test
    void testDotFormatValidation() {
        // Use factor rule which has a real decision point
        DecisionVisualization viz = visualizer.visualize(MULTIPLE_DECISIONS_GRAMMAR, "factor");

        assertFalse(viz.getDecisions().isEmpty(), "factor rule should have decision points");
        DecisionPoint decision = viz.getDecisions().get(0);

        String dot = decision.getDotFormat();
        assertNotNull(dot, "DOT format should not be null");
        assertTrue(dot.startsWith("digraph"), "DOT should start with digraph");
        assertTrue(dot.contains("->"), "DOT should contain transitions");
        assertTrue(dot.endsWith("}") || dot.trim().endsWith("}"), "DOT should end with }");
    }

    @Test
    void testDecisionCounts() {
        // Use factor rule which has a real decision point
        DecisionVisualization viz = visualizer.visualize(MULTIPLE_DECISIONS_GRAMMAR, "factor");

        assertFalse(viz.getDecisions().isEmpty(), "factor rule should have decision points");
        DecisionPoint decision = viz.getDecisions().get(0);

        assertTrue(decision.getStateCount() > 0, "Should count states in decision subgraph");
        assertTrue(decision.getTransitionCount() >= decision.getAlternativeCount(),
                  "Should have at least as many transitions as alternatives");
    }

    @Test
    void testInvalidRuleName() {
        assertThrows(IllegalArgumentException.class, () -> {
            visualizer.visualize(SIMPLE_DECISION_GRAMMAR, "nonexistent");
        }, "Should throw for nonexistent rule");
    }

    @Test
    void testNullGrammar() {
        assertThrows(IllegalArgumentException.class, () -> {
            visualizer.visualize(null, "expr");
        }, "Should throw for null grammar");
    }

    @Test
    void testEmptyGrammar() {
        assertThrows(IllegalArgumentException.class, () -> {
            visualizer.visualize("", "expr");
        }, "Should throw for empty grammar");
    }

    @Test
    void testDecisionNumbering() {
        DecisionVisualization viz = visualizer.visualize(MULTIPLE_DECISIONS_GRAMMAR, "expr");

        assertFalse(viz.getDecisions().isEmpty());

        // Decision numbers should be unique
        long uniqueDecisions = viz.getDecisions().stream()
                .map(DecisionPoint::getDecisionNumber)
                .distinct()
                .count();

        assertEquals(viz.getDecisions().size(), uniqueDecisions,
                    "Each decision should have a unique decision number");
    }
}
