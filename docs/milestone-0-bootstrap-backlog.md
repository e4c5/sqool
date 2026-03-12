# Milestone 0 Bootstrap Backlog

## Status

- Draft: v0.1
- Scope: Checklist section 1 - Foundation and project bootstrap
- Goal: define an executable backlog for standing up the project baseline

## 1. Purpose

This document expands the "Foundation and project bootstrap" checklist into a concrete implementation backlog. It is intended to be detailed enough to drive the first implementation pass without yet locking every later-phase design decision.

Milestone 0 should leave the repository in a state where:

- the project builds on Java 25,
- ANTLR generation works,
- tests run,
- benchmarks run,
- modules are wired correctly, and
- the repository is ready for dialect implementation work.

## 2. Recommended baseline decisions

These are the recommended defaults for Milestone 0.

### 2.1 Build tool recommendation

**Selected choice: Gradle**

Rationale:

- strong multi-module ergonomics
- built-in ANTLR plugin support
- straightforward Java toolchain support for Java 25
- good fit for JMH integration
- better incremental-build ergonomics than the ANTLR Maven plugin path

Milestone 0 has been implemented with Gradle.

### 2.2 Build structure recommendation

Use a multi-module layout with a thin root project and focused subprojects:

- `sqool-core`
- `sqool-ast`
- `sqool-grammar-mysql`
- `sqool-grammar-postgresql`
- `sqool-grammar-oracle`
- `sqool-grammar-sqlite`
- `sqool-dialect-mysql`
- `sqool-dialect-postgresql`
- `sqool-dialect-oracle`
- `sqool-dialect-sqlite`
- `sqool-conformance`
- `sqool-bench`

### 2.3 Code generation recommendation

- Commit grammar files.
- Generate ANTLR sources during the build.
- Do not hand-edit generated sources.

### 2.4 Testing recommendation

Start with:

- JUnit Jupiter for tests
- simple golden-file support for future AST assertions

Keep the bootstrap test stack small. Broader test tooling can be added later.

### 2.5 Benchmarking recommendation

Use JMH in a dedicated benchmark module from the start so performance work does not become an afterthought.

### 2.6 Quality tooling recommendation

Keep bootstrap quality tooling minimal but useful:

- formatter
- basic style/static checks
- CI build verification

The initial goal is consistency, not maximum enforcement.

## 3. Milestone 0 definition of done

Milestone 0 is complete when all of the following are true:

1. The repository builds from a clean checkout on Java 25.
2. The multi-module structure is in place and validated.
3. ANTLR code generation is working in at least one grammar module.
4. Unit tests can run from the root build.
5. JMH benchmarks can run from the benchmark module.
6. Dependency versions are centrally pinned and documented.
7. Basic CI checks run successfully.
8. Repository conventions are documented well enough for follow-on implementation.

## 4. Work breakdown structure

## Epic B0-1: Build tool and repository conventions

### Objective

Choose and install the build baseline that all other work depends on.

### Tasks

- [ ] Decide and record the build tool choice
- [ ] Add the build wrapper to the repository
- [ ] Create root build files
- [ ] Create shared version and plugin management
- [ ] Define repository naming and module naming conventions
- [ ] Document root build commands for developers and CI

### Deliverables

- root build descriptor
- wrapper scripts
- shared version catalog or equivalent dependency management
- brief bootstrap/build usage notes

### Dependencies

- none

### Acceptance criteria

- The root build command works from a clean checkout.
- All subprojects can be discovered from the root build.

## Epic B0-2: Multi-module project skeleton

### Objective

Create the project structure needed for clean separation between API, AST, grammars, dialects, tests, and benchmarks.

### Tasks

- [ ] Add module declarations to the root build
- [ ] Create directory structure for each planned module
- [ ] Add placeholder source sets for main and test code
- [ ] Add placeholder package structure
- [ ] Add minimal per-module build files where needed
- [ ] Verify inter-module dependency wiring

### Deliverables

- module directories
- registered subprojects
- initial dependency graph

### Dependencies

- B0-1

### Acceptance criteria

- Each module resolves correctly in the build.
- The dependency graph reflects the intended architecture.

## Epic B0-3: Java 25 baseline

### Objective

Lock the project onto Java 25 in a reproducible way.

### Tasks

- [ ] Configure Java 25 toolchains
- [ ] Set source and target compatibility rules
- [ ] Set compiler flags shared across modules
- [ ] Decide whether preview features are disabled by default
- [ ] Add a build check that fails fast on the wrong Java version

### Deliverables

- shared Java toolchain configuration
- root compiler configuration

### Dependencies

- B0-1

### Acceptance criteria

- The build uses Java 25 consistently.
- Preview features are not required for Milestone 0.

## Epic B0-4: ANTLR integration baseline

### Objective

Prove that ANTLR code generation works within the chosen multi-module layout.

### Tasks

- [ ] Add ANTLR plugin configuration to grammar modules
- [ ] Configure grammar source directories
- [ ] Configure generated-source output directories
- [ ] Validate Java target generation
- [ ] Add a trivial smoke grammar or seed one real grammar module
- [ ] Verify generated sources compile through the normal build
- [ ] Document the grammar generation workflow

### Deliverables

