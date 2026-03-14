package io.github.e4c5.sqool.ast;

/** Marker interface for supported table references. */
public sealed interface TableReference extends AstNode
    permits DerivedTableReference, JoinTableReference, NamedTableReference {}
