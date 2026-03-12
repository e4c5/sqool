grammar BootstrapSql;

script
    : statement* EOF
    ;

statement
    : selectStatement SEMICOLON?
    ;

selectStatement
    : SELECT selectList FROM tableName
    ;

selectList
    : STAR
    | IDENTIFIER (COMMA IDENTIFIER)*
    ;

tableName
    : IDENTIFIER
    ;

SELECT : 'SELECT';
FROM : 'FROM';
STAR : '*';
COMMA : ',';
SEMICOLON : ';';
IDENTIFIER : [a-zA-Z_] [a-zA-Z_0-9]*;
WS : [ \t\r\n]+ -> skip;
