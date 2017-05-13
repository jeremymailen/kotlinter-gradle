# Kotlinter Gradle

[![Build Status](https://api.travis-ci.org/jeremymailen/kotlinter-gradle.svg?branch=master)](https://travis-ci.org/jeremymailen/kotlinter-gradle)

Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://github.com/shyiko/ktlint) engine.

### Installation

```groovy
plugins {
    id 'org.jmailen.kotlinter' version '0.6.0'
}
```
_Requires_: use of one of the JetBrains Kotlin Gradle plugins in your project.

### Features
- Linting and formatting tasks
- Incremental build support
- `.kt` and `.kts` source support
- Report and console outputs

### Tasks

`formatKotlin`: format Kotlin source code according to ktlint rules (when possible to auto-format).

`lintKotlin`: check Kotlin source code for lint formatting error and (by default) fail the build.

Additionally the `check` task becomes dependent on `lintKotlin`.

Granular tasks also exist for each source set in the project: `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`*.

### Configuration
Options are configured in the `kotlinter` extension. Defaults shown.
```groovy
kotlinter {
    ignoreFailures = false
}
```

The `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`* tasks inherit from [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
so you can customize includes, excludes, and source.

```groovy
lintKotlinMain {
    exclude '**/*Generated.kt'
}
```

### Planned Features
- dependency configuration for adding ktlint [rulesets](https://github.com/shyiko/ktlint#creating-a-ruleset)
- additional configurability along the lines of [checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)
