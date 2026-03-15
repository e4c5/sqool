/*
 * Oracle SQL ANTLR4 Lexer Grammar – sqool internal fork
 *
 * Derived from: https://github.com/antlr/grammars-v4
 * Upstream path: sql/plsql
 * See UPSTREAM.md for full provenance and local deviations.
 *
 * Local deviations:
 *  1. Scoped to the Oracle SQL v1 subset (core DML/DDL + SELECT).
 *  2. Added Java package header for sqool namespace.
 *  3. PL/SQL procedural constructs excluded from the token set.
 */

// $antlr-format alignTrailingComments on, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments off, useTab off
// $antlr-format allowShortRulesOnASingleLine on, alignSemicolons ownLine, alignColons trailing

lexer grammar OracleLexer;

options {
    caseInsensitive = true;
}

@header {
package io.github.e4c5.sqool.grammar.oracle.generated;
}

// -------------------------------------------------------------------------
// Symbols
// -------------------------------------------------------------------------

SEMI        : ';';
COMMA       : ',';
DOT         : '.';
LPAREN      : '(';
RPAREN      : ')';
LBRACKET    : '[';
RBRACKET    : ']';
STAR        : '*';
PLUS        : '+';
MINUS       : '-';
SLASH       : '/';
PERCENT     : '%';
TILDE       : '~';
AMP         : '&';
PIPE        : '|';
CARET       : '^';
EQ          : '=';
NEQ         : '<>' | '!=';
LT          : '<';
LTE         : '<=';
GT          : '>';
GTE         : '>=';
CONCAT      : '||';
AT_SIGN     : '@';
COLON       : ':';

// -------------------------------------------------------------------------
// Keywords (alphabetically)
// -------------------------------------------------------------------------

ACTION      : 'ACTION';
ALL         : 'ALL';
ALWAYS      : 'ALWAYS';
ALTER       : 'ALTER';
AND         : 'AND';
AS          : 'AS';
ASC         : 'ASC';
BETWEEN     : 'BETWEEN';
BY          : 'BY';
CASCADE     : 'CASCADE';
CASE_KW     : 'CASE';
CHAR_KW     : 'CHAR';
CHECK       : 'CHECK';
COMMIT_KW   : 'COMMIT';
CONNECT     : 'CONNECT';
CONSTRAINT  : 'CONSTRAINT';
CREATE      : 'CREATE';
CROSS       : 'CROSS';
CURRENT     : 'CURRENT';
DATE_KW     : 'DATE';
DEFAULT_KW  : 'DEFAULT';
DELETE      : 'DELETE';
DESC        : 'DESC';
DISTINCT    : 'DISTINCT';
DROP        : 'DROP';
DUAL        : 'DUAL';
ELSE        : 'ELSE';
END_KW      : 'END';
ESCAPE      : 'ESCAPE';
EXISTS      : 'EXISTS';
FALSE_KW    : 'FALSE';
FETCH       : 'FETCH';
FIRST_KW    : 'FIRST';
FOR         : 'FOR';
FOREIGN     : 'FOREIGN';
FROM        : 'FROM';
FULL        : 'FULL';
GENERATED       : 'GENERATED';
GLOBAL      : 'GLOBAL';
GROUP       : 'GROUP';
HAVING      : 'HAVING';
IDENTITY        : 'IDENTITY';
IF_KW       : 'IF';
IN_KW       : 'IN';
INNER       : 'INNER';
INSERT      : 'INSERT';
INTERSECT   : 'INTERSECT';
INTO        : 'INTO';
IS          : 'IS';
JOIN        : 'JOIN';
KEY         : 'KEY';
LEFT        : 'LEFT';
LIKE        : 'LIKE';
MERGE       : 'MERGE';
MINUS_KW    : 'MINUS';
NATURAL     : 'NATURAL';
NEXT_KW     : 'NEXT';
NO          : 'NO';
NOT         : 'NOT';
NULL_KW     : 'NULL';
NULLS       : 'NULLS';
OF          : 'OF';
OFFSET      : 'OFFSET';
ON          : 'ON';
ONLY        : 'ONLY';
OR          : 'OR';
ORDER       : 'ORDER';
OUTER       : 'OUTER';
OVER        : 'OVER';
PARTITION   : 'PARTITION';
PRIMARY     : 'PRIMARY';
RECURSIVE   : 'RECURSIVE';
REFERENCES  : 'REFERENCES';
RESTRICT    : 'RESTRICT';
RIGHT       : 'RIGHT';
ROLLBACK_KW : 'ROLLBACK';
ROW         : 'ROW';
ROWNUM      : 'ROWNUM';
ROWID       : 'ROWID';
ROWS        : 'ROWS';
SAVEPOINT   : 'SAVEPOINT';
SELECT      : 'SELECT';
SET         : 'SET';
START       : 'START';
SYSDATE     : 'SYSDATE';
SYSTIMESTAMP: 'SYSTIMESTAMP';
TABLE       : 'TABLE';
TEMP        : 'TEMP';
TEMPORARY   : 'TEMPORARY';
THEN        : 'THEN';
TO          : 'TO';
TRANSACTION : 'TRANSACTION';
TRUE_KW     : 'TRUE';
TRUNCATE    : 'TRUNCATE';
UNION       : 'UNION';
UNIQUE       : 'UNIQUE';
UPDATE      : 'UPDATE';
USING       : 'USING';
VALUES      : 'VALUES';
WHEN        : 'WHEN';
WHERE       : 'WHERE';
WITH        : 'WITH';
WITHOUT     : 'WITHOUT';
WORK        : 'WORK';

