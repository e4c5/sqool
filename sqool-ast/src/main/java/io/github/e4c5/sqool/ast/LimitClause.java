package io.github.e4c5.sqool.ast;

/** LIMIT clause represented as row count plus optional offset. */
public record LimitClause(long rowCount, Long offset, SourceSpan sourceSpan) implements AstNode {}
