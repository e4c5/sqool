# sqool-dialect-mysql

MySQL dialect parser module.

Current MVP scope:

- real upstream MySQL grammar integration
- SLL-first parse path with LL fallback
- normalized AST mapping for:
  - `CREATE TABLE`
  - `CREATE DATABASE`
  - `DISTINCT`
  - `DELETE`
  - `DROP TABLE`
  - `INSERT`
  - aliases
  - arithmetic expressions
  - aggregate and generic function calls
  - selected runtime built-in functions (`COALESCE`, `IF`, `MOD`, `DATE`, `NOW`, `CURDATE`, `CURRENT_USER`)
  - derived tables
  - `GROUP BY`
  - `HAVING`
  - `IN` / `BETWEEN` / `LIKE`
  - qualified references
  - `REPLACE`
  - script mode for mixed multi-statement SELECT/DML/DDL batches
  - `SHOW COLUMNS`
  - `SHOW CREATE TABLE`
  - `SHOW DATABASES`
  - `SHOW TABLES`
  - `TRUNCATE TABLE`
  - `UNION` / `UNION ALL`
  - `UPDATE`
  - joins with `USING`
  - `WHERE`
  - explicit joins with `ON`
  - `ORDER BY`
  - numeric `LIMIT`
- valid but not-yet-normalized MySQL statements fall back to `MySqlRawStatement`
- syntax diagnostics for malformed input
