package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Simple named table reference supported by the MySQL MVP. */
public record NamedTableReference(String name, SourceSpan sourceSpan) implements TableReference {

  public NamedTableReference {
    Objects.requireNonNull(name, "name");
  }
}
