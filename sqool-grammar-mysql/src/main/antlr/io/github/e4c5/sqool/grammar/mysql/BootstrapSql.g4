grammar BootstrapSql;

script
    : statement (SEMICOLON statement)* SEMICOLON? EOF
    ;

statement
    : selectStatement
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

SELECT : [sS] [eE] [lL] [eE] [cC] [tT];
FROM : [fF] [rR] [oO] [mM];
STAR : '*';
COMMA : ',';
SEMICOLON : ';';
IDENTIFIER : [a-zA-Z_] [a-zA-Z_0-9]*;
WS : [ \t\r\n]+ -> skip;
