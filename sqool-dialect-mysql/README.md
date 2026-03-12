# sqool-dialect-mysql

MySQL dialect parser module.

Current MVP scope:

- real upstream MySQL grammar integration
- SLL-first parse path with LL fallback
- normalized AST mapping for simple `SELECT ... FROM ...` statements
- syntax diagnostics for malformed input
