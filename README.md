# Kotlinter Gradle

[![Build Status](https://api.travis-ci.org/jeremymailen/kotlinter-gradle.svg?branch=master)](https://travis-ci.org/jeremymailen/kotlinter-gradle)

Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://ktlint.github.io) engine.

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

```groovy
plugins {
    id 'org.jmailen.kotlinter' version '1.5.1'
}
```

### Compatibility

Kotlinter 1.4.0 and later compatible with Kotlin Gradle plugins 1.1.50+

Kotlinter 1.2.0 and later compatible with Kotlin Gradle plugins 1.1.3+

Kotlinter 1.1.0 and earlier compatible with Kotlin Gradle plugins 1.1 - 1.1.2-5

### Features
- Extends Kotlin JVM and Android projects with lint and format tasks for each `SourceSet`
- Standalone `LintTask` and `FormatTask` types for defining custom tasks
- Incremental build support
- `.kt` and `.kts` source support
- Console output and configurable reporters

### Tasks

If your project uses the JetBrains Kotlin JVM or Android Gradle plugins, standard tasks are created:

`formatKotlin`: format Kotlin source code according to `ktlint` rules or warn when auto-format not possible.

`lintKotlin`: report Kotlin lint errors and by default fail the build.

Also `check` becomes dependent on `lintKotlin`.

Granular tasks exist for each source set in the project: `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`*.

### Custom Tasks

If you haven't applied these plugins you can create custom tasks:

```groovy
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

task ktLint(type: LintTask, group: 'verification') {
    source files('src/kotlin')
    report = file('build/lint-report.txt')
}

task ktFormat(type: FormatTask, group: 'formatting') {
    source files('src/kotlin')
    report = file('build/format-report.txt')
}
```

### Configuration
Options are configured in the `kotlinter` extension. Defaults shown.
```groovy
kotlinter {
    ignoreFailures = false
    indentSize = 4
    reporters = ['checkstyle', 'plain']
}
```
Options for `reporters`: checkstyle, json, plain

*Note: `reporter` with a single value is deprecated but supported for backwards compatibility.

Reporters behave as described at: https://github.com/shyiko/ktlint

### Customizing Tasks

The `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`* tasks inherit from [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
so you can customize includes, excludes, and source.

```groovy
lintKotlinMain {
    exclude '**/*Generated.kt'
}
```

### Custom ktlint version

If you need to use a different version of `ktlint` you can override the dependency.

```groovy
buildscript {
    configurations.classpath {
        resolutionStrategy { force 'com.github.shyiko:ktlint:0.11.1' }
    }
}
```

### Custom Rules

You can add custom ktlint RuleSets using the `buildscript` classpath:

```groovy
buildscript {
    dependencies {
        classpath files('libs/my-custom-ktlint-rules.jar')
        classpath 'org.other.ktlint:custom-rules:1.0'
    }
}
```
