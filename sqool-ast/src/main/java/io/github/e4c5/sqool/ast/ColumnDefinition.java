package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified column definition for CREATE TABLE support. */
public record ColumnDefinition(
    String name, String typeName, List<String> attributes, SourceSpan sourceSpan)
    implements AstNode {

  public ColumnDefinition {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(typeName, "typeName");
    Objects.requireNonNull(attributes, "attributes");
    attributes = List.copyOf(attributes);
  }
}
