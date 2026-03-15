package io.github.e4c5.sqool.ast;

import java.util.List;

/**
 * Shared builder for normalized DML statement AST nodes. Dialect mappers (e.g. Oracle, PostgreSQL)
 * parse grammar-specific context and delegate AST construction here so only grammar branching
 * remains dialect-specific.
 */
public final class DmlAstBuilder {

  private DmlAstBuilder() {}

  /**
   * Builds a normalized INSERT statement from already-parsed inputs (VALUES or SELECT path).
   * Assignments and on-duplicate are empty; dialect-specific extensions use raw fallback.
   */
  public static InsertStatement buildInsert(
      String tableName,
      List<String> columns,
      List<List<Expression>> rows,
      Statement sourceQuery,
      SourceSpan sourceSpan) {
    return new InsertStatement(
        tableName,
        columns,
        rows,
        List.of(),
        sourceQuery,
        List.of(),
        false,
        sourceSpan);
  }

  /**
   * Builds a normalized UPDATE statement from already-parsed target, assignments, and WHERE.
   */
  public static UpdateStatement buildUpdate(
      TableReference target,
      List<ColumnAssignment> assignments,
      Expression where,
      SourceSpan sourceSpan) {
    return new UpdateStatement(
        target, assignments, where, List.of(), null, false, sourceSpan);
  }

  /**
   * Builds a normalized DELETE statement from already-parsed target and WHERE.
   */
  public static DeleteStatement buildDelete(
      TableReference target, Expression where, SourceSpan sourceSpan) {
    return new DeleteStatement(target, where, List.of(), null, sourceSpan);
  }
}
