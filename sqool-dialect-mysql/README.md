# sqool-dialect-mysql

MySQL dialect parser module.

Current MVP scope:

- real upstream MySQL grammar integration
- SLL-first parse path with LL fallback
- normalized AST mapping for:
  - aliases
  - qualified references
  - `WHERE`
  - explicit joins with `ON`
  - `ORDER BY`
  - numeric `LIMIT`
- syntax diagnostics for malformed input
