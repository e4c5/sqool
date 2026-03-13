package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified CREATE TABLE statement AST. */
public record CreateTableStatement(
    String tableName,
    boolean temporary,
    boolean ifNotExists,
    List<ColumnDefinition> columns,
    String likeTableName,
    String tableOptions,
    SourceSpan sourceSpan)
    implements Statement {

  public CreateTableStatement {
    Objects.requireNonNull(tableName, "tableName");
    Objects.requireNonNull(columns, "columns");
    columns = List.copyOf(columns);
  }
}
