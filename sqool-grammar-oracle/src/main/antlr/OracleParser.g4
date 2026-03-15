/*
 * Oracle SQL ANTLR4 Parser Grammar – sqool internal fork
 *
 * Derived from: https://github.com/antlr/grammars-v4
 * Upstream path: sql/plsql
 * See UPSTREAM.md for full provenance and local deviations.
 *
 * Local deviations:
 *  1. Scoped to the Oracle SQL v1 subset (core DML/DDL + SELECT).
 *  2. Added Java package header for sqool namespace.
 *  3. PL/SQL anonymous blocks and procedural constructs excluded.
 *  4. Simplified expression grammar for improved SLL prediction performance.
 */

// $antlr-format alignTrailingComments on, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments off, useTab off
// $antlr-format allowShortRulesOnASingleLine off, allowShortBlocksOnASingleLine on, alignSemicolons ownLine
// $antlr-format alignColons hanging

parser grammar OracleParser;

options {
    tokenVocab = OracleLexer;
}

@header {
package io.github.e4c5.sqool.grammar.oracle.generated;
}

// =========================================================================
// Entry points
// =========================================================================

/** Script entry point: one or more statements separated by semicolons. */
root
    : statement (SEMI statement)* SEMI? EOF
    ;

/** Single-statement entry point. */
singleStatement
    : statement SEMI? EOF
    ;

statement
    : selectStatement
    | insertStatement
    | updateStatement
    | deleteStatement
    | createTableStatement
    | dropTableStatement
    | truncateStatement
    | commitStatement
    | rollbackStatement
    | savepointStatement
    ;

// =========================================================================
// SELECT
// =========================================================================

selectStatement
    : withClause? queryExpression orderByClause? (fetchClause | offsetFetchClause)?
    ;

withClause
    : WITH cteDefinition (COMMA cteDefinition)*
    ;

cteDefinition
    : cteName = name AS LPAREN selectStatement RPAREN
    ;

queryExpression
    : queryTerm (setOperator queryTerm)*
    ;

setOperator
    : UNION ALL?
    | INTERSECT
    | MINUS_KW
    ;

queryTerm
    : SELECT DISTINCT? selectList fromClause? whereClause? groupByClause? havingClause?
    | LPAREN selectStatement RPAREN
    ;

selectList
    : selectItem (COMMA selectItem)*
    ;

selectItem
    : STAR                          # allColumnsItem
    | tableRef DOT STAR             # tableAllColumnsItem
    | expr (AS? alias = name)?      # expressionItem
    ;

// =========================================================================
// FROM / JOIN
// =========================================================================

fromClause
    : FROM tableReference (COMMA tableReference)*
    ;

tableReference
    : tablePrimary joinClause*
    ;

tablePrimary
    : qualifiedName (AS? alias = name)?         # namedTable
    | LPAREN selectStatement RPAREN AS? alias = name  # derivedTable
    | LPAREN tableReference RPAREN              # parenTableRef
    ;

joinClause
    : CROSS JOIN tablePrimary                                                    # crossJoin
    | NATURAL joinKind? JOIN tablePrimary                                        # naturalJoin
    | joinKind? JOIN tablePrimary (ON expr | USING LPAREN columnList RPAREN)     # qualifiedJoin
    ;

joinKind
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    ;

// =========================================================================
// WHERE / GROUP BY / HAVING
// =========================================================================

whereClause
    : WHERE expr
    ;

groupByClause
    : GROUP BY expr (COMMA expr)*
    ;

havingClause
    : HAVING expr
    ;

// =========================================================================
// ORDER BY / FETCH FIRST / OFFSET-FETCH
// =========================================================================

orderByClause
    : ORDER BY orderByItem (COMMA orderByItem)*
    ;

orderByItem
    : expr direction = (ASC | DESC)? (NULLS (FIRST_KW | LAST_KW))?
    ;

