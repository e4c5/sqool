# Milestone 6: Remaining Implementation Plan

This document is the implementation plan for all items in the [SQL Parser Implementation Checklist](sql-parser-implementation-checklist.md) that are not yet complete. It is generated from the checklist state as of the post–Milestone 5 completion pass.

**Related docs:** [release-readiness.md](release-readiness.md) (release checklist), [next-milestone-checklist.md](next-milestone-checklist.md) (next-phase tracking), [post-m5-roadmap.md](post-m5-roadmap.md) (future work).

---

## 1. Status of the main checklist

Sections **1–12** of the implementation checklist are **complete**. Section **13 (Packaging and release readiness)** has one item done (dialect coverage documentation) and five remaining. Section **14 (Deferred or later-phase)** is intentionally out of scope for the next release.

---

## 2. Milestone 6: Release readiness

**Goal:** Complete the remaining §13 items so the library can be published (e.g. to Maven Central) and maintained with clear versioning and grammar refresh rules.

**Definition of done:** All five remaining §13 tasks are completed or explicitly deferred with a short rationale; release-readiness.md and this plan are updated.

### 2.1 Work breakdown

#### Task R1: Finalize module publication layout

- **Objective:** Decide which modules are published and under what Maven coordinates.
- **Tasks:**
  - [x] List candidate modules; exclude conformance, bench, grammar modules.
  - [x] Define groupId `io.github.e4c5` and artifactIds (sqool-core, sqool-ast, sqool-dialect-*).
  - [x] Configure Maven Publish in root build.gradle.kts; POM metadata, Javadoc/sources.
  - [x] Document in release-readiness.md §1.
- **Deliverable:** Documented publication layout.
- **Dependencies:** None.

#### Task R2: Define versioning strategy

- **Objective:** Adopt a versioning scheme and compatibility policy.
- **Tasks:**
  - [x] Adopt semantic versioning (MAJOR.MINOR.PATCH).
  - [x] Document breaking changes (API, AST, dialect support) in release-readiness.md §2.
  - [x] Record in CONTRIBUTING (Versioning and release).
- **Deliverable:** Versioning and compatibility policy.
- **Dependencies:** None.

#### Task R3: Publish benchmark and feature support reports

- **Objective:** Make benchmark and feature support visible for releases.
- **Tasks:**
  - [x] Benchmark reports: CI artifact; release workflow uploads jmh-results-{tag}. Feature support: link to dialect-coverage.md (release-readiness.md §4).
- **Deliverable:** Process and docs for benchmark/feature reports.
- **Dependencies:** R1 (to know what is “released”).

#### Task R4: Prepare release automation and CI quality gates

- **Objective:** Enable tag-based (or similar) release and artifact publishing.
- **Tasks:**
  - [x] Release trigger: push tag `v*`; .github/workflows/release.yml runs check + JMH, then publish (OSSRH placeholder).
  - [x] Quality gates: ./gradlew check and :sqool-bench:jmh in release workflow.
  - [x] Runbook in CONTRIBUTING and release-readiness.md §5.
- **Deliverable:** Release automation and CI gates; runbook for releasing.
- **Dependencies:** R1, R2.

#### Task R5: Define the upstream grammar refresh process

- **Objective:** Document how and when vendored grammars are updated.
- **Tasks:**
  - [x] Document who is responsible (e.g. maintainers).
  - [x] Document when to refresh (e.g. security fix, major ANTLR upgrade, or scheduled).
  - [x] Document steps: obtain upstream source, copy into grammar module, update UPSTREAM.md (commit hash, path), run build and conformance tests, run benchmarks.
  - [x] Add or point to this process in release-readiness.md and/or each grammar module’s UPSTREAM.md.
- **Deliverable:** Upstream grammar refresh process.
- **Dependencies:** None.

### 2.2 Suggested execution order for Milestone 6

1. R1 (module layout) and R2 (versioning) — can be done in parallel.
2. R5 (grammar refresh) — independent.
3. R3 (benchmark/feature reports) — after or with R1.
4. R4 (release automation) — after R1 and R2.

### 2.3 Exit criteria for Milestone 6

- Publication layout and versioning are documented and agreed.
- Benchmark and feature reporting for releases are documented (and automated where desired).
- A release can be performed using the defined process.
- Upstream grammar refresh is documented and repeatable.

---

## 3. Deferred / later-phase (Section 14)

The following items remain in the implementation checklist as **deferred or later-phase**. They are **not** part of Milestone 6. Treat them as candidate future milestones or backlog.

| Item | Description | Suggested milestone |
|------|-------------|---------------------|
| Full Oracle PL/SQL support | Extend Oracle beyond SQL-only to procedural constructs. | Future “Oracle PL/SQL” milestone |
| Comment preservation and trivia-aware AST | Capture comments and whitespace in AST for tooling. | Future “Tooling / IDE” milestone |
| Incremental parsing support | Parse only changed regions for editors. | Future “Tooling / IDE” milestone |
| SQL formatting and linting layers | Pretty-print and lint using the AST. | Future “Formatting / Linting” milestone |
| Semantic validation and catalog-aware analysis | Validate against schema/catalog. | Future “Semantic layer” milestone |
| Dialect-to-dialect transpilation | Translate SQL between dialects. | Future “Transpilation” milestone |

No implementation plan is spelled out here for §14; when the project prioritizes one of these, expand it into a dedicated milestone (e.g. “Milestone 7: Oracle PL/SQL”) with its own backlog and tasks.

---

## 4. Summary

| Category | Status | Plan |
|----------|--------|------|
| Checklist §1–§12 | Complete | — |
| Checklist §13 (release readiness) | **Complete** (R1–R5 implemented) | Milestone 6 done |
| Checklist §14 (deferred) | Not started | Backlog / future milestones (§3) |

**Milestone 6 is complete.** Publication layout, versioning, benchmark/feature reports, release workflow, and grammar refresh process are documented and configured. To publish to Maven Central, add OSSRH and signing configuration and uncomment the publish step in `.github/workflows/release.yml`.