- ANTLR plugin configuration
- at least one working grammar generation path
- grammar generation notes

### Dependencies

- B0-1
- B0-2
- B0-3

### Acceptance criteria

- Running the build generates ANTLR sources automatically.
- Generated sources compile without manual steps.

## Epic B0-5: Testing baseline

### Objective

Establish the minimal automated test platform used by all later modules.

### Tasks

- [ ] Add JUnit configuration
- [ ] Add one smoke test per core bootstrap module
- [ ] Add shared test utility conventions if needed
- [ ] Confirm test discovery works from the root build
- [ ] Confirm CI can run tests without custom local setup

### Deliverables

- test dependencies
- smoke tests
- root test execution path

### Dependencies

- B0-1
- B0-2
- B0-3

### Acceptance criteria

- Root test execution succeeds.
- A failing test is reported clearly and stops the build.

## Epic B0-6: Benchmark baseline with JMH

### Objective

Stand up the performance harness before parser logic is implemented.

### Tasks

- [ ] Create `sqool-bench`
- [ ] Add JMH integration
- [ ] Add a minimal benchmark class
- [ ] Confirm benchmark packaging and execution
- [ ] Document how to run benchmarks locally and in CI
- [ ] Decide whether benchmark results are stored as artifacts only or also committed as reports

### Deliverables

- working benchmark module
- minimal benchmark execution path
- benchmark usage notes

### Dependencies

- B0-1
- B0-2
- B0-3

### Acceptance criteria

- A sample benchmark runs successfully from the benchmark module.
- Benchmark invocation is documented and reproducible.

## Epic B0-7: Dependency and version governance

### Objective

Prevent toolchain drift and make upgrades deliberate.

### Tasks

- [ ] Centralize dependency versions
- [ ] Pin ANTLR tool and runtime versions
- [ ] Pin JUnit and JMH versions
- [ ] Pin plugin versions
- [ ] Document the upgrade policy for build-critical dependencies
- [ ] Add dependency locking or an equivalent reproducibility mechanism if appropriate

### Deliverables

- centralized version management
- dependency governance note

### Dependencies

- B0-1

### Acceptance criteria

- Critical build and runtime versions are pinned in one obvious place.
- Dependency updates can be reviewed cleanly.

## Epic B0-8: Formatting, static analysis, and CI

### Objective

Set the minimum repository quality bar for follow-on implementation work.

### Tasks

- [ ] Add a formatter configuration
- [ ] Add one static analysis or style check
- [ ] Add CI workflow for build and test verification
- [ ] Add CI workflow for benchmark smoke execution if lightweight enough
- [ ] Define the expected pre-merge verification commands
- [ ] Document contributor expectations in the repository

### Deliverables

- formatter configuration
- basic static/style check
- CI workflow definition
- repository contributor/build guidance

### Dependencies

- B0-1
- B0-2
- B0-3
- B0-5

### Acceptance criteria

- CI verifies the baseline build and tests.
- Formatting and quality checks are easy to run locally.

## 5. Suggested execution order

The preferred implementation sequence inside Milestone 0 is:

1. B0-1 Build tool and repository conventions
2. B0-2 Multi-module project skeleton
3. B0-3 Java 25 baseline
4. B0-7 Dependency and version governance
5. B0-4 ANTLR integration baseline
6. B0-5 Testing baseline
7. B0-6 Benchmark baseline with JMH
8. B0-8 Formatting, static analysis, and CI

This sequence gets the build spine in place before grammar generation, then adds tests, benchmarks, and CI on top.

## 6. Proposed issue decomposition

If Milestone 0 is split into implementation issues, use one issue per epic:

- Issue 1: choose build tool and root build conventions
- Issue 2: create multi-module skeleton
- Issue 3: configure Java 25 baseline
- Issue 4: add ANTLR generation baseline
- Issue 5: add testing baseline
- Issue 6: add JMH benchmark baseline
- Issue 7: centralize dependency version management
- Issue 8: add formatting, static analysis, and CI checks

If smaller issues are preferred, split each epic into setup, validation, and documentation subtasks.

## 7. Out of scope for Milestone 0

Milestone 0 should not attempt to complete:

- public API finalization
- AST definition beyond placeholders needed for compilation
- dialect-specific parsing logic
- grammar hardening
- benchmark comparisons against JSqlParser
- corpus conformance suites

Those belong to later milestones.

## 8. Risks specific to bootstrap work

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Build tool indecision delays all work | High | Make the build-tool decision first and record it. |
| Too many quality tools create friction early | Medium | Start with minimal useful checks and expand later. |
| ANTLR generation layout becomes awkward across modules | High | Prove one grammar module end to end before scaling out. |
| Benchmark setup is deferred too long | Medium | Add JMH in Milestone 0 even with trivial benchmarks. |
| Java 25 support differs across plugins | Medium | Use pinned tool versions and validate the full build from CI early. |

## 9. Immediate next implementation step

The immediate next task after approving this backlog should be to validate the bootstrap baseline with review feedback and then begin the MySQL MVP work:

1. confirm the Gradle bootstrap remains the desired foundation,
2. capture any non-blocking Milestone 0 follow-up items, and
3. start importing and hardening the real MySQL grammar.

That work moves the project from infrastructure bootstrap into the first functional dialect milestone.