/** Oracle 12c+ row limiting clause: FETCH FIRST n ROWS ONLY */
fetchClause
    : FETCH (FIRST_KW | NEXT_KW) expr? (ROW | ROWS) ONLY
    ;

/** Standard OFFSET / FETCH clause */
offsetFetchClause
    : OFFSET expr (ROW | ROWS)?
      (FETCH (FIRST_KW | NEXT_KW) expr? (ROW | ROWS) ONLY)?
    ;

// =========================================================================
// INSERT
// =========================================================================

insertStatement
    : INSERT INTO qualifiedName (AS? alias = name)? (LPAREN columnList RPAREN)?
      insertSource
    ;

insertSource
    : VALUES rowValues (COMMA rowValues)*   # insertValues
    | selectStatement                       # insertSelect
    ;

rowValues
    : LPAREN insertExpr (COMMA insertExpr)* RPAREN
    ;

insertExpr
    : DEFAULT_KW        # defaultExpr
    | expr              # valueExpr
    ;

// =========================================================================
// UPDATE
// =========================================================================

updateStatement
    : UPDATE qualifiedName (AS? alias = name)?
      SET setClauseList
      whereClause?
    ;

setClauseList
    : setClause (COMMA setClause)*
    ;

setClause
    : columnName EQ expr
    | columnName EQ DEFAULT_KW
    ;

// =========================================================================
// DELETE
// =========================================================================

deleteStatement
    : DELETE FROM? qualifiedName (AS? alias = name)?
      whereClause?
    ;

// =========================================================================
// CREATE TABLE
// =========================================================================

createTableStatement
    : CREATE (GLOBAL TEMPORARY | TEMPORARY)? TABLE ifNotExists? qualifiedName
      LPAREN tableElementList RPAREN
    ;

ifNotExists
    : IF_KW NOT EXISTS
    ;

tableElementList
    : tableElement (COMMA tableElement)*
    ;

tableElement
    : columnDefinition
    | tableConstraint
    ;

columnDefinition
    : columnName typeName columnConstraint*
    ;

tableConstraint
    : (CONSTRAINT name)? tableConstraintBody
    ;

tableConstraintBody
    : PRIMARY KEY LPAREN columnList RPAREN
    | UNIQUE LPAREN columnList RPAREN
    | FOREIGN KEY LPAREN columnList RPAREN REFERENCES qualifiedName (LPAREN columnList RPAREN)?
      referentialAction*
    | CHECK LPAREN expr RPAREN
    ;

referentialAction
    : ON (UPDATE | DELETE) (CASCADE | SET NULL_KW | SET DEFAULT_KW | RESTRICT | NO ACTION)
    ;

columnConstraint
    : (CONSTRAINT name)? columnConstraintBody
    ;

columnConstraintBody
    : NOT NULL_KW
    | NULL_KW
    | UNIQUE
    | PRIMARY KEY
    | DEFAULT_KW expr
    | REFERENCES qualifiedName (LPAREN columnList RPAREN)? referentialAction*
    | CHECK LPAREN expr RPAREN
    | GENERATED (ALWAYS | BY DEFAULT_KW) AS IDENTITY (LPAREN sequenceOption* RPAREN)?
    ;

sequenceOption
    : name expr
    ;

// =========================================================================
// DROP TABLE
// =========================================================================

dropTableStatement
    : DROP TABLE ifExists? qualifiedName (CASCADE (CONSTRAINT | CONSTRAINTS)?)? PURGE?
    ;

ifExists
    : IF_KW EXISTS
    ;

// =========================================================================
// TRUNCATE
// =========================================================================

truncateStatement
    : TRUNCATE TABLE? qualifiedName
    ;

// =========================================================================
// Transaction control
// =========================================================================

commitStatement
    : COMMIT_KW (WORK | TRANSACTION)?
    ;

rollbackStatement
    : ROLLBACK_KW (WORK | TRANSACTION)? (TO SAVEPOINT? name)?
    ;

