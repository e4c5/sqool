# sqool-dialect-mysql

MySQL dialect parser module.

Current MVP scope:

- real upstream MySQL grammar integration
- SLL-first parse path with LL fallback
- normalized AST mapping for:
  - `DISTINCT`
  - aliases
  - arithmetic expressions
  - aggregate and generic function calls
  - selected runtime built-in functions (`COALESCE`, `IF`, `MOD`, `DATE`, `NOW`, `CURDATE`, `CURRENT_USER`)
  - `GROUP BY`
  - `HAVING`
  - `IN` / `BETWEEN` / `LIKE`
  - qualified references
  - `WHERE`
  - explicit joins with `ON`
  - `ORDER BY`
  - numeric `LIMIT`
- syntax diagnostics for malformed input
