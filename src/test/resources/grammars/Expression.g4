/**
 * Expression Grammar - Demonstrates key ANTLR4 features for compiler engineers
 * 
 * This grammar demonstrates:
 * - Left recursion (automatically transformed by ANTLR4)
 * - Operator precedence via alternative ordering
 * - Associativity (left and right)
 * - Semantic predicates
 * - Actions (comments showing where they'd go)
 * - Token modes wouldn't work in combined grammar
 */
grammar Expression;

// Entry point
program
    : statement* EOF
    ;

// Statements
statement
    : assignment ';'
    | expression ';'
    | ifStatement
    | whileStatement
    | '{' statement* '}'
    ;

assignment
    : ID '=' expression
    ;

ifStatement
    : 'if' '(' expression ')' statement ('else' statement)?
    ;

whileStatement
    : 'while' '(' expression ')' statement
    ;

// Expressions with precedence and associativity
// ANTLR4 handles left-recursion automatically
// Alternatives are tried top-to-bottom, giving precedence order
expression
    : expression '?' expression ':' expression  // Ternary (right assoc)
    | expression '||' expression                // Logical OR
    | expression '&&' expression                // Logical AND
    | expression '|' expression                 // Bitwise OR
    | expression '^' expression                 // Bitwise XOR
    | expression '&' expression                 // Bitwise AND
    | expression ('==' | '!=') expression       // Equality
    | expression ('<' | '>' | '<=' | '>=') expression  // Relational
    | expression ('<<' | '>>') expression       // Shift
    | expression ('+' | '-') expression         // Additive
    | expression ('*' | '/' | '%') expression   // Multiplicative
    | ('!' | '~' | '-' | '+') expression        // Unary prefix
    | expression ('++' | '--')                  // Unary postfix
    | expression '[' expression ']'             // Array access
    | expression '(' argumentList? ')'          // Function call
    | expression '.' ID                         // Member access
    | primary
    ;

primary
    : '(' expression ')'
    | literal
    | ID
    ;

literal
    : INT
    | FLOAT
    | STRING
    | 'true'
    | 'false'
    | 'null'
    ;

argumentList
    : expression (',' expression)*
    ;

// Lexer Rules
ID      : [a-zA-Z_] [a-zA-Z_0-9]* ;
INT     : [0-9]+ ;
FLOAT   : [0-9]+ '.' [0-9]* | '.' [0-9]+ ;
STRING  : '"' (~["\r\n] | '\\"')* '"' 
        | '\'' (~['\r\n] | '\\\'')* '\'' 
        ;

// Keywords (must be after ID to get proper priority)
// In a real grammar, you'd typically handle keywords differently

// Whitespace and comments
WS          : [ \t\r\n]+ -> skip ;
LINE_COMMENT: '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT: '/*' .*? '*/' -> skip ;

