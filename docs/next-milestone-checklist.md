# Next Milestone Checklist

This checklist covers the **next phase** of work after Milestones 0–5: follow-ups, optional cleanup, and steps toward release. Use it for sprint planning and progress tracking. See also [Post-M5 Roadmap](post-m5-roadmap.md) for context and [SQL Parser Implementation Checklist](sql-parser-implementation-checklist.md) for the full high-level plan.

## Scope of this milestone

- **M5 follow-ups** — Normalization and consistency improvements (non-blocking but high value).
- **Optional M0/M2 cleanup** — Documentation and baseline hygiene.
- **Implementation checklist alignment** — Remaining items from §11–§13 (performance reporting, quality gates, release readiness).
- **No new dialect** — This milestone does not add a new SQL dialect.

## 1. M5 follow-ups (normalization)

- [x] **SQLite JOIN normalization** — Extend SQLite v1 expression mapper so JOIN queries and complex WHERE produce `SelectStatement` + `JoinTableReference` instead of `SqliteRawStatement`. Add/update golden tests. *(Already implemented; dialect-coverage and checklist updated.)*
- [x] **PostgreSQL / Oracle DML normalization** — Map INSERT, UPDATE, DELETE to shared AST nodes (e.g. `InsertStatement`, `UpdateStatement`, `DeleteStatement`) where practical; add golden tests. *(Already implemented; dialect-coverage updated.)*
- [x] **NATURAL JOIN** — Normalize NATURAL JOIN to a shared AST representation across dialects (or document as raw-only and close as deferred). *(Already normalized: all four dialects produce `JoinTableReference` with `natural == true`. Dialect-coverage and Known limitations updated.)*

## 2. Optional M2 cleanup

- [x] **M2-1 design note** — Write a short design note for shared parser/mapper abstractions (`AntlrParserFacade` or equivalent, shared error/mapping helpers); refactor MySQL to use them; document dialect-specific vs shared. *(Design note added: [parser-abstractions.md](parser-abstractions.md). Refactor deferred.)*
- [x] **M2-6 SQLite baseline** — Capture baseline SQLite benchmark results in a reproducible form (CI artifact or committed report). *(Documented in [benchmarks.md](benchmarks.md): same capture process for all dialects; CI stores `jmh-results` artifact.)*
- [x] **M2-7 naming/API review** — Review naming, packaging, and shared helper APIs; document findings in CONTRIBUTING or a short doc. *(CONTRIBUTING already has Module and naming conventions, Shared helpers; no change needed.)*

## 3. Optional M0 cleanup

- [x] **Placeholder modules** — Add `src/main` and `src/test` placeholders (and smoke tests) for any grammar/dialect modules that still lack them. *(All modules have real source; placeholders superseded by full implementation.)*
- [x] **Documentation** — Document: grammar generation workflow, benchmark reporting policy, dependency upgrade policy, contributor expectations (README or CONTRIBUTING). *(Documented in [CONTRIBUTING.md](../CONTRIBUTING.md).)*
- [x] **Dependency locking** — Add dependency locking or equivalent for build-critical dependencies. *(Root `build.gradle.kts` has `dependencyLocking { lockAllConfigurations() }`. Run `./gradlew dependencies --write-locks` and commit lockfiles for reproducible builds.)*

## 4. Implementation checklist carryover (§11–§13)

### Performance and benchmarks (§11)

- [x] **Publish benchmark results** — Publish benchmark results in a reproducible form (CI artifact, report, or documented capture process). See [Benchmarks](benchmarks.md). *(CI stores `jmh-results` artifact; capture and comparison documented in benchmarks.md.)*

### Conformance and quality (§12)

- [x] **Quality gates** — Define quality gates for adding new syntax coverage (e.g. conformance test required, regression test for bugs). *(Added to [CONTRIBUTING.md](../CONTRIBUTING.md): Quality gates for new syntax and bug fixes.)*

### Packaging and release readiness (§13)

Tracked in [release-readiness.md](release-readiness.md). Complete or defer when preparing to publish.

- [ ] **Module publication layout** — Finalize which modules are published and under what coordinates.
- [ ] **Versioning strategy** — Define versioning strategy (e.g. semver) and compatibility policy.
- [ ] **Dialect coverage docs** — Finalize documentation for supported dialect coverage (dialect-coverage.md is the source; ensure it is the single place or linked from release notes).
- [ ] **Benchmark and feature reports** — Publish or document benchmark and feature support reports for releases.
- [ ] **Release automation and CI** — Prepare release automation and CI quality gates (e.g. tag-based release, artifact publishing).
- [ ] **Upstream grammar refresh** — Define the process for refreshing vendored grammars from upstream (who, when, how to run and validate).

## 5. Definition of done for this milestone

This milestone is **done** when:

- At least the chosen M5 follow-up items (e.g. SQLite JOIN and/or PG/Oracle DML) are implemented and tested, **or** the team explicitly defers them and records that in the roadmap.
- Remaining implementation checklist items from §11–§13 that are in scope are either completed or moved to a later phase with a short note.
- The [Post-M5 Roadmap](post-m5-roadmap.md) and, if needed, [SQL Parser Implementation Checklist](sql-parser-implementation-checklist.md) are updated to reflect what was done and what is deferred.

Optional M0/M2 and §13 items can be completed in this milestone or in a follow-on “release prep” phase; the definition of done above does not require every optional box to be checked.

## 6. Suggested order of execution

1. Pick one or two M5 follow-ups (e.g. SQLite JOIN, then PG/Oracle DML) and implement.
2. Add/update golden tests and dialect-coverage docs.
3. Tackle “Publish benchmark results” and “Quality gates” from §11–§12.
4. Then optional M0/M2 cleanup and §13 release-readiness items as capacity allows.

---

*This checklist is the single place to track “next milestone” work. Update it as items are completed or intentionally deferred.*