// -------------------------------------------------------------------------
// Oracle type keywords
// -------------------------------------------------------------------------

BINARY_DOUBLE   : 'BINARY_DOUBLE';
BINARY_FLOAT    : 'BINARY_FLOAT';
BLOB            : 'BLOB';
CLOB            : 'CLOB';
FLOAT_KW        : 'FLOAT';
INTEGER_KW      : 'INTEGER';
INT_KW          : 'INT';
INTERVAL        : 'INTERVAL';
LONG_KW         : 'LONG';
NCHAR           : 'NCHAR';
NCLOB           : 'NCLOB';
NUMBER_KW       : 'NUMBER';
NVARCHAR2       : 'NVARCHAR2';
PURGE           : 'PURGE';
RAW_KW          : 'RAW';
REAL            : 'REAL';
SMALLINT        : 'SMALLINT';
TIMESTAMP       : 'TIMESTAMP';
VARCHAR2        : 'VARCHAR2';
VARCHAR         : 'VARCHAR';
XMLTYPE         : 'XMLTYPE';
YEAR            : 'YEAR';
MONTH           : 'MONTH';
DAY             : 'DAY';
HOUR            : 'HOUR';
MINUTE          : 'MINUTE';
SECOND          : 'SECOND';
LOCAL           : 'LOCAL';
TIME            : 'TIME';
ZONE            : 'ZONE';
PRECISION       : 'PRECISION';
DOUBLE          : 'DOUBLE';
CHAR_VARYING    : 'CHAR VARYING';
CHARACTER       : 'CHARACTER';
VARYING         : 'VARYING';
NATIONAL        : 'NATIONAL';
DECIMAL_KW      : 'DECIMAL';
NUMERIC_KW      : 'NUMERIC';
BOOLEAN         : 'BOOLEAN';
BYTE            : 'BYTE';
CONSTRAINTS     : 'CONSTRAINTS';

// -------------------------------------------------------------------------
// Literals
// -------------------------------------------------------------------------

INTEGER_LITERAL
    : [0-9]+
    ;

FLOAT_LITERAL
    : [0-9]+ '.' [0-9]*
    | '.' [0-9]+
    | [0-9]+ ('.' [0-9]*)? [e] [+-]? [0-9]+
    ;

STRING_LITERAL
    : '\'' (~'\'' | '\'\'')*  '\''
    ;

// -------------------------------------------------------------------------
// Identifiers
// -------------------------------------------------------------------------

IDENTIFIER
    : [a-z_\u0080-\uffff] [a-z_0-9$#\u0080-\uffff]*
    ;

QUOTED_IDENTIFIER
    : '"' (~'"' | '""')* '"'
    ;

// -------------------------------------------------------------------------
// Comments and whitespace
// -------------------------------------------------------------------------

BLOCK_COMMENT
    : '/*' (BLOCK_COMMENT | .)*? '*/' -> skip
    ;

LINE_COMMENT
    : '--' ~[\r\n]* -> skip
    ;

WHITESPACE
    : [ \t\r\n\f]+ -> skip
    ;
