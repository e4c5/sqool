# Release Readiness Checklist

This document tracks items that should be completed or decided before publishing sqool to a public repository (e.g. Maven Central). It aligns with [SQL Parser Implementation Checklist §13](sql-parser-implementation-checklist.md) and [Next Milestone Checklist §4](next-milestone-checklist.md). For the full implementation plan (tasks, order, exit criteria) for these items, see [Milestone 6: Remaining Implementation Plan](milestone-6-remaining-implementation-plan.md).

## Module publication layout

- [ ] Decide which modules are published (e.g. `sqool-core`, `sqool-ast`, `sqool-dialect-*` only; exclude `sqool-conformance`, `sqool-bench` from publication).
- [ ] Define Maven group and artifact coordinates (e.g. `io.github.e4c5:sqool-core:1.0.0`).
- [ ] Configure publishing in Gradle (Maven Publish plugin, POM metadata, optional Javadoc/sources).

## Versioning strategy

- [ ] Adopt a versioning scheme (e.g. semantic versioning).
- [ ] Document compatibility policy (e.g. what constitutes a breaking change for the public API and AST).

## Dialect coverage documentation

- [ ] Confirm [dialect-coverage.md](dialect-coverage.md) is the single source of truth for “what is supported per dialect.”
- [ ] Link dialect coverage from README and any release notes or changelog.

## Benchmark and feature reports

- [ ] Decide whether to publish benchmark reports with releases (e.g. in repo, or CI-only).
- [ ] Document feature support (e.g. “v1 supported statement types”) for release notes.

## Release automation and CI

- [ ] Define release process (e.g. tag-based: `v1.0.0` triggers publish).
- [ ] Configure CI to publish artifacts (e.g. to Maven Central via OSSRH) on release tags.
- [ ] Add quality gates that must pass before release (e.g. `check`, benchmarks, no known regressions).

## Upstream grammar refresh

- [ ] Document the process for updating vendored grammars: who updates, when, how to run (e.g. copy from `antlr/grammars-v4`, run build, run conformance), and how to validate (tests, manual smoke).
- [ ] Ensure each grammar module’s `UPSTREAM.md` is updated when the vendored revision changes.

---

*Complete or defer each item; update this doc and [next-milestone-checklist.md](next-milestone-checklist.md) when items are done or moved to a later phase.*
