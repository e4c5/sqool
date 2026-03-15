/*
 * PostgreSQL ANTLR4 Parser Grammar – sqool internal fork
 *
 * Derived from: https://github.com/antlr/grammars-v4
 * Upstream path: sql/postgresql
 * See UPSTREAM.md for full provenance and local deviations.
 *
 * Local deviations:
 *  1. Scoped to the PostgreSQL v1 subset (core DML/DDL + SELECT).
 *  2. Added Java package header for sqool namespace.
 *  3. Simplified expression grammar for improved prediction performance.
 *  4. RETURNING clause added as a first-class construct.
 */

// $antlr-format alignTrailingComments on, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments off, useTab off
// $antlr-format allowShortRulesOnASingleLine off, allowShortBlocksOnASingleLine on, alignSemicolons ownLine
// $antlr-format alignColons hanging

parser grammar PostgreSQLParser;

options {
    tokenVocab = PostgreSQLLexer;
}

@header {
package io.github.e4c5.sqool.grammar.postgresql.generated;
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
    | beginStatement
    | commitStatement
    | rollbackStatement
    ;

// =========================================================================
// SELECT
// =========================================================================

selectStatement
    : withClause? queryExpression orderByClause? limitClause? offsetClause? fetchClause?
    ;

withClause
    : WITH RECURSIVE? cteDefinition (COMMA cteDefinition)*
    ;

cteDefinition
    : cteName = name AS LPAREN selectStatement RPAREN
    ;

queryExpression
    : queryTerm (setOperator queryTerm)*
    ;

setOperator
    : UNION ALL?
    | INTERSECT ALL?
    | EXCEPT ALL?
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
    : CROSS JOIN tablePrimary                                           # crossJoin
    | NATURAL joinKind? JOIN tablePrimary                               # naturalJoin
    | joinKind? JOIN tablePrimary (ON expr | USING LPAREN columnList RPAREN)  # qualifiedJoin
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
// ORDER BY / LIMIT / OFFSET / FETCH
// =========================================================================

orderByClause
    : ORDER BY orderByItem (COMMA orderByItem)*
    ;

orderByItem
    : expr direction = (ASC | DESC)? (NULLS (FIRST_KW | LAST_KW))?
    ;

limitClause
    : LIMIT (expr | ALL)
    ;

offsetClause
    : OFFSET expr (ROW | ROWS)?
    ;

fetchClause
    : FETCH (FIRST_KW | NEXT_KW) expr? (ROW | ROWS) ONLY
    ;

// =========================================================================
// INSERT
// =========================================================================

insertStatement
    : INSERT INTO qualifiedName (AS? alias = name)? (LPAREN columnList RPAREN)?
      insertSource
      onConflictClause?
      returningClause?
    ;

insertSource
    : VALUES rowValues (COMMA rowValues)*   # insertValues
    | selectStatement                       # insertSelect
    | DEFAULT_KW VALUES                     # insertDefaults
    ;

rowValues
    : LPAREN insertExpr (COMMA insertExpr)* RPAREN
    ;

insertExpr
    : DEFAULT_KW        # defaultExpr
    | expr              # valueExpr
    ;

onConflictClause
    : ON CONFLICT conflictTarget? conflictAction
    ;

conflictTarget
    : LPAREN columnList RPAREN
    ;

conflictAction
    : DO NOTHING
    | DO UPDATE SET setClauseList whereClause?
    ;

returningClause
    : RETURNING selectList
    ;

// =========================================================================
// UPDATE
// =========================================================================

updateStatement
    : UPDATE ONLY? qualifiedName (AS? alias = name)?
      SET setClauseList
      fromClause?
      whereClause?
      returningClause?
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
    : DELETE FROM ONLY? qualifiedName (AS? alias = name)?
      (USING tableReference (COMMA tableReference)*)?
      whereClause?
      returningClause?
    ;

// =========================================================================
// CREATE TABLE
// =========================================================================

createTableStatement
    : CREATE (TEMP | TEMPORARY)? TABLE ifNotExists? qualifiedName
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
    | FOREIGN KEY LPAREN columnList RPAREN REFERENCES qualifiedName (LPAREN columnList RPAREN)? referentialAction*
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
    | GENERATED (ALWAYS | BY DEFAULT_KW) AS IDENTITY (LPAREN sequenceOption* RPAREN)?
    | CHECK LPAREN expr RPAREN
    ;

sequenceOption
    : name expr
    ;

// =========================================================================
// DROP TABLE
// =========================================================================

dropTableStatement
    : DROP TABLE ifExists? qualifiedName (COMMA qualifiedName)* (CASCADE | RESTRICT)?
    ;

ifExists
    : IF_KW EXISTS
    ;

// =========================================================================
// TRUNCATE
// =========================================================================

truncateStatement
    : TRUNCATE TABLE? qualifiedName (COMMA qualifiedName)*
    ;

// =========================================================================
// Transaction control
// =========================================================================

beginStatement
    : BEGIN_KW (WORK | TRANSACTION)?
    ;

commitStatement
    : COMMIT_KW (WORK | TRANSACTION)?
    ;

rollbackStatement
    : ROLLBACK_KW (WORK | TRANSACTION)?
    ;

// =========================================================================
// Type names
// =========================================================================

typeName
    : baseTypeName (LBRACKET INTEGER_LITERAL? RBRACKET)*
    | CHARACTER VARYING (LPAREN INTEGER_LITERAL RPAREN)?
    | DOUBLE PRECISION
    | TIMESTAMP (LPAREN INTEGER_LITERAL RPAREN)? (WITH | WITHOUT) TIME ZONE
    | TIME (LPAREN INTEGER_LITERAL RPAREN)? (WITH | WITHOUT) TIME ZONE
    | INTERVAL intervalFields?
    ;

baseTypeName
    : INT_KW
    | INTEGER_KW
    | BIGINT
    | SMALLINT
    | SERIAL
    | BIGSERIAL
    | SMALLSERIAL
    | BOOLEAN
    | TEXT
    | CHAR_KW (LPAREN INTEGER_LITERAL RPAREN)?
    | CHARACTER (LPAREN INTEGER_LITERAL RPAREN)?
    | VARCHAR (LPAREN INTEGER_LITERAL RPAREN)?
    | NUMERIC_KW (LPAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RPAREN)?
    | DECIMAL_KW (LPAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RPAREN)?
    | REAL
    | FLOAT_KW (LPAREN INTEGER_LITERAL RPAREN)?
    | DOUBLE PRECISION
    | DATE_KW
    | TIME
    | TIMESTAMP
    | INTERVAL
    | UUID
    | JSON
    | JSONB
    | name
    ;

intervalFields
    : YEAR
    | MONTH
    | DAY
    | HOUR
    | MINUTE
    | SECOND
    | YEAR TO MONTH
    | DAY TO HOUR
    | DAY TO MINUTE
    | DAY TO SECOND
    | HOUR TO MINUTE
    | HOUR TO SECOND
    | MINUTE TO SECOND
    ;

// =========================================================================
// Expressions
// =========================================================================

/**
 * Precedence (highest first, per ANTLR4 left-recursive convention):
 *  1. :: CAST
 *  2. Unary - + ~
 *  3. * / %
 *  4. + -
 *  5. || (concat)
 *  6. Bitwise & | ^
 *  7. Comparison < > <= >= = <>
 *  8. IS / IS NOT / ISNULL / NOTNULL
 *  9. LIKE / ILIKE / SIMILAR TO
 * 10. BETWEEN / NOT BETWEEN
 * 11. IN / NOT IN
 * 12. NOT (unary logical)
 * 13. AND
 * 14. OR  (lowest)
 */
expr
    : expr CAST_OP typeName                                                              # castExpr
    | op = (MINUS | PLUS | TILDE) expr                                                  # unaryExpr
    | expr op = (STAR | SLASH | PERCENT) expr                                            # mulExpr
    | expr op = (PLUS | MINUS) expr                                                      # addExpr
    | expr CONCAT expr                                                                   # concatExpr
    | expr op = (AMP | PIPE | CARET) expr                                                # bitwiseExpr
    | expr op = (LT | GT | LTE | GTE | EQ | NEQ) expr                                   # compExpr
    | expr IS NOT? NULL_KW                                                               # isNullExpr
    | expr IS NOT? DISTINCT FROM expr                                                    # isDistinctExpr
    | expr NOT? BETWEEN symmetricClause? expr AND expr                                   # betweenExpr
    | expr NOT? (LIKE | ILIKE) expr (ESCAPE expr)?                                       # likeExpr
    | expr NOT? SIMILAR TO expr (ESCAPE expr)?                                           # similarExpr
    | expr NOT? IN_KW LPAREN (expr (COMMA expr)* | selectStatement) RPAREN              # inExpr
    | NOT expr                                                                           # notExpr
    | expr AND expr                                                                      # andExpr
    | expr OR expr                                                                       # orExpr
    | CAST_KW LPAREN expr AS typeName RPAREN                                             # sqlCastExpr
    | EXISTS LPAREN selectStatement RPAREN                                               # existsExpr
    | CASE_KW caseCondition* (ELSE expr)? END_KW                                        # searchedCaseExpr
    | CASE_KW expr caseCondition* (ELSE expr)? END_KW                                   # simpleCaseExpr
    | functionCall                                                                       # funcCallExpr
    | LPAREN selectStatement RPAREN                                                      # scalarSubquery
    | ARRAY LBRACKET (expr (COMMA expr)*)? RBRACKET                                     # arrayExpr
    | LPAREN expr RPAREN                                                                 # parenExpr
    | literal                                                                            # literalExpr
    | qualifiedName                                                                      # nameExpr
    ;

symmetricClause
    : SYMMETRIC
    | ASYMMETRIC
    ;

caseCondition
    : WHEN expr THEN expr
    ;

functionCall
    : functionName LPAREN (DISTINCT? funcArgs | STAR)? RPAREN filterClause? overClause?
    ;

funcArgs
    : expr (COMMA expr)*
    ;

filterClause
    : FILTER LPAREN WHERE expr RPAREN
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
    | DOLLAR_STRING        # dollarStrLiteral
    | ESCAPE_STRING        # escapeStrLiteral
    | UNICODE_STRING       # unicodeStrLiteral
    | BIT_STRING           # bitStrLiteral
    | HEX_STRING           # hexStrLiteral
    | TRUE_KW              # trueLiteral
    | FALSE_KW             # falseLiteral
    | NULL_KW              # nullLiteral
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

tableName
    : qualifiedName
    ;

tableRef
    : qualifiedName
    ;

/**
 * Unreserved identifier: any IDENTIFIER or QUOTED_IDENTIFIER, plus keywords
 * that are safe to use as identifiers in the v1 subset.
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
    | ARRAY
    | ASC
    | ASYMMETRIC
    | BEGIN_KW
    | BIGINT
    | BIGSERIAL
    | BOOLEAN
    | BY
    | CASCADE
    | CAST_KW
    | CHAR_KW
    | CHARACTER
    | CHECK
    | COMMIT_KW
    | CONFLICT
    | CONSTRAINT
    | DATE_KW
    | DAY
    | DECIMAL_KW
    | DEFAULT_KW
    | DESC
    | DO
    | DOUBLE
    | DROP
    | ELSE
    | ESCAPE
    | FETCH
    | FILTER
    | FIRST_KW
    | FLOAT_KW
    | FOR
    | FUNCTION
    | GENERATED
    | GROUP
    | HAVING
    | HOUR
    | IDENTITY
    | INT_KW
    | INTEGER_KW
    | INTERVAL
    | JSON
    | JSONB
    | KEY
    | LAST_KW
    | LIMIT
    | MINUTE
    | MONTH
    | NATURAL
    | NEXT_KW
    | NO
    | NOTHING
    | NULLS
    | NUMERIC_KW
    | OF
    | OFFSET
    | ONLY
    | OVER
    | PARTITION
    | PRECISION
    | PRIMARY
    | REAL
    | RECURSIVE
    | REFERENCES
    | RESTRICT
    | RETURNING
    | ROLLBACK_KW
    | ROW
    | ROWS
    | SECOND
    | SERIAL
    | SET
    | SIMILAR
    | SMALLINT
    | SMALLSERIAL
    | SYMMETRIC
    | TABLE
    | TEMP
    | TEMPORARY
    | TEXT
    | TIME
    | TIMESTAMP
    | TO
    | TRANSACTION
    | TRUNCATE
    | UNION
    | UNIQUE
    | USING
    | UUID
    | VALUES
    | VARCHAR
    | VARYING
    | WITH
    | WITHOUT
    | WORK
    | YEAR
    | ZONE
    | INTERSECT
    | EXCEPT
    ;
