# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

- Build project: `./gradlew build`
- Run all tests: `./gradlew test`
- Run a single test: `./gradlew test --tests "org.jmailen.gradle.kotlinter.functional.KotlinProjectTest.testName"`
- Run integration tests projects (from test-project* directories): `../gradlew lintKotlin formatKotlin`
- Lint Kotlin code: `./gradlew lintKotlin`
- Format Kotlin code: `./gradlew formatKotlin`
- Lint specific source set: `./gradlew lintKotlinMain`
- Format specific source set: `./gradlew formatKotlinMain`

## Code Style Guidelines

- Kotlin code follows ktlint rules with editorconfig customizations
- Max line length: 140 characters
- Indentation: 4 spaces
- New line at EOF required
- Trailing commas allowed in declarations and function calls
- Imports follow ktlint standard ordering
- Use kotlinter extension for configuration in Gradle projects
- Tests use JUnit 5 (Jupiter) assertions
- Functional tests extend `WithGradleTest` for Gradle TestKit integration
- Error handling uses custom `KotlinterError` and `LintFailure` classes
- When adding features, ensure backward compatibility as this is a Gradle plugin