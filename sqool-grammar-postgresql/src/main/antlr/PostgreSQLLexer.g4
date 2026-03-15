/*
 * PostgreSQL ANTLR4 Lexer Grammar – sqool internal fork
 *
 * Derived from: https://github.com/antlr/grammars-v4
 * Upstream path: sql/postgresql
 * See UPSTREAM.md for full provenance and local deviations.
 *
 * Local deviations:
 *  1. Scoped to the PostgreSQL v1 subset (core DML/DDL + SELECT).
 *  2. Added Java package header for sqool namespace.
 *  3. Dollar-quoted string support limited to the simple $$ form.
 */

// $antlr-format alignTrailingComments on, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments off, useTab off
// $antlr-format allowShortRulesOnASingleLine on, alignSemicolons ownLine, alignColons trailing

lexer grammar PostgreSQLLexer;

options {
    caseInsensitive = true;
}

@header {
package io.github.e4c5.sqool.grammar.postgresql.generated;
}

// -------------------------------------------------------------------------
// Symbols
// -------------------------------------------------------------------------

SEMI      : ';';
COMMA     : ',';
DOT       : '.';
LPAREN    : '(';
RPAREN    : ')';
LBRACKET  : '[';
RBRACKET  : ']';
STAR      : '*';
PLUS      : '+';
MINUS     : '-';
SLASH     : '/';
PERCENT   : '%';
CARET     : '^';
TILDE     : '~';
AMP       : '&';
PIPE      : '|';
CAST_OP   : '::';
CONCAT    : '||';
JPTR      : '->';
JPTR2     : '->>';
EQ        : '=';
NEQ       : '<>' | '!=';
LT        : '<';
LTE       : '<=';
GT        : '>';
GTE       : '>=';

// -------------------------------------------------------------------------
// Keywords (alphabetically)
// -------------------------------------------------------------------------

ALL         : 'ALL';
AND         : 'AND';
ARRAY       : 'ARRAY';
AS          : 'AS';
ASC         : 'ASC';
BEGIN_KW    : 'BEGIN';
BETWEEN     : 'BETWEEN';
BY          : 'BY';
CASCADE     : 'CASCADE';
CASE_KW     : 'CASE';
CAST_KW     : 'CAST';
COMMIT_KW   : 'COMMIT';
CONFLICT    : 'CONFLICT';
CONSTRAINT  : 'CONSTRAINT';
CREATE      : 'CREATE';
CROSS       : 'CROSS';
DEFAULT_KW  : 'DEFAULT';
DELETE      : 'DELETE';
DESC        : 'DESC';
DISTINCT    : 'DISTINCT';
DO          : 'DO';
DROP        : 'DROP';
ELSE        : 'ELSE';
END_KW      : 'END';
EXCEPT      : 'EXCEPT';
EXISTS      : 'EXISTS';
FALSE_KW    : 'FALSE';
FETCH       : 'FETCH';
FILTER      : 'FILTER';
FIRST_KW    : 'FIRST';
FOR         : 'FOR';
FOREIGN     : 'FOREIGN';
FROM        : 'FROM';
FULL        : 'FULL';
GROUP       : 'GROUP';
HAVING      : 'HAVING';
IF_KW       : 'IF';
ILIKE       : 'ILIKE';
IN_KW       : 'IN';
INNER       : 'INNER';
INSERT      : 'INSERT';
INTERSECT   : 'INTERSECT';
INTO        : 'INTO';
IS          : 'IS';
JOIN        : 'JOIN';
LAST_KW     : 'LAST';
LEFT        : 'LEFT';
LIKE        : 'LIKE';
LIMIT       : 'LIMIT';
NATURAL     : 'NATURAL';
NEXT_KW     : 'NEXT';
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
PRIMARY     : 'PRIMARY';
RECURSIVE   : 'RECURSIVE';
REFERENCES  : 'REFERENCES';
RESTRICT    : 'RESTRICT';
RETURNING   : 'RETURNING';
RIGHT       : 'RIGHT';
ROLLBACK_KW : 'ROLLBACK';
ROW         : 'ROW';
ROWS        : 'ROWS';
SELECT      : 'SELECT';
SET         : 'SET';
SIMILAR     : 'SIMILAR';
TABLE       : 'TABLE';
TEMP        : 'TEMP';
TEMPORARY   : 'TEMPORARY';
THEN        : 'THEN';
TO          : 'TO';
TRUE_KW     : 'TRUE';
TRUNCATE    : 'TRUNCATE';
UNION       : 'UNION';
UNIQUE      : 'UNIQUE';
UPDATE      : 'UPDATE';
USING       : 'USING';
VALUES      : 'VALUES';
WHEN        : 'WHEN';
WHERE       : 'WHERE';
WITH        : 'WITH';
WITHOUT     : 'WITHOUT';

