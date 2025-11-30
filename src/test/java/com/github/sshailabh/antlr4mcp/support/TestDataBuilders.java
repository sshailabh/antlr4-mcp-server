package com.github.sshailabh.antlr4mcp.support;

import com.github.sshailabh.antlr4mcp.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test data builders for domain model objects.
 *
 * Provides fluent builders for creating test instances of:
 * - ValidationResult
 * - ParseResult
 * - AmbiguityReport
 * - GrammarError
 * - CallGraphResult
 * - ComplexityMetrics
 *
 * Usage:
 * <pre>
 * ValidationResult result = TestDataBuilders.validationResult()
 *     .withGrammarName("Test")
 *     .withSuccess(true)
 *     .withParserRules(5)
 *     .withLexerRules(3)
 *     .build();
 * </pre>
 */
public final class TestDataBuilders {

    private TestDataBuilders() {
        // Utility class
    }

    // ========== ValidationResult Builder ==========

    public static ValidationResultBuilder validationResult() {
        return new ValidationResultBuilder();
    }

    public static class ValidationResultBuilder {
        private boolean success = true;
        private String grammarName = "TestGrammar";
        private int parserRules = 0;
        private int lexerRules = 0;
        private final List<GrammarError> errors = new ArrayList<>();

        public ValidationResultBuilder withSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public ValidationResultBuilder withGrammarName(String name) {
            this.grammarName = name;
            return this;
        }

        public ValidationResultBuilder withParserRules(int count) {
            this.parserRules = count;
            return this;
        }

        public ValidationResultBuilder withLexerRules(int count) {
            this.lexerRules = count;
            return this;
        }

        public ValidationResultBuilder withError(GrammarError error) {
            this.errors.add(error);
            this.success = false;
            return this;
        }

        public ValidationResultBuilder withError(String message, int line, int column) {
            return withError(grammarError()
                .withMessage(message)
                .withLine(line)
                .withColumn(column)
                .build());
        }

        public ValidationResult build() {
            return ValidationResult.builder()
                .success(success)
                .grammarName(grammarName)
                .parserRules(parserRules)
                .lexerRules(lexerRules)
                .errors(new ArrayList<>(errors))
                .build();
        }
    }

    // ========== GrammarError Builder ==========

    public static GrammarErrorBuilder grammarError() {
        return new GrammarErrorBuilder();
    }

    public static class GrammarErrorBuilder {
        private String message = "Test error";
        private String type = "ERROR";
        private int line = 1;
        private int column = 0;
        private String suggestedFix = null;
        private String ruleName = null;

        public GrammarErrorBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        public GrammarErrorBuilder withType(String type) {
            this.type = type;
            return this;
        }

        public GrammarErrorBuilder withLine(int line) {
            this.line = line;
            return this;
        }

        public GrammarErrorBuilder withColumn(int column) {
            this.column = column;
            return this;
        }

        public GrammarErrorBuilder withSuggestion(String suggestion) {
            this.suggestedFix = suggestion;
            return this;
        }

        public GrammarErrorBuilder withRuleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public GrammarError build() {
            return GrammarError.builder()
                .message(message)
                .type(type)
                .line(line)
                .column(column)
                .suggestedFix(suggestedFix)
                .ruleName(ruleName)
                .build();
        }
    }

    // ========== ParseResult Builder ==========

    public static ParseResultBuilder parseResult() {
        return new ParseResultBuilder();
    }

    public static class ParseResultBuilder {
        private boolean success = true;
        private String parseTree = null;
        private String tokens = null;
        private String svg = null;
        private final List<GrammarError> errors = new ArrayList<>();

        public ParseResultBuilder withSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public ParseResultBuilder withParseTree(String tree) {
            this.parseTree = tree;
            return this;
        }

        public ParseResultBuilder withTokens(String tokens) {
            this.tokens = tokens;
            return this;
        }

        public ParseResultBuilder withSvg(String svg) {
            this.svg = svg;
            return this;
        }

