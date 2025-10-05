grammar TestWithImport;

import CommonLexer;

program
    : statement+ EOF
    ;

statement
    : assignment
    | expression
    ;

assignment
    : ID '=' expression ';'
    ;

expression
    : ID
    | NUMBER
    | STRING
    ;
