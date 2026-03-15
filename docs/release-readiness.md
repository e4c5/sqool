# Release Readiness Checklist

This document tracks items that should be completed or decided before publishing sqool to a public repository (e.g. Maven Central). It aligns with [SQL Parser Implementation Checklist §13](sql-parser-implementation-checklist.md) and [Milestone 6: Remaining Implementation Plan](milestone-6-remaining-implementation-plan.md).

---

## 1. Module publication layout

**Decided.** The following modules are published to Maven:

| Module | ArtifactId | Purpose |
|--------|------------|---------|
| Core API and runtime | `sqool-core` | Public parser API, parse options, diagnostics, source spans, shared parser utilities. |
| AST model | `sqool-ast` | Normalized AST nodes, dialect raw statement wrappers. |
| MySQL grammar | `sqool-grammar-mysql` | Vendored MySQL ANTLR grammar and generated parser/lexer; required by the MySQL dialect. |
| PostgreSQL grammar | `sqool-grammar-postgresql` | Vendored PostgreSQL ANTLR grammar and generated parser/lexer. |
| Oracle grammar | `sqool-grammar-oracle` | Vendored Oracle SQL ANTLR grammar and generated parser/lexer. |
| SQLite grammar | `sqool-grammar-sqlite` | Vendored SQLite ANTLR grammar and generated parser/lexer. |
| MySQL dialect | `sqool-dialect-mysql` | MySQL parser and AST mapping. Depends on `sqool-grammar-mysql`. |
| PostgreSQL dialect | `sqool-dialect-postgresql` | PostgreSQL parser and AST mapping. Depends on `sqool-grammar-postgresql`. |
| Oracle dialect | `sqool-dialect-oracle` | Oracle SQL parser and AST mapping. Depends on `sqool-grammar-oracle`. |
| SQLite dialect | `sqool-dialect-sqlite` | SQLite parser and AST mapping. Depends on `sqool-grammar-sqlite`. |

**Not published:** `sqool-conformance`, `sqool-bench` — they are test/benchmark only.

**Coordinates:** `io.github.e4c5:sqool-core:VERSION`, `io.github.e4c5:sqool-ast:VERSION`, `io.github.e4c5:sqool-dialect-mysql:VERSION`, etc.

**Gradle:** Maven Publish is configured in the root `build.gradle.kts` for the publishable modules. Run `./gradlew publishToMavenLocal` to publish to the local Maven repository, or configure a remote repository and run `publish`.

- [x] Decide which modules are published
- [x] Define Maven group and artifact coordinates
- [x] Configure publishing in Gradle (Maven Publish plugin, POM metadata, Javadoc/sources jars)

---

## 2. Versioning strategy

**Adopted: Semantic Versioning (SemVer)** — `MAJOR.MINOR.PATCH`.

| Change type | When to increment | Compatibility |
|-------------|-------------------|---------------|
| **MAJOR** | Breaking changes to the public API or AST. | Consumers must migrate. |
| **MINOR** | New dialect support, new normalized statement/expression types, new public API in a backward-compatible way. | Backward compatible. |
| **PATCH** | Bug fixes, performance improvements, documentation, internal refactors with no API/AST change. | Backward compatible. |

**Breaking changes include (but are not limited to):**

- Removing or renaming public types or methods in `sqool-core` or the dialect facades.
- Changing the shape of `ParseResult`, `ParseSuccess`, `ParseFailure`, `ParseOptions`, or `SyntaxDiagnostic`.
- Removing or renaming AST node types in `sqool-ast`, or changing the meaning of existing AST nodes in a way that breaks consumers.
- Dropping support for a previously supported dialect or Java version.

**Pre-release:** Use `-SNAPSHOT` for development (e.g. `0.1.0-SNAPSHOT`). For release candidates, use `-RC1`, `-RC2` (e.g. `1.0.0-RC1`).

- [x] Adopt a versioning scheme (semver)
- [x] Document compatibility policy

---

## 3. Dialect coverage documentation

- [x] [dialect-coverage.md](dialect-coverage.md) is the single source of truth for supported constructs per dialect.
- [x] README links to the Dialect Coverage Matrix and usage examples.

---

## 4. Benchmark and feature reports

**Benchmark reports:** Stored as **CI artifacts only** (see [CONTRIBUTING.md](../CONTRIBUTING.md)). On each push to `main`, the CI runs JMH and uploads the `jmh-results` artifact (JSON). For releases, attach or link to the benchmark artifact from the release tag run, or document in release notes that latest benchmarks are available from the `main` branch CI artifacts.

**Feature support:** For each release, document supported statement types and dialect coverage in the release notes. Link to [dialect-coverage.md](dialect-coverage.md) as the authoritative matrix. A one-paragraph “Feature support” summary can be copied from the README “Current parser coverage” section.

- [x] Decide: benchmark reports are CI-only; optional attachment to release.
- [x] Document: feature support = dialect-coverage.md + release-note summary.

---

## 5. Release automation and CI

**Release process (tag-based):**

1. Ensure `main` is green (CI: build, test, check).
2. Update version in `build.gradle.kts` (and any other places) from `-SNAPSHOT` to the release version (e.g. `1.0.0`).
3. Commit the version bump: e.g. `Release 1.0.0`.
4. Tag the commit: e.g. `v1.0.0`.
5. Push the tag. The **release** workflow runs: full check, then publish (if configured).
6. After publishing, bump version to the next development version (e.g. `1.0.1-SNAPSHOT`) and push.

**CI quality gates before release:** The release workflow runs `./gradlew check` (which includes tests, checkstyle, spotless). Optionally run `./gradlew :sqool-bench:jmh` locally to confirm no performance regressions.

**Publishing to Maven Central:** Configure the `publish` task to use OSSRH (Sonatype) and GPG signing. Add repository URL and credentials via environment variables or Gradle properties (do not commit secrets). See the [release workflow](.github/workflows/release.yml) for the trigger; complete the publish step with your OSSRH and signing setup.

- [x] Define release process (tag-based)
- [x] Add release workflow (check + publish placeholder)
- [x] Document quality gates

---

## 6. Upstream grammar refresh

**Process:** Documented in [upstream-grammar-refresh.md](upstream-grammar-refresh.md). Summary:

- **Who:** Maintainers.
- **When:** On security or critical bug fix in upstream grammar; or when upgrading ANTLR; or on a scheduled review.
- **How:** Copy/cherry-pick from upstream (e.g. `antlr/grammars-v4`), update the grammar module’s `UPSTREAM.md` (commit hash, path), run `./gradlew build` and conformance tests, run benchmarks. No automated sync; manual copy and validation.

- [x] Document the process (see upstream-grammar-refresh.md)
- [x] Require UPSTREAM.md update when vendored revision changes

---

*Last updated: Milestone 6 implementation.*
