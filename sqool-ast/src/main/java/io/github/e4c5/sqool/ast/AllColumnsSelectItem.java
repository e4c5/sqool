package io.github.e4c5.sqool.ast;

/** Represents a `*` projection. */
public record AllColumnsSelectItem(SourceSpan sourceSpan) implements SelectItem {}
