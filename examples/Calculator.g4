grammar Calculator;

expr
    : expr ('*'|'/') expr       # MulDiv
    | expr ('+'|'-') expr       # AddSub
    | NUMBER                     # Num
    | '(' expr ')'              # Parens
    ;

NUMBER : [0-9]+ ('.' [0-9]+)? ;
WS     : [ \t\r\n]+ -> skip ;
