package com.github.sshailabh.antlr4mcp.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EmbeddedCodeAnalyzer - lightweight detection of embedded code in grammars.
 * Focuses on boolean checks used by GrammarInterpreter for warning generation.
 */
@SpringBootTest
@DisplayName("Embedded Code Analyzer Tests")
class EmbeddedCodeAnalyzerTest {

    @Autowired
    private EmbeddedCodeAnalyzer analyzer;

    private String grammarWithActions;
    private String grammarWithPredicates;
    private String grammarWithBoth;
    private String cleanGrammar;

    @BeforeEach
    void setUp() {
        grammarWithActions = """
            grammar WithActions;

            @header {
                import java.util.*;
            }

            @members {
                private int count = 0;
            }

            expr : INT { System.out.println($INT.text); } ;
            INT : [0-9]+ ;
            """;

        grammarWithPredicates = """
            grammar WithPredicates;

            expr : { count > 0 }? INT
                 | { count < 10 }? ID
                 ;

            INT : [0-9]+ ;
            ID : [a-zA-Z]+ ;
            """;

        grammarWithBoth = """
            grammar WithBoth;

            @header { import java.util.*; }

            expr : { count > 0 }? INT { count++; }
                 | ID { names.add($ID.text); }
                 ;

            INT : [0-9]+ ;
            ID : [a-zA-Z]+ ;
            """;

        cleanGrammar = """
            grammar Clean;

            expr : term (('+' | '-') term)* ;
            term : INT ;
            INT : [0-9]+ ;
            """;
    }

    @Test
    @DisplayName("Detect @header and @members actions")
    void testDetectHeaderAndMembersActions() {
        assertTrue(analyzer.hasActions(grammarWithActions),
            "Should detect @header and @members actions");
        assertFalse(analyzer.hasPredicates(grammarWithActions),
            "Should not detect predicates");
        assertTrue(analyzer.hasEmbeddedCode(grammarWithActions),
            "Should detect embedded code");
    }

    @Test
    @DisplayName("Detect inline actions")
    void testDetectInlineActions() {
        String inlineActionGrammar = """
            grammar InlineActions;

            expr : INT { count++; } '+' INT ;
            INT : [0-9]+ ;
            """;

        assertTrue(analyzer.hasActions(inlineActionGrammar),
            "Should detect inline actions");
        assertTrue(analyzer.hasEmbeddedCode(inlineActionGrammar),
            "Should detect embedded code");
    }

    @Test
    @DisplayName("Detect semantic predicates")
    void testDetectPredicates() {
        assertTrue(analyzer.hasPredicates(grammarWithPredicates),
            "Should detect semantic predicates");
        assertFalse(analyzer.hasActions(grammarWithPredicates),
            "Should not detect actions");
        assertTrue(analyzer.hasEmbeddedCode(grammarWithPredicates),
            "Should detect embedded code");
    }

    @Test
    @DisplayName("Detect both actions and predicates")
    void testDetectBoth() {
        assertTrue(analyzer.hasActions(grammarWithBoth),
            "Should detect actions");
        assertTrue(analyzer.hasPredicates(grammarWithBoth),
            "Should detect predicates");
        assertTrue(analyzer.hasEmbeddedCode(grammarWithBoth),
            "Should detect embedded code");
    }

    @Test
    @DisplayName("Handle clean grammar without embedded code")
    void testCleanGrammar() {
        assertFalse(analyzer.hasActions(cleanGrammar),
            "Should not detect actions");
        assertFalse(analyzer.hasPredicates(cleanGrammar),
            "Should not detect predicates");
        assertFalse(analyzer.hasEmbeddedCode(cleanGrammar),
            "Should not detect embedded code");
    }

    @Test
    @DisplayName("Detect @init and @after actions")
    void testDetectInitAndAfterActions() {
        String initAfterGrammar = """
            grammar InitAfter;

            @init { int x = 0; }
            @after { cleanup(); }

            expr : INT ;
            INT : [0-9]+ ;
            """;

        assertTrue(analyzer.hasActions(initAfterGrammar),
            "Should detect @init and @after actions");
    }

    @Test
    @DisplayName("Handle complex nested predicates")
    void testNestedPredicates() {
        String nestedGrammar = """
            grammar Nested;

            expr : { x > 0 && y < 10 }? INT
                 | { z == 0 }? ID
                 ;

            INT : [0-9]+ ;
            ID : [a-zA-Z]+ ;
            """;

        assertTrue(analyzer.hasPredicates(nestedGrammar),
            "Should detect nested predicates");
    }

    @Test
    @DisplayName("Distinguish between predicates and actions")
    void testDistinguishPredicatesAndActions() {
        String predicateOnly = "{ x > 0 }?";
        String actionOnly = "{ x++; }";

        assertTrue(analyzer.hasPredicates(predicateOnly),
            "Should detect predicate");
        assertFalse(analyzer.hasActions(predicateOnly),
            "Should not detect action in predicate");

        assertTrue(analyzer.hasActions(actionOnly),
            "Should detect action");
        assertFalse(analyzer.hasPredicates(actionOnly),
            "Should not detect predicate in action");
    }

    @Test
    @DisplayName("Performance: Check large grammar efficiently")
    void testPerformanceOnLargeGrammar() {
        // Build a large grammar with many rules
        StringBuilder largeGrammar = new StringBuilder("grammar Large;\n\n");
        for (int i = 0; i < 1000; i++) {
            largeGrammar.append("rule").append(i).append(" : INT ;\n");
        }
        largeGrammar.append("INT : [0-9]+ ;");

        long start = System.nanoTime();
        boolean hasCode = analyzer.hasEmbeddedCode(largeGrammar.toString());
        long duration = System.nanoTime() - start;

        assertFalse(hasCode, "Large clean grammar should have no embedded code");
        assertTrue(duration < 100_000_000, // 100ms
            "Should check large grammar in under 100ms");
    }

    @Test
    @DisplayName("Handle edge case: empty grammar")
    void testEmptyGrammar() {
        assertFalse(analyzer.hasActions(""),
            "Empty grammar should have no actions");
        assertFalse(analyzer.hasPredicates(""),
            "Empty grammar should have no predicates");
        assertFalse(analyzer.hasEmbeddedCode(""),
            "Empty grammar should have no embedded code");
    }

    @Test
    @DisplayName("Handle edge case: curly braces in string literals")
    void testCurlyBracesInStrings() {
        String grammarWithStrings = """
            grammar Strings;

            expr : STRING ;
            STRING : '"' (~["])* '"' ;
            """;

        // Note: This is a simplified test. Real grammars might have {'}' in strings,
        // but our detector is conservative and may flag them.
        assertFalse(analyzer.hasActions(grammarWithStrings),
            "Should not detect actions in clean grammar");
    }
}
