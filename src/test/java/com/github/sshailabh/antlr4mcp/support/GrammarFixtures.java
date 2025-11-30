package com.github.sshailabh.antlr4mcp.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Central repository for test grammar fixtures.
 *
 * This class provides:
 * - Inline grammar samples for common test cases
 * - File-based grammar loading utilities
 * - Sample inputs for parsing tests
 * - Grammar variants for edge case testing
 *
 * All grammars are categorized by complexity and purpose:
 * - SIMPLE: Minimal grammars for basic validation
 * - CALC: Expression grammars for parsing tests
 * - COMPLEX: Real-world grammars (JSON, etc.)
 * - INVALID: Malformed grammars for error testing
 * - AMBIGUOUS: Grammars with intentional ambiguities
 */
public final class GrammarFixtures {

    private GrammarFixtures() {
        // Utility class
    }

    // ========== SIMPLE GRAMMARS ==========

    public static final String SIMPLE_HELLO = """
        grammar Hello;
        start : 'hello' ;
        """;

    public static final String SIMPLE_HELLO_WORLD = """
        grammar HelloWorld;
        start : 'hello' WORLD ;
        WORLD : 'world' | 'universe' ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    public static final String MINIMAL_GRAMMAR = """
        grammar Minimal;
        root : 'x' ;
        """;

    public static final String EMPTY_RULE_GRAMMAR = """
        grammar EmptyRule;
        prog : EOF ;
        """;

    // ========== CALCULATOR GRAMMARS ==========

    public static final String SIMPLE_CALC = """
        grammar SimpleCalc;
        expr : term (('+'|'-') term)* ;
        term : factor (('*'|'/') factor)* ;
        factor : INT | '(' expr ')' ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    public static final String LEFT_RECURSIVE_CALC = """
        grammar LeftRecursiveCalc;
        expr
            : expr ('*'|'/') expr
            | expr ('+'|'-') expr
            : NUMBER
            ;
        NUMBER : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    public static final String PRECEDENCE_CALC = """
        grammar PrecedenceCalc;
        prog : expr EOF ;
        expr : expr ('*'|'/') expr
             | expr ('+'|'-') expr
             | INT
             ;
        INT : [0-9]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    // ========== AMBIGUOUS GRAMMARS ==========

    public static final String DANGLING_ELSE = """
        grammar DanglingElse;
        stat
            : 'if' expr 'then' stat ('else' stat)?
            | ID
            ;
        expr : ID ;
        ID : [a-z]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    public static final String AMBIGUOUS_ALTERNATIVES = """
        grammar AmbiguousAlternatives;
        rule
            : ID ID
            | ID
            ;
        ID : [a-z]+ ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    // ========== INVALID GRAMMARS ==========

    public static final String UNDEFINED_RULE = """
        grammar UndefinedRule;
        start : undefined_rule ;
        """;

    public static final String MISSING_SEMICOLON = """
        grammar MissingSemicolon;
        start : 'hello'
        end : 'world' ;
        """;

    public static final String NO_GRAMMAR_DECLARATION = """
        start : 'hello' ;
        """;

    public static final String WRONG_GRAMMAR_NAME = """
        grammar Expected;
        start : 'test' ;
        """;

    public static final String EMPTY_GRAMMAR = "";

    // ========== LEXER GRAMMARS ==========

    public static final String COMMON_LEXER = """
        lexer grammar CommonLexer;
        ID : [a-zA-Z_][a-zA-Z_0-9]* ;
        NUMBER : [0-9]+ ('.' [0-9]+)? ;
        STRING : '"' (~["])* '"' ;
        WS : [ \\t\\r\\n]+ -> skip ;
        COMMENT : '//' ~[\\r\\n]* -> skip ;
        """;

    // ========== COMPLEX GRAMMARS ==========

    public static final String JSON_INLINE = """
        grammar Json;
        json : value ;
        value
            : object
            | array
            | STRING
            | NUMBER
            | 'true'
            | 'false'
            | 'null'
            ;
        object : '{' pair (',' pair)* '}' | '{' '}' ;
        pair : STRING ':' value ;
        array : '[' value (',' value)* ']' | '[' ']' ;
        STRING : '"' (~["])* '"' ;
        NUMBER : '-'? [0-9]+ ('.' [0-9]+)? ([eE] [+-]? [0-9]+)? ;
        WS : [ \\t\\r\\n]+ -> skip ;
        """;

    // ========== SAMPLE INPUTS ==========

