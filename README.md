# Kotlinter Gradle

[![Build Status](https://api.travis-ci.org/jeremymailen/kotlinter-gradle.svg?branch=master)](https://travis-ci.org/jeremymailen/kotlinter-gradle)

Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://github.com/shyiko/ktlint) engine.

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

```groovy
plugins {
    id 'org.jmailen.kotlinter' version '0.7.0'
}
```

### Features
- Linting and formatting tasks
- Incremental build support
- `.kt` and `.kts` source support
- Report and console outputs

### Tasks

If your project uses the JetBrains Kotlin JVM Gradle plugin, standard tasks will created:

`formatKotlin`: format Kotlin source code according to `ktlint` rules (when possible to auto-format).

`lintKotlin`: check Kotlin source code for lint formatting error and (by default) fail the build.

Additionally the `check` task becomes dependent on `lintKotlin`.

Granular tasks also exist for each source set in the project: `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`*.

If you haven't applied the Kotlin JVM plugin you can still create custom tasks:

```groovy
import org.jmailen.gradle.kotlinter.tasks.LintTask

task ktLint(type: LintTask, group: 'verification') {
    source files('src/kotlin')
    report = file('build/lint-report.txt')
}
```

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

If you need to use a different version of `ktlint` you can override the dependency.

```groovy
buildscript {
    configurations.classpath {
        resolutionStrategy { force 'com.github.shyiko:ktlint:0.6.1' }
    }
}
```

### Planned Features
- dependency configuration for adding ktlint [rulesets](https://github.com/shyiko/ktlint#creating-a-ruleset)
- additional configurability along the lines of [checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)
