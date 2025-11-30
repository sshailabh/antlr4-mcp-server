package com.github.sshailabh.antlr4mcp.support;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static com.github.sshailabh.antlr4mcp.support.GrammarFixtures.*;

/**
 * Centralized test scenarios for parameterized tests.
 *
 * Provides reusable test case streams for common testing patterns:
 * - Valid grammar validation scenarios
 * - Invalid grammar scenarios with expected errors
 * - Parsing scenarios with inputs
 * - Ambiguity detection scenarios
 * - Edge case scenarios
 */
public final class TestScenarios {

    private TestScenarios() {
        // Utility class
    }

    // ========== Validation Scenarios ==========

    /**
     * Valid grammars with expected metadata
     * Arguments: description, grammar, expectedName, expectedParserRules, expectedLexerRules
     */
    public static Stream<Arguments> validGrammars() {
        return Stream.of(
            Arguments.of("Simple hello grammar", SIMPLE_HELLO, "Hello", 1, 0),
            Arguments.of("Hello world with alternatives", SIMPLE_HELLO_WORLD, "HelloWorld", 1, 2),
            Arguments.of("Simple calculator", SIMPLE_CALC, "SimpleCalc", 3, 2),
            Arguments.of("Minimal grammar", MINIMAL_GRAMMAR, "Minimal", 1, 0),
            Arguments.of("Empty rule grammar", EMPTY_RULE_GRAMMAR, "EmptyRule", 1, 0),
            Arguments.of("JSON grammar", JSON_INLINE, "Json", 5, 3),
            Arguments.of("Precedence calculator", PRECEDENCE_CALC, "PrecedenceCalc", 2, 2)
        );
    }

    /**
     * Invalid grammars with expected error patterns
     * Arguments: description, grammar, expectedErrorPattern
     */
    public static Stream<Arguments> invalidGrammars() {
        return Stream.of(
            Arguments.of("Undefined rule reference", UNDEFINED_RULE, "undefined"),
            Arguments.of("Missing semicolon", MISSING_SEMICOLON, "missing"),
            Arguments.of("No grammar declaration", NO_GRAMMAR_DECLARATION, "grammar"),
            Arguments.of("Empty grammar", EMPTY_GRAMMAR, "empty")
        );
    }

    /**
     * Lexer-only grammars
     * Arguments: description, grammar, expectedName, expectedLexerRules
     */
    public static Stream<Arguments> lexerGrammars() {
        return Stream.of(
            Arguments.of("Common lexer", COMMON_LEXER, "CommonLexer", 5)
        );
    }

    // ========== Parsing Scenarios ==========

    /**
     * Successful parsing scenarios
     * Arguments: description, grammar, input, startRule, expectedInTree
     */
    public static Stream<Arguments> successfulParsing() {
        return Stream.of(
            Arguments.of("Hello parsing", SIMPLE_HELLO, Inputs.HELLO, "start", "hello"),
            Arguments.of("Hello world parsing", SIMPLE_HELLO_WORLD, Inputs.HELLO_WORLD, "start", "WORLD"),
            Arguments.of("Simple expression", SIMPLE_CALC, Inputs.SIMPLE_EXPR, "expr", "INT"),
            Arguments.of("Complex expression", SIMPLE_CALC, Inputs.COMPLEX_EXPR, "expr", "term"),
            Arguments.of("Nested expression", SIMPLE_CALC, Inputs.NESTED_EXPR, "expr", "factor"),
            Arguments.of("Simple JSON", JSON_INLINE, Inputs.SIMPLE_JSON, "json", "object"),
            Arguments.of("Complex JSON", JSON_INLINE, Inputs.COMPLEX_JSON, "json", "array")
        );
    }

    /**
     * Failed parsing scenarios
     * Arguments: description, grammar, input, startRule, expectedErrorPattern
     */
    public static Stream<Arguments> failedParsing() {
        return Stream.of(
            Arguments.of("Invalid input", SIMPLE_HELLO, Inputs.INVALID_INPUT, "start", "extraneous"),
            Arguments.of("Empty input on non-empty grammar", SIMPLE_HELLO, Inputs.EMPTY_INPUT, "start", "mismatched"),
            Arguments.of("Wrong JSON syntax", JSON_INLINE, "{invalid}", "json", "mismatched")
        );
    }

