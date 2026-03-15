package io.github.e4c5.sqool.ast;

/** Identifies the kind of a raw PostgreSQL statement that was not fully mapped into the AST. */
public enum PostgresqlStatementKind {
  SELECT,
  INSERT,
  UPDATE,
  DELETE,
  CREATE_TABLE,
  DROP_TABLE,
  TRUNCATE,
  BEGIN,
  COMMIT,
  ROLLBACK,
  OTHER
}
