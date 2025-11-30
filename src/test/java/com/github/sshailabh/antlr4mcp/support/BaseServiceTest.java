package com.github.sshailabh.antlr4mcp.support;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class BaseServiceTest {

    protected static final String SIMPLE_GRAMMAR = """
        grammar Simple;
        prog: stat+ ;
        stat: expr ';' ;
        expr: INT | ID ;
        INT: [0-9]+ ;
        ID: [a-zA-Z]+ ;
        WS: [ \\t\\r\\n]+ -> skip ;
        """;

    protected static final String EXPR_GRAMMAR = """
        grammar Expr;
        expr: term (('+' | '-') term)* ;
        term: factor (('*' | '/') factor)* ;
        factor: INT | '(' expr ')' ;
        INT: [0-9]+ ;
        WS: [ \\t\\r\\n]+ -> skip ;
        """;

    protected static final String AMBIGUOUS_GRAMMAR = """
        grammar Ambig;
        expr: expr '+' expr | INT ;
        INT: [0-9]+ ;
        WS: [ \\t\\r\\n]+ -> skip ;
        """;

    protected static final String INVALID_GRAMMAR = """
        grammar Invalid
        prog: stat+
        """;
}