        public ParseResultBuilder withError(GrammarError error) {
            this.errors.add(error);
            this.success = false;
            return this;
        }

        public ParseResult build() {
            return ParseResult.builder()
                .success(success)
                .parseTree(parseTree)
                .tokens(tokens)
                .svg(svg)
                .errors(new ArrayList<>(errors))
                .build();
        }
    }

    // ========== AmbiguityReport Builder ==========

    public static AmbiguityReportBuilder ambiguityReport() {
        return new AmbiguityReportBuilder();
    }

    public static class AmbiguityReportBuilder {
        private boolean hasAmbiguities = false;
        private final List<Ambiguity> ambiguities = new ArrayList<>();

        public AmbiguityReportBuilder withAmbiguity(Ambiguity ambiguity) {
            this.ambiguities.add(ambiguity);
            this.hasAmbiguities = true;
            return this;
        }

        public AmbiguityReportBuilder withAmbiguity(String ruleName, int line, int column) {
            Ambiguity ambiguity = Ambiguity.builder()
                .ruleName(ruleName)
                .line(line)
                .column(column)
                .build();
            return withAmbiguity(ambiguity);
        }

        public AmbiguityReport build() {
            return AmbiguityReport.builder()
                .hasAmbiguities(hasAmbiguities)
                .ambiguities(new ArrayList<>(ambiguities))
                .build();
        }
    }

    // ========== Tool Request Builder ==========

    public static ToolRequestBuilder toolRequest() {
        return new ToolRequestBuilder();
    }

    public static class ToolRequestBuilder {
        private String toolName = "test_tool";
        private final Map<String, Object> arguments = new HashMap<>();

        public ToolRequestBuilder withName(String name) {
            this.toolName = name;
            return this;
        }

        public ToolRequestBuilder withArgument(String key, Object value) {
            this.arguments.put(key, value);
            return this;
        }

        public ToolRequestBuilder withGrammar(String grammarText) {
            return withArgument("grammar_text", grammarText);
        }

        public ToolRequestBuilder withInput(String input) {
            return withArgument("sample_input", input);
        }

        public ToolRequestBuilder withStartRule(String startRule) {
            return withArgument("start_rule", startRule);
        }

        public ToolRequestBuilder withFormat(String format) {
            return withArgument("format", format);
            }

        public ToolRequestBuilder withOutputFormat(String format) {
            return withArgument("output_format", format);
        }

        public Map<String, Object> buildArguments() {
            return new HashMap<>(arguments);
        }
    }

    // ========== Test Case Builder ==========

    /**
     * Builder for creating parameterized test cases
     */
    public static TestCaseBuilder testCase() {
        return new TestCaseBuilder();
    }

    public static class TestCaseBuilder {
        private String name;
        private String grammar;
        private String input;
        private String startRule;
        private boolean expectSuccess = true;
        private String expectedError = null;

        public TestCaseBuilder named(String name) {
            this.name = name;
            return this;
        }

        public TestCaseBuilder withGrammar(String grammar) {
            this.grammar = grammar;
            return this;
        }

        public TestCaseBuilder withInput(String input) {
            this.input = input;
            return this;
        }

        public TestCaseBuilder withStartRule(String startRule) {
            this.startRule = startRule;
            return this;
        }

        public TestCaseBuilder expectSuccess() {
            this.expectSuccess = true;
            return this;
        }

        public TestCaseBuilder expectFailure() {
            this.expectSuccess = false;
            return this;
        }

        public TestCaseBuilder expectError(String errorPattern) {
            this.expectedError = errorPattern;
            this.expectSuccess = false;
            return this;
        }

        public TestCase build() {
            return new TestCase(name, grammar, input, startRule, expectSuccess, expectedError);
        }
    }

    /**
     * Represents a complete test case for parameterized testing
     */
    public record TestCase(
        String name,
        String grammar,
        String input,
        String startRule,
        boolean expectSuccess,
        String expectedError
    ) {
        @Override
        public String toString() {
            return name != null ? name : "TestCase";
        }
    }
}
