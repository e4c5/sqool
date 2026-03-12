# Milestone 0 Review Checklist

## Purpose

This checklist is for reviewing the Milestone 0 bootstrap work. It is meant to validate that the project foundation is sound before the first real dialect implementation work begins.

This review should focus on:

- build and repository structure
- Java 25 toolchain strategy
- ANTLR integration pattern
- testing and benchmark wiring
- CI and reproducibility

This review should not spend much time on:

- final parser API design
- AST completeness
- SQL grammar correctness beyond the smoke baseline
- dialect feature coverage
- real parser performance claims

## Recommended review outcome

At the end of the review, classify Milestone 0 as one of:

- **Approved**: foundation is good enough to start MySQL MVP
- **Approved with follow-ups**: foundation is acceptable, but a few non-blocking issues should be fixed soon
- **Changes required**: one or more structural decisions should be corrected before starting Milestone 1

## 1. Build system and repository structure

- [ ] Gradle is a reasonable build choice for this project
- [ ] The wrapper is committed and usable from a clean checkout
- [ ] The root build is easy to understand
- [ ] The version catalog and shared build configuration are centralized appropriately
- [ ] The multi-module layout matches the intended long-term architecture
- [ ] Module names are clear and consistent
- [ ] The current skeleton does not create obvious future coupling problems

### Evidence to review

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradlew`, `gradlew.bat`, and wrapper files
- top-level module directories

### Review questions

1. Are any modules missing or oddly placed?
2. Is there any build logic that should be moved out of the root build later?
3. Does the current structure make dialect-by-dialect development straightforward?

## 2. Java 25 baseline and toolchain strategy

- [ ] Java 25 is configured as the intended project baseline
- [ ] The toolchain configuration is explicit and reproducible
- [ ] The build can provision the required JDK without relying on a manually prepared machine
- [ ] The current setup avoids preview-feature dependence
- [ ] The Java version strategy is appropriate for CI and contributor workflows

### Evidence to review

- Java toolchain configuration in `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- CI workflow setup

### Review questions

1. Is Java 25 the right baseline now, or should the project temporarily compile on an earlier LTS?
2. Is auto-download of toolchains acceptable for this repository?
3. Are there any environment assumptions that should be documented more explicitly?

## 3. ANTLR integration baseline

- [ ] The repository demonstrates a working ANTLR generation path
- [ ] Generated sources are produced by the build, not edited by hand
- [ ] The ANTLR wiring is understandable and maintainable
- [ ] The grammar module structure will scale to real vendored grammars
- [ ] Generated source handling is compatible with formatting and static analysis rules

### Evidence to review

- `sqool-grammar-mysql`
- ANTLR configuration in `build.gradle.kts`
- bootstrap grammar and parser smoke test

### Review questions

1. Is one grammar module enough proof for Milestone 0?
2. Should generated sources remain excluded from style checks in the current way?
3. Will the current ANTLR layout still work once the real MySQL grammar is imported?

## 4. Testing baseline

- [ ] JUnit is wired correctly at the root build level
- [ ] Smoke tests prove the build and module wiring, not just trivial assertions
- [ ] The testing setup is simple enough for early development
- [ ] The test conventions are acceptable for later expansion into conformance suites

### Evidence to review

- `sqool-core` tests
- `sqool-ast` tests
- `sqool-grammar-mysql` parser smoke test
- test configuration in `build.gradle.kts`

### Review questions

1. Are the current smoke tests sufficient for a bootstrap milestone?
2. Is any additional shared test infrastructure needed now, or should it wait?

## 5. Benchmark baseline

- [ ] JMH is integrated successfully
- [ ] The benchmark module layout is appropriate
- [ ] The benchmark configuration is light enough for bootstrap verification
- [ ] The repository is now ready for later parser-vs-parser benchmark work

### Evidence to review

- `sqool-bench`
- JMH plugin configuration in `build.gradle.kts`
- bootstrap benchmark and benchmark command usage

### Review questions

1. Is the benchmark module too minimal, or is it the right level for Milestone 0?
2. Are the current default JMH settings sensible for a bootstrap verification path?

## 6. Formatting, static analysis, and CI

- [ ] Formatting is easy to run locally
- [ ] Static analysis is present without being excessively heavy for bootstrap work
- [ ] CI covers the most important verification steps
- [ ] CI assumptions are compatible with Java 25 and Gradle toolchains
- [ ] The quality bar is appropriate for early-stage parser development

### Evidence to review

- Spotless and Checkstyle configuration
- `.github/workflows/ci.yml`
- `README.md`

### Review questions

1. Is the current Checkstyle baseline too weak, too strict, or about right?
2. Should CI run the full JMH benchmark, compile only, or keep the current split?
3. Are any local developer commands missing from the README?

## 7. Documentation and onboarding

- [ ] The repository explains how to build and verify the baseline
- [ ] The planning documents and implemented baseline are still aligned
- [ ] The next implementation step is clear from the documentation

### Evidence to review

- `README.md`
- `docs/sql-parser-technical-design.md`
- `docs/sql-parser-implementation-checklist.md`
- `docs/milestone-0-bootstrap-backlog.md`

### Review questions

1. Is the README sufficient for a new contributor to run the project?
2. Are any planning documents now stale after the Milestone 0 implementation?

## 8. Specific things to watch for

During review, pay extra attention to these possible foundation issues:

- build logic becoming too centralized in the root project
- module boundaries that will break down when real dialect code is added
- ANTLR generation assumptions that only work for the bootstrap grammar
- Java 25 toolchain friction in CI or contributor environments
- benchmark wiring that is too slow or too loose for repeatable use

## 9. Suggested review commands

Use these commands as the baseline review verification:

```bash
./gradlew build
./gradlew verifyBootstrap
./gradlew :sqool-bench:jmh
```

Optional deeper checks:

```bash
./gradlew check
./gradlew tasks
```

## 10. Exit criteria for review sign-off

Milestone 0 review is complete when:

- the foundation is approved or approved with follow-ups,
- any blocking structural issues are identified clearly,
- the team agrees the repository is ready for the MySQL MVP stage.