    public static class Inputs {
        public static final String HELLO = "hello";
        public static final String HELLO_WORLD = "hello world";
        public static final String HELLO_UNIVERSE = "hello universe";

        public static final String SIMPLE_EXPR = "3 + 4";
        public static final String COMPLEX_EXPR = "3 + 4 * 5";
        public static final String NESTED_EXPR = "(3 + 4) * (5 - 2)";
        public static final String LARGE_EXPR = buildLargeExpression(50);

        public static final String SIMPLE_JSON = "{\"name\": \"John\"}";
        public static final String COMPLEX_JSON = """
            {
                "name": "John",
                "age": 30,
                "address": {
                    "city": "NYC",
                    "zip": "10001"
                },
                "tags": ["developer", "java"]
            }
            """;

        public static final String INVALID_INPUT = "goodbye";
        public static final String EMPTY_INPUT = "";

        private static String buildLargeExpression(int terms) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < terms; i++) {
                if (i > 0) sb.append(" + ");
                sb.append(i);
            }
            return sb.toString();
        }
    }

    // ========== FILE-BASED GRAMMARS ==========

    /**
     * Grammar files available in src/test/resources/grammars/
     */
    public enum GrammarFile {
        SIMPLE_CALC("SimpleCalc.g4"),
        JSON("Json.g4"),
        COMMON_LEXER("CommonLexer.g4"),
        AMBIGUOUS("AmbiguousGrammar.g4"),
        HELLO("Hello.g4"),
        TEST_WITH_IMPORT("TestWithImport.g4");

        private final String filename;

        GrammarFile(String filename) {
            this.filename = filename;
        }

        public String load() throws Exception {
            Path path = Paths.get("src/test/resources/grammars", filename);
            return Files.readString(path);
        }

        public String getFilename() {
            return filename;
        }

        public Path getPath() {
            return Paths.get("src/test/resources/grammars", filename);
        }
    }

    // ========== GRAMMAR BUILDERS ==========

    /**
     * Fluent builder for creating test grammars
     */
    public static class GrammarBuilder {
        private String name = "TestGrammar";
        private final StringBuilder rules = new StringBuilder();
        private final StringBuilder lexerRules = new StringBuilder();

        public GrammarBuilder named(String name) {
            this.name = name;
            return this;
        }

        public GrammarBuilder withRule(String ruleName, String ruleBody) {
            rules.append(ruleName).append(" : ").append(ruleBody).append(" ;\n");
            return this;
        }

        public GrammarBuilder withLexerRule(String ruleName, String pattern) {
            lexerRules.append(ruleName).append(" : ").append(pattern).append(" ;\n");
            return this;
        }

        public GrammarBuilder withStartRule(String ruleBody) {
            return withRule("start", ruleBody);
        }

        public GrammarBuilder withWhitespace() {
            return withLexerRule("WS", "[ \\t\\r\\n]+ -> skip");
        }

        public String build() {
            StringBuilder grammar = new StringBuilder();
            grammar.append("grammar ").append(name).append(";\n\n");
            grammar.append(rules);
            if (lexerRules.length() > 0) {
                grammar.append("\n");
                grammar.append(lexerRules);
            }
            return grammar.toString();
        }
    }

    public static GrammarBuilder builder() {
        return new GrammarBuilder();
    }

    // ========== EXPECTED RESULTS ==========

    /**
     * Expected validation results for known grammars
     */
    public static class ExpectedResults {
        private static final Map<String, ValidationExpectation> EXPECTATIONS = new HashMap<>();

        static {
            EXPECTATIONS.put(SIMPLE_HELLO, new ValidationExpectation("Hello", 1, 0, true));
            EXPECTATIONS.put(SIMPLE_CALC, new ValidationExpectation("SimpleCalc", 3, 2, true));
            EXPECTATIONS.put(JSON_INLINE, new ValidationExpectation("Json", 5, 3, true));
            EXPECTATIONS.put(COMMON_LEXER, new ValidationExpectation("CommonLexer", 0, 5, true));
            EXPECTATIONS.put(UNDEFINED_RULE, new ValidationExpectation("UndefinedRule", -1, -1, false));
            EXPECTATIONS.put(EMPTY_GRAMMAR, new ValidationExpectation(null, -1, -1, false));
        }

        public static ValidationExpectation get(String grammar) {
            return EXPECTATIONS.get(grammar);
        }
    }

    /**
     * Expected validation outcome for a grammar
     */
    public static record ValidationExpectation(
        String grammarName,
        int parserRules,
        int lexerRules,
        boolean shouldSucceed
    ) {}
}
