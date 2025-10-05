package com.github.sshailabh.antlr4mcp.analysis;

import com.github.sshailabh.antlr4mcp.analysis.EmbeddedCodeAnalyzer.EmbeddedCodeReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Embedded Code Analyzer Tests")
class EmbeddedCodeAnalyzerTest {

    @Autowired
    private EmbeddedCodeAnalyzer analyzer;

    private String grammarWithJavaCode;
    private String grammarWithPythonCode;
    private String grammarWithJavaScriptCode;
    private String grammarWithPredicates;
    private String grammarWithInlineActions;
    private String cleanGrammar;

    @BeforeEach
    void setUp() {
        grammarWithJavaCode = """
            grammar JavaGrammar;

            @header {
                import java.util.*;
                import java.io.*;
            }

            @members {
                private int count = 0;

                public void incrementCount() {
                    count++;
                }
            }

            expr : INT { System.out.println("Found: " + $INT.text); } ;

            INT : [0-9]+ ;
            """;

        grammarWithPythonCode = """
            grammar PythonGrammar;

            @header {
                import sys
            }

            @members {
                def process(self, value):
                    print(f"Processing: {value}")
            }

            expr : INT { self.process($INT.text) } ;

            INT : [0-9]+ ;
            """;

        grammarWithJavaScriptCode = """
            grammar JSGrammar;

            @header {
                const util = require('util');
            }

            @members {
                let count = 0;

                const increment = () => {
                    count++;
                    console.log(`Count: ${count}`);
                };
            }

            expr : INT { console.log('Found: ' + $INT.text); } ;

            INT : [0-9]+ ;
            """;

        grammarWithPredicates = """
            grammar Predicates;

            expr : { count > 0 }? INT
                 | { count < 10 }? ID
                 ;

            INT : [0-9]+ ;
            ID : [a-zA-Z]+ ;
            """;

        grammarWithInlineActions = """
            grammar InlineActions;

            expr : INT { count++; } '+' INT { count++; }
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
    @DisplayName("Detect Java embedded code")
    void testDetectJavaCode() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithJavaCode);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals("java", report.getDetectedLanguage(), "Should detect Java");
        assertTrue(report.getActionCount() > 0, "Should have actions");
        assertTrue(report.getInlineActionCount() > 0, "Should have inline actions");

        // Check specific actions
        assertTrue(report.getActions().containsKey("header"), "Should have header action");
        assertTrue(report.getActions().containsKey("members"), "Should have members action");
    }

    @Test
    @DisplayName("Detect Python embedded code")
    void testDetectPythonCode() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithPythonCode);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals("python", report.getDetectedLanguage(), "Should detect Python");
        assertTrue(report.getActionCount() > 0, "Should have actions");
    }

    @Test
    @DisplayName("Detect JavaScript embedded code")
    void testDetectJavaScriptCode() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithJavaScriptCode);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals("javascript", report.getDetectedLanguage(), "Should detect JavaScript");
        assertTrue(report.getActionCount() > 0, "Should have actions");
    }

    @Test
    @DisplayName("Detect semantic predicates")
    void testDetectPredicates() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithPredicates);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals(2, report.getPredicateCount(), "Should have 2 predicates");
        assertTrue(report.getPredicates().size() == 2, "Should have 2 predicate entries");
    }

    @Test
    @DisplayName("Detect inline actions")
    void testDetectInlineActions() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithInlineActions);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertTrue(report.getInlineActionCount() > 0, "Should have inline actions");
    }

    @Test
    @DisplayName("Handle clean grammar without embedded code")
    void testCleanGrammar() {
        EmbeddedCodeReport report = analyzer.analyze(cleanGrammar);

        assertFalse(report.hasEmbeddedCode(), "Should not detect embedded code");
        assertEquals(0, report.getActionCount(), "Should have no actions");
        assertEquals(0, report.getPredicateCount(), "Should have no predicates");
        assertEquals(0, report.getInlineActionCount(), "Should have no inline actions");
    }

    @Test
    @DisplayName("Strip embedded code from grammar")
    void testStripEmbeddedCode() {
        String stripped = analyzer.stripEmbeddedCode(grammarWithJavaCode);

        assertNotNull(stripped, "Stripped grammar should not be null");
        assertFalse(stripped.contains("@header"), "Should remove @header");
        assertFalse(stripped.contains("@members"), "Should remove @members");
        assertFalse(stripped.contains("System.out.println"), "Should remove inline actions");

        // Should still contain grammar structure
        assertTrue(stripped.contains("grammar JavaGrammar"), "Should keep grammar declaration");
        assertTrue(stripped.contains("expr"), "Should keep rules");
        assertTrue(stripped.contains("INT"), "Should keep token definitions");
    }

    @Test
    @DisplayName("Verify hasEmbeddedCode method")
    void testHasEmbeddedCode() {
        assertTrue(analyzer.hasEmbeddedCode(grammarWithJavaCode),
            "Should detect Java code");
        assertTrue(analyzer.hasEmbeddedCode(grammarWithPythonCode),
            "Should detect Python code");
        assertTrue(analyzer.hasEmbeddedCode(grammarWithPredicates),
            "Should detect predicates");
        assertFalse(analyzer.hasEmbeddedCode(cleanGrammar),
            "Should not detect code in clean grammar");
    }

    @Test
    @DisplayName("Extract code with correct positions")
    void testCodePositions() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithJavaCode);

        // Check that positions are recorded
        report.getActions().values().forEach(fragments -> {
            fragments.forEach(fragment -> {
                assertTrue(fragment.getPosition() >= 0,
                    "Position should be non-negative");
                assertNotNull(fragment.getCode(),
                    "Code should not be null");
            });
        });
    }

    @Test
    @DisplayName("Count actions correctly")
    void testActionCounting() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithJavaCode);

        int totalActions = report.getActionCount();
        int headerActions = report.getActions().getOrDefault("header", java.util.Collections.emptyList()).size();
        int memberActions = report.getActions().getOrDefault("members", java.util.Collections.emptyList()).size();

        assertTrue(totalActions >= 2, "Should have at least 2 actions (header + members)");
        assertEquals(1, headerActions, "Should have 1 header action");
        assertEquals(1, memberActions, "Should have 1 members action");
    }

    @Test
    @DisplayName("Generate report map correctly")
    void testReportMap() {
        EmbeddedCodeReport report = analyzer.analyze(grammarWithJavaCode);
        java.util.Map<String, Object> map = report.toMap();

        assertNotNull(map, "Map should not be null");
        assertTrue((Boolean) map.get("hasEmbeddedCode"), "Should indicate embedded code present");
        assertEquals("java", map.get("detectedLanguage"), "Should have detected language");
        assertTrue((Integer) map.get("actionCount") > 0, "Should have action count");
        assertTrue(map.containsKey("actions"), "Should contain actions");
    }

    @Test
    @DisplayName("Handle mixed action types")
    void testMixedActionTypes() {
        String mixedGrammar = """
            grammar Mixed;

            @header { import java.util.*; }

            @init { int x = 0; }

            expr : { x > 0 }? INT { x++; }
                 | ID
                 ;

            INT : [0-9]+ ;
            ID : [a-zA-Z]+ ;
            """;

        EmbeddedCodeReport report = analyzer.analyze(mixedGrammar);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertTrue(report.getActionCount() >= 2, "Should have header and init actions");
        assertTrue(report.getPredicateCount() >= 1, "Should have predicate");
        assertTrue(report.getInlineActionCount() >= 1, "Should have inline action");
    }

    @Test
    @DisplayName("Detect C++ code patterns")
    void testDetectCppCode() {
        String cppGrammar = """
            grammar CppGrammar;

            @header {
                #include <iostream>
                #include <vector>
            }

            @members {
                std::vector<int> values;
            }

            expr : INT { std::cout << "Value: " << $INT.text << std::endl; } ;

            INT : [0-9]+ ;
            """;

        EmbeddedCodeReport report = analyzer.analyze(cppGrammar);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals("cpp", report.getDetectedLanguage(), "Should detect C++");
    }

    @Test
    @DisplayName("Detect C# code patterns")
    void testDetectCSharpCode() {
        String csharpGrammar = """
            grammar CSharpGrammar;

            @header {
                using System;
                using System.Collections.Generic;
            }

            @members {
                private List<int> values = new List<int>();
            }

            expr : INT { Console.WriteLine("Value: " + $INT.text); } ;

            INT : [0-9]+ ;
            """;

        EmbeddedCodeReport report = analyzer.analyze(csharpGrammar);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals("csharp", report.getDetectedLanguage(), "Should detect C#");
    }

    @Test
    @DisplayName("Handle unknown language")
    void testUnknownLanguage() {
        String unknownGrammar = """
            grammar Unknown;

            @members {
                some_code_here
            }

            expr : INT ;
            INT : [0-9]+ ;
            """;

        EmbeddedCodeReport report = analyzer.analyze(unknownGrammar);

        assertTrue(report.hasEmbeddedCode(), "Should detect embedded code");
        assertEquals("unknown", report.getDetectedLanguage(),
            "Should classify as unknown language");
    }
}
