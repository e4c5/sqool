package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified REPLACE statement AST. */
public record ReplaceStatement(
    String tableName,
    List<String> columns,
    List<List<Expression>> rows,
    List<ColumnAssignment> assignments,
    Statement sourceQuery,
    SourceSpan sourceSpan)
    implements Statement {

  public ReplaceStatement {
    Objects.requireNonNull(tableName, "tableName");
    Objects.requireNonNull(columns, "columns");
    Objects.requireNonNull(rows, "rows");
    Objects.requireNonNull(assignments, "assignments");
    columns = List.copyOf(columns);
    rows = rows.stream().map(List::copyOf).toList();
    assignments = List.copyOf(assignments);
  }
}
