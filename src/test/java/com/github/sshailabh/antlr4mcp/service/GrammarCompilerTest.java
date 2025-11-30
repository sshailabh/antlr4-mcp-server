package com.github.sshailabh.antlr4mcp.service;

import com.github.sshailabh.antlr4mcp.support.BaseServiceTest;
import org.antlr.v4.tool.Grammar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrammarCompilerTest extends BaseServiceTest {

    @Autowired
    private GrammarCompiler grammarCompiler;

    @Test
    void testLoadGrammar_ValidGrammar() throws Exception {
        Grammar grammar = grammarCompiler.loadGrammar(SIMPLE_GRAMMAR);

        assertThat(grammar).isNotNull();
        assertThat(grammar.name).isEqualTo("Simple");
        assertThat(grammar.rules).isNotEmpty();
    }

    @Test
    void testLoadGrammar_WithLexerAndParser() throws Exception {
        Grammar grammar = grammarCompiler.loadGrammar(EXPR_GRAMMAR);

        assertThat(grammar).isNotNull();
        assertThat(grammar.name).isEqualTo("Expr");
        assertThat(grammar.getRule("expr")).isNotNull();
        assertThat(grammar.getRule("term")).isNotNull();
        assertThat(grammar.getRule("factor")).isNotNull();
    }

    @Test
    void testLoadGrammar_InvalidGrammar() {
        assertThatThrownBy(() -> grammarCompiler.loadGrammar(INVALID_GRAMMAR))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("grammar declaration");
    }

    @Test
    void testLoadGrammar_EmptyGrammar() {
        String grammarText = "";

        assertThatThrownBy(() -> grammarCompiler.loadGrammar(grammarText))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("grammar declaration");
    }

    @Test
    void testLoadGrammar_NullGrammar() {
        assertThatThrownBy(() -> grammarCompiler.loadGrammar(null))
            .isInstanceOf(Exception.class);
    }

    @Test
    void testLoadGrammar_ComplexGrammar() throws Exception {
        String grammarText = """
            grammar JSON;
            json: value ;
            value
               : STRING
               | NUMBER
               | obj
               | array
               | 'true'
               | 'false'
               | 'null'
               ;
            obj: '{' pair (',' pair)* '}' | '{' '}' ;
            pair: STRING ':' value ;
            array: '[' value (',' value)* ']' | '[' ']' ;
            STRING: '"' (~[\"\\\\] | '\\\\' .)* '"' ;
            NUMBER: '-'? INT ('.' [0-9]+)? ([eE] [+-]? [0-9]+)? ;
            fragment INT: '0' | [1-9] [0-9]* ;
            WS: [ \\t\\r\\n]+ -> skip ;
            """;

        Grammar grammar = grammarCompiler.loadGrammar(grammarText);

        assertThat(grammar).isNotNull();
        assertThat(grammar.name).isEqualTo("JSON");
        assertThat(grammar.getRule("json")).isNotNull();
        assertThat(grammar.getRule("value")).isNotNull();
        assertThat(grammar.getRule("obj")).isNotNull();
        assertThat(grammar.getRule("array")).isNotNull();
    }
}
