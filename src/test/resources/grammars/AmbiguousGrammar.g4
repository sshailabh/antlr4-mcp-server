grammar AmbiguousGrammar;

// Intentionally ambiguous grammar for testing
stmt
    : 'if' expr 'then' stmt 'else' stmt
    | 'if' expr 'then' stmt
    | ID
    ;

expr
    : ID
    | NUMBER
    ;

ID : [a-z]+ ;
NUMBER : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
