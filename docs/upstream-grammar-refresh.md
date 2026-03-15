# Upstream Grammar Refresh Process

This document describes how to update the vendored ANTLR grammars from their upstream sources. Each grammar module has an `UPSTREAM.md` that records the source repository, path, and commit.

## Who and when

- **Who:** Maintainers. Anyone proposing a grammar update should follow this process and update the relevant `UPSTREAM.md`.
- **When:**
  - Security or critical bug fix in the upstream grammar.
  - ANTLR version upgrade that requires grammar changes.
  - Scheduled review (e.g. yearly) to pull in upstream improvements.
  - Adding support for new syntax that already exists upstream.

## Per-dialect upstream locations

| Dialect | Upstream repo | Upstream path | Local module |
|---------|----------------|---------------|--------------|
| MySQL | [antlr/grammars-v4](https://github.com/antlr/grammars-v4) | `sql/mysql/Oracle` | `sqool-grammar-mysql` |
| SQLite | See sqool-grammar-sqlite/UPSTREAM.md | — | `sqool-grammar-sqlite` |
| PostgreSQL | antlr/grammars-v4 | `sql/postgresql` | `sqool-grammar-postgresql` |
| Oracle | antlr/grammars-v4 | `sql/plsql` | `sqool-grammar-oracle` |

Always check the module’s `UPSTREAM.md` for the exact path and any local notes.

## Steps to refresh a grammar

1. **Identify the upstream source and revision.**  
   Clone or fetch the upstream repo (e.g. `git clone https://github.com/antlr/grammars-v4`) and note the commit hash you want (e.g. latest `main`, or a specific fix).

2. **Copy the grammar files.**  
   Copy the relevant `.g4` files from the upstream path into the module’s `src/main/antlr/` (or the path used by that module). Preserve any local structure (e.g. package directories) required by the build.

3. **Apply local patches if needed.**  
   Re-apply any local changes the project uses (documented in `UPSTREAM.md` or in code comments). If the upstream change obsoletes a patch, remove it and document.

4. **Update UPSTREAM.md.**  
   Set the new commit hash, date, and a short note (e.g. “Pull in fix for X”). List any new local deviations.

5. **Build and generate.**  
   Run:
   ```bash
   ./gradlew :sqool-grammar-<dialect>:generateGrammarSource
   ./gradlew build
   ```
   Fix any compilation or generation errors.

6. **Run tests.**  
   Run the full test suite, including the dialect’s conformance tests:
   ```bash
   ./gradlew check
   ./gradlew :sqool-conformance:test
   ```

7. **Run benchmarks (optional but recommended).**  
   Ensure no large performance regression:
   ```bash
   ./gradlew :sqool-bench:jmh
   ```

8. **Commit.**  
   Commit the updated grammar files, generated sources (if committed), and `UPSTREAM.md` with a message like: `chore(grammar-mysql): refresh from grammars-v4 abc1234`.

## Validation checklist

- [ ] Build passes: `./gradlew build`
- [ ] All tests pass: `./gradlew check`
- [ ] Conformance tests pass for the updated dialect
- [ ] UPSTREAM.md updated with new commit and any new deviations
- [ ] No unintended changes to other modules

## Notes

- **No automated sync.** We do not automatically pull from upstream. Every refresh is a deliberate copy-and-validate step.
- **Generated sources.** Some modules commit generated parser/lexer code; others generate only at build time. Follow the existing convention for that module when refreshing.
- **Conflicts.** If upstream has diverged significantly from our vendored copy, consider a side-by-side diff and document all intentional local changes in `UPSTREAM.md`.
