# Kotlinter Gradle

[![Build Status](https://api.travis-ci.org/jeremymailen/kotlinter-gradle.svg?branch=master)](https://travis-ci.org/jeremymailen/kotlinter-gradle)

Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://github.com/shyiko/ktlint) engine.

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

```groovy
plugins {
    id 'org.jmailen.kotlinter' version '0.9.0'
}
```

### Features
- Extends Kotlin JVM and Android projects with lint and format tasks for each `SourceSet`
- Standalone `LintTask` and `FormatTask` types for defining custom tasks
- Incremental build support
- `.kt` and `.kts` source support
- Report and console outputs

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
        resolutionStrategy { force 'com.github.shyiko:ktlint:0.7.1' }
    }
}
```
