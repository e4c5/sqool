package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Derived table backed by a subquery. */
public record DerivedTableReference(
    Statement subquery, String alias, List<String> columnAliases, SourceSpan sourceSpan)
    implements TableReference {

  public DerivedTableReference {
    Objects.requireNonNull(subquery, "subquery");
    columnAliases = columnAliases == null ? List.of() : List.copyOf(columnAliases);
  }
}