    // ========== Ambiguity Scenarios ==========

    /**
     * Grammars with known ambiguities
     * Arguments: description, grammar, sampleInput, expectedAmbiguousRule
     */
    public static Stream<Arguments> ambiguousGrammars() {
        return Stream.of(
            Arguments.of("Dangling else", DANGLING_ELSE, "if x then if y then z else w", "stat"),
            Arguments.of("Ambiguous alternatives", AMBIGUOUS_ALTERNATIVES, "a b", "rule"),
            Arguments.of("Left recursive calc", LEFT_RECURSIVE_CALC, "1 + 2 * 3", "expr")
        );
    }

    /**
     * Grammars without ambiguities
     * Arguments: description, grammar, sampleInput
     */
    public static Stream<Arguments> unambiguousGrammars() {
        return Stream.of(
            Arguments.of("Simple hello", SIMPLE_HELLO, Inputs.HELLO),
            Arguments.of("Simple calc", SIMPLE_CALC, Inputs.SIMPLE_EXPR),
            Arguments.of("JSON", JSON_INLINE, Inputs.SIMPLE_JSON)
        );
    }

    // ========== Grammar Building Scenarios ==========

    /**
     * Dynamically built grammars for testing grammar builder
     * Arguments: description, builderConfig, expectedName, expectedRules
     */
    public static Stream<Arguments> dynamicGrammars() {
        return Stream.of(
            Arguments.of(
                "Simple rule grammar",
                (GrammarBuilderConfig) builder -> builder
                    .named("Test")
                    .withStartRule("'hello'"),
                "Test", 1
            ),
            Arguments.of(
                "Multiple rules grammar",
                (GrammarBuilderConfig) builder -> builder
                    .named("Multi")
                    .withRule("start", "expr")
                    .withRule("expr", "INT")
                    .withLexerRule("INT", "[0-9]+")
                    .withWhitespace(),
                "Multi", 2
            )
        );
    }

    @FunctionalInterface
    public interface GrammarBuilderConfig {
        GrammarFixtures.GrammarBuilder configure(GrammarFixtures.GrammarBuilder builder);
    }

    // ========== Edge Case Scenarios ==========

    /**
     * Edge cases and stress tests
     * Arguments: description, testInput
     */
    public static Stream<Arguments> edgeCases() {
        return Stream.of(
            Arguments.of("Large expression", Inputs.LARGE_EXPR),
            Arguments.of("Deeply nested structure", "((((1))))"),
            Arguments.of("Unicode characters", "hello 世界"),
            Arguments.of("Special characters", "!@#$%^&*()")
        );
    }

    // ========== Security Scenarios ==========

    /**
     * Security test scenarios
     * Arguments: description, input, shouldBlock
     */
    public static Stream<Arguments> securityTests() {
        return Stream.of(
            Arguments.of("SQL injection attempt", "'; DROP TABLE--", true),
            Arguments.of("Path traversal", "../../../etc/passwd", true),
            Arguments.of("Command injection", "$(rm -rf /)", true),
            Arguments.of("Script injection", "<script>alert(1)</script>", true),
            Arguments.of("Normal input", "hello world", false)
        );
    }

    // ========== Format Scenarios ==========

    /**
     * Different output format scenarios
     * Arguments: format, fileExtension, contentPattern
     */
    public static Stream<Arguments> outputFormats() {
        return Stream.of(
            Arguments.of("json", ".json", "\\{"),
            Arguments.of("svg", ".svg", "<svg"),
            Arguments.of("dot", ".dot", "digraph"),
            Arguments.of("lisp", ".lisp", "\\("),
            Arguments.of("text", ".txt", ".*")
        );
    }

    // ========== Grammar Complexity Scenarios ==========

