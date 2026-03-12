package io.github.e4c5.sqool.ast;

/** Marker interface for SQL statements in the normalized AST. */
public sealed interface Statement extends AstNode permits SelectStatement, SetOperationStatement {}
