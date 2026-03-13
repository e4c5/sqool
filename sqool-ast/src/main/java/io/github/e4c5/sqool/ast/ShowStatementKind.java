package io.github.e4c5.sqool.ast;

/** Supported SHOW statement kinds in the current AST slice. */
public enum ShowStatementKind {
  SHOW_COLUMNS,
  SHOW_CREATE_TABLE,
  SHOW_DATABASES,
  SHOW_TABLES
}
