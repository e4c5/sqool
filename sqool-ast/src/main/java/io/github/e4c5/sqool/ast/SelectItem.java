package io.github.e4c5.sqool.ast;

/** Marker interface for supported SELECT list items. */
public sealed interface SelectItem extends AstNode
    permits AllColumnsSelectItem, ExpressionSelectItem {}
