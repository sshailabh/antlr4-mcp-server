grammar SqlSubset;

statement
    : selectStmt
    | insertStmt
    | updateStmt
    | deleteStmt
    ;

selectStmt
    : SELECT columnList FROM tableName whereClause?
    ;

insertStmt
    : INSERT INTO tableName '(' columnList ')' VALUES '(' valueList ')'
    ;

updateStmt
    : UPDATE tableName SET assignment (',' assignment)* whereClause?
    ;

deleteStmt
    : DELETE FROM tableName whereClause?
    ;

whereClause
    : WHERE condition
    ;

condition
    : columnName '=' value
    | columnName '>' value
    | columnName '<' value
    ;

assignment
    : columnName '=' value
    ;

columnList
    : columnName (',' columnName)*
    | '*'
    ;

valueList
    : value (',' value)*
    ;

tableName
    : IDENTIFIER
    ;

columnName
    : IDENTIFIER
    ;

value
    : STRING
    | NUMBER
    | 'NULL'
    ;

SELECT : 'SELECT' ;
FROM   : 'FROM' ;
WHERE  : 'WHERE' ;
INSERT : 'INSERT' ;
INTO   : 'INTO' ;
VALUES : 'VALUES' ;
UPDATE : 'UPDATE' ;
SET    : 'SET' ;
DELETE : 'DELETE' ;

IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

STRING
    : '\'' ( ~['\\] | '\\' . )* '\''
    ;

NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