// -------------------------------------------------------------------------
// Transaction keywords
// -------------------------------------------------------------------------

WORK        : 'WORK';
TRANSACTION : 'TRANSACTION';

// -------------------------------------------------------------------------
// Interval keywords
// -------------------------------------------------------------------------

YEAR        : 'YEAR';
MONTH       : 'MONTH';
DAY         : 'DAY';
HOUR        : 'HOUR';
MINUTE      : 'MINUTE';
SECOND      : 'SECOND';

// -------------------------------------------------------------------------
// Other missing keywords
// -------------------------------------------------------------------------

ACTION      : 'ACTION';
ALWAYS      : 'ALWAYS';
ASYMMETRIC  : 'ASYMMETRIC';
CHECK       : 'CHECK';
ESCAPE      : 'ESCAPE';
FUNCTION    : 'FUNCTION';
GENERATED   : 'GENERATED';
IDENTITY    : 'IDENTITY';
KEY         : 'KEY';
NO          : 'NO';
NOTHING     : 'NOTHING';
PARTITION   : 'PARTITION';
SYMMETRIC   : 'SYMMETRIC';

BIGINT      : 'BIGINT';
BIGSERIAL   : 'BIGSERIAL';
BOOLEAN     : 'BOOLEAN';
CHAR_KW     : 'CHAR';
CHARACTER   : 'CHARACTER';
DATE_KW     : 'DATE';
DECIMAL_KW  : 'DECIMAL';
DOUBLE      : 'DOUBLE';
FLOAT_KW    : 'FLOAT';
INT_KW      : 'INT';
INTEGER_KW  : 'INTEGER';
INTERVAL    : 'INTERVAL';
JSONB       : 'JSONB';
JSON        : 'JSON';
NUMERIC_KW  : 'NUMERIC';
PRECISION   : 'PRECISION';
REAL        : 'REAL';
SERIAL      : 'SERIAL';
SMALLINT    : 'SMALLINT';
SMALLSERIAL : 'SMALLSERIAL';
TEXT        : 'TEXT';
TIME        : 'TIME';
TIMESTAMP   : 'TIMESTAMP';
UUID        : 'UUID';
VARCHAR     : 'VARCHAR';
VARYING     : 'VARYING';
ZONE        : 'ZONE';

// -------------------------------------------------------------------------
// Literals
// -------------------------------------------------------------------------

INTEGER_LITERAL
    : [0-9]+
    ;

FLOAT_LITERAL
    : [0-9]+ '.' [0-9]*
    | '.' [0-9]+
    | [0-9]+ ('.' [0-9]*)? [eE] [+-]? [0-9]+
    ;

// Simple dollar-quoted string: $$...$$
DOLLAR_STRING
    : '$$' .*? '$$'
    ;

STRING_LITERAL
    : '\'' (~'\'' | '\'\'')*  '\''
    ;

// E-prefixed escape string
ESCAPE_STRING
    : [eE] '\'' (~['\\\r\n] | '\\' . | '\'\'')*  '\''
    ;

// Unicode string
UNICODE_STRING
    : [uU] '&' '\'' (~'\'' | '\'\'')*  '\''
    ;

BIT_STRING
    : [bB] '\'' [01]* '\''
    ;

HEX_STRING
    : [xX] '\'' [0-9a-fA-F]* '\''
    ;

// -------------------------------------------------------------------------
// Identifiers
// -------------------------------------------------------------------------

IDENTIFIER
    : [a-zA-Z_\u0080-\uffff] [a-zA-Z_0-9$\u0080-\uffff]*
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