savepointStatement
    : SAVEPOINT name
    ;

// =========================================================================
// Type names
// =========================================================================

typeName
    : NUMBER_KW (LPAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RPAREN)?
    | VARCHAR2 LPAREN INTEGER_LITERAL (BYTE | CHAR_KW)? RPAREN
    | NVARCHAR2 LPAREN INTEGER_LITERAL RPAREN
    | CHAR_KW VARYING? (LPAREN INTEGER_LITERAL (BYTE | CHAR_KW)? RPAREN)?
    | NCHAR (LPAREN INTEGER_LITERAL RPAREN)?
    | VARCHAR (LPAREN INTEGER_LITERAL RPAREN)?
    | DATE_KW
    | TIMESTAMP (LPAREN INTEGER_LITERAL RPAREN)? (WITH LOCAL? TIME ZONE)?
    | INTERVAL intervalQualifier
    | FLOAT_KW (LPAREN INTEGER_LITERAL RPAREN)?
    | BINARY_FLOAT
    | BINARY_DOUBLE
    | INTEGER_KW
    | INT_KW
    | SMALLINT
    | REAL
    | DECIMAL_KW (LPAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RPAREN)?
    | NUMERIC_KW (LPAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RPAREN)?
    | CLOB
    | NCLOB
    | BLOB
    | RAW_KW LPAREN INTEGER_LITERAL RPAREN
    | LONG_KW RAW_KW?
    | XMLTYPE
    | BOOLEAN
    | CHARACTER VARYING? (LPAREN INTEGER_LITERAL RPAREN)?
    | DOUBLE PRECISION
    | name
    ;

intervalQualifier
    : YEAR (LPAREN INTEGER_LITERAL RPAREN)? TO MONTH
    | DAY (LPAREN INTEGER_LITERAL RPAREN)? TO SECOND (LPAREN INTEGER_LITERAL RPAREN)?
    | YEAR (LPAREN INTEGER_LITERAL RPAREN)?
    | MONTH
    | DAY (LPAREN INTEGER_LITERAL RPAREN)?
    | HOUR
    | MINUTE
    | SECOND (LPAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RPAREN)?
    ;

// =========================================================================
// Expressions
// =========================================================================

/**
 * Precedence (highest first, per ANTLR4 left-recursive convention):
 *  1. Unary - +
 *  2. * /
 *  3. + - || (concat)
 *  4. Comparison < > <= >= = <>
 *  5. IS / IS NOT NULL
 *  6. LIKE
 *  7. BETWEEN / NOT BETWEEN
 *  8. IN / NOT IN
 *  9. NOT (unary logical)
 * 10. AND
 * 11. OR  (lowest)
 */
expr
    : op = (MINUS | PLUS) expr                                                  # unaryExpr
    | expr op = (STAR | SLASH) expr                                              # mulExpr
    | expr op = (PLUS | MINUS | CONCAT) expr                                     # addExpr
    | expr op = (LT | GT | LTE | GTE | EQ | NEQ) expr                           # compExpr
    | expr IS NOT? NULL_KW                                                       # isNullExpr
    | expr NOT? BETWEEN expr AND expr                                            # betweenExpr
    | expr NOT? LIKE expr (ESCAPE expr)?                                         # likeExpr
    | expr NOT? IN_KW LPAREN (expr (COMMA expr)* | selectStatement) RPAREN      # inExpr
    | NOT expr                                                                   # notExpr
    | expr AND expr                                                              # andExpr
    | expr OR expr                                                               # orExpr
    | CASE_KW caseCondition+ (ELSE expr)? END_KW                                # searchedCaseExpr
    | CASE_KW expr caseCondition+ (ELSE expr)? END_KW                           # simpleCaseExpr
    | EXISTS LPAREN selectStatement RPAREN                                       # existsExpr
    | functionCall                                                               # funcCallExpr
    | LPAREN selectStatement RPAREN                                              # scalarSubquery
    | LPAREN expr RPAREN                                                         # parenExpr
    | literal                                                                    # literalExpr
    | qualifiedName                                                              # nameExpr
    ;

