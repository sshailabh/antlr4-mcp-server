grammar Hello;

start : 'hello' WORLD ;

WORLD : 'world' | 'universe' ;
WS : [ \t\r\n]+ -> skip ;
