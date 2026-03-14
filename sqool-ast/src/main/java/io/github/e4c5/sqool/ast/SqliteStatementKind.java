package io.github.e4c5.sqool.ast;

/** SQLite statement kinds used by the raw-statement fallback node. */
public enum SqliteStatementKind {
  ALTER_TABLE,
  ANALYZE,
  ATTACH,
  BEGIN,
  COMMIT,
  CREATE_INDEX,
  CREATE_TABLE,
  CREATE_TRIGGER,
  CREATE_VIEW,
  CREATE_VIRTUAL_TABLE,
  DELETE,
  DETACH,
  DROP,
  INSERT,
  PRAGMA,
  REINDEX,
  RELEASE,
  ROLLBACK,
  SAVEPOINT,
  SELECT,
  UPDATE,
  VACUUM,
  OTHER
}