    /**
     * Grammar complexity test cases
     * Arguments: description, grammar, expectedComplexityRange
     */
    public static Stream<Arguments> complexityTests() {
        return Stream.of(
            Arguments.of("Minimal", MINIMAL_GRAMMAR, ComplexityRange.LOW),
            Arguments.of("Simple", SIMPLE_HELLO, ComplexityRange.LOW),
            Arguments.of("Medium", SIMPLE_CALC, ComplexityRange.MEDIUM),
            Arguments.of("Complex", JSON_INLINE, ComplexityRange.HIGH),
            Arguments.of("Very complex", PRECEDENCE_CALC, ComplexityRange.HIGH)
        );
    }

    public enum ComplexityRange {
        LOW(0, 5),
        MEDIUM(5, 15),
        HIGH(15, 100);

        public final int min;
        public final int max;

        ComplexityRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public boolean contains(int value) {
            return value >= min && value < max;
        }
    }

    // ========== Left Recursion Scenarios ==========

    /**
     * Left recursion detection scenarios
     * Arguments: description, grammar, hasLeftRecursion, recursiveRule
     */
    public static Stream<Arguments> leftRecursionTests() {
        return Stream.of(
            Arguments.of("Direct left recursion", LEFT_RECURSIVE_CALC, true, "expr"),
            Arguments.of("No left recursion", SIMPLE_CALC, false, null),
            Arguments.of("Precedence with left recursion", PRECEDENCE_CALC, true, "expr")
        );
    }

    // ========== Integration Test Scenarios ==========

    /**
     * End-to-end integration scenarios combining multiple operations
     * Arguments: description, grammar, input, operations
     */
    public static Stream<Arguments> integrationScenarios() {
        return Stream.of(
            Arguments.of(
                "Full workflow - validate, parse, analyze",
                SIMPLE_CALC,
                Inputs.SIMPLE_EXPR,
                new String[]{"validate", "parse", "analyze_complexity"}
            ),
            Arguments.of(
                "Complex workflow - JSON parsing and visualization",
                JSON_INLINE,
                Inputs.COMPLEX_JSON,
                new String[]{"validate", "parse", "visualize_tree", "detect_ambiguity"}
            )
        );
    }

    // ========== Compilation Target Scenarios ==========

    /**
     * Multi-target compilation scenarios
     * Arguments: description, grammar, targetLanguage, expectedTargetName
     */
    public static Stream<Arguments> compilationTargets() {
        return Stream.of(
            Arguments.of("Java compilation", PRECEDENCE_CALC, "java", "Java"),
            Arguments.of("Python3 compilation", PRECEDENCE_CALC, "python3", "Python3"),
            Arguments.of("JavaScript compilation", PRECEDENCE_CALC, "javascript", "JavaScript"),
            Arguments.of("TypeScript compilation", PRECEDENCE_CALC, "typescript", "TypeScript"),
            Arguments.of("CSharp compilation", SIMPLE_CALC, "csharp", "CSharp"),
            Arguments.of("Go compilation", SIMPLE_CALC, "go", "Go")
        );
    }

    /**
     * Visualization format scenarios
     * Arguments: description, grammar, ruleName, format, expectedKey
     */
    public static Stream<Arguments> visualizationFormats() {
        return Stream.of(
            Arguments.of("DOT format", SIMPLE_CALC, "expr", "dot", "dot"),
            Arguments.of("Mermaid format", SIMPLE_CALC, "expr", "mermaid", "mermaid"),
            Arguments.of("All formats", PRECEDENCE_CALC, "expr", "all", "dot") // check for at least one key
        );
    }

    /**
     * Invalid compilation scenarios
     * Arguments: description, grammar, targetLanguage, expectedErrorPattern
     */
    public static Stream<Arguments> invalidCompilations() {
        return Stream.of(
            Arguments.of("Invalid language", SIMPLE_HELLO, "invalid_lang", "error"),
            Arguments.of("Empty grammar", "", "java", "error"),
            Arguments.of("Malformed grammar", UNDEFINED_RULE, "python3", "error")
        );
    }
}