caseCondition
    : WHEN expr THEN expr
    ;

functionCall
    : functionName LPAREN (DISTINCT? funcArgs | STAR)? RPAREN overClause?
    ;

funcArgs
    : expr (COMMA expr)*
    ;

overClause
    : OVER LPAREN windowSpecification RPAREN
    | OVER name
    ;

windowSpecification
    : (PARTITION BY expr (COMMA expr)*)?
      orderByClause?
    ;

functionName
    : qualifiedName
    ;

literal
    : INTEGER_LITERAL      # intLiteral
    | FLOAT_LITERAL        # floatLiteral
    | STRING_LITERAL       # strLiteral
    | TRUE_KW              # trueLiteral
    | FALSE_KW             # falseLiteral
    | NULL_KW              # nullLiteral
    | SYSDATE              # sysdateLiteral
    | SYSTIMESTAMP         # systimestampLiteral
    ;

// =========================================================================
// Names / Identifiers
// =========================================================================

qualifiedName
    : name (DOT name)*
    ;

columnList
    : columnName (COMMA columnName)*
    ;

columnName
    : name
    ;

tableRef
    : qualifiedName
    ;

/**
 * Unreserved identifier: any IDENTIFIER or QUOTED_IDENTIFIER, plus keywords
 * that are safe to use as identifiers in the Oracle v1 subset.
 */
name
    : IDENTIFIER
    | QUOTED_IDENTIFIER
    | unreservedKeyword
    ;

unreservedKeyword
    : ACTION
    | ALL
    | ALWAYS
    | ASC
    | BINARY_DOUBLE
    | BINARY_FLOAT
    | BLOB
    | BOOLEAN
    | BYTE
    | BY
    | CASCADE
    | CHAR_KW
    | CHARACTER
    | CHECK
    | CLOB
    | COMMIT_KW
    | CONNECT
    | CONSTRAINT
    | CONSTRAINTS
    | CURRENT
    | DATE_KW
    | DAY
    | DECIMAL_KW
    | DEFAULT_KW
    | DESC
    | DOUBLE
    | DROP
    | DUAL
    | ELSE
    | ESCAPE
    | FETCH
    | FIRST_KW
    | FLOAT_KW
    | FOR
    | FULL
    | GENERATED
    | GLOBAL
    | GROUP
    | HAVING
    | HOUR
    | IDENTITY
    | INT_KW
    | INTEGER_KW
    | INTERVAL
    | KEY
    | LAST_KW
    | LEFT
    | LONG_KW
    | MINUTE
    | MONTH
    | NATURAL
    | NEXT_KW
    | NCHAR
    | NCLOB
    | NO
    | NULL_KW
    | NULLS
    | NUMBER_KW
    | NUMERIC_KW
    | NVARCHAR2
    | OF
    | OFFSET
    | ONLY
    | OUTER
    | OVER
    | PARTITION
    | PRECISION
    | PRIMARY
    | PURGE
    | RAW_KW
    | REAL
    | RECURSIVE
    | REFERENCES
    | RESTRICT
    | RIGHT
    | ROLLBACK_KW
    | ROW
    | ROWID
    | ROWNUM
    | ROWS
    | SAVEPOINT
    | SECOND
    | SET
    | SMALLINT
    | START
    | SYSDATE
    | SYSTIMESTAMP
    | TABLE
    | TEMP
    | TEMPORARY
    | TIME
    | TIMESTAMP
    | TO
    | TRANSACTION
    | TRUNCATE
    | UNION
    | UNIQUE
    | USING
    | VALUES
    | VARCHAR
    | VARCHAR2
    | VARYING
    | WITH
    | WITHOUT
    | WORK
    | XMLTYPE
    | YEAR
    | ZONE
    | LOCAL
    | NATIONAL
    ;
