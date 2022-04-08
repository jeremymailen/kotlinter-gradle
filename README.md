# Kotlinter Gradle

[![Build Status](https://github.com/jeremymailen/kotlinter-gradle/workflows/Build%20Project/badge.svg)](https://github.com/jeremymailen/kotlinter-gradle/actions)
[![Latest Version](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/jmailen/gradle/kotlinter-gradle/maven-metadata.xml?label=gradle)](https://plugins.gradle.org/plugin/org.jmailen.kotlinter)

Painless Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://ktlint.github.io) engine.

It aims to be easy to set up with _zero_ required configuration and behaves as you'd expect out of the box.

It's also fast because it integrates the ktlint _engine_ directly with Gradle's incremental build and uses the Worker API to parallelize work.

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

#### Single module

<details open>
<summary>Kotlin</summary>

```kotlin
plugins {
    id("org.jmailen.kotlinter") version "3.10.0"
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
plugins {
    id "org.jmailen.kotlinter" version "3.10.0"
}
```

</details>

#### Multi-module and Android

<details open>
<summary>Kotlin</summary>
Root `build.gradle.kts`

```kotlin
plugins {
    id("org.jmailen.kotlinter") version "3.10.0" apply false
}
```

Each module `build.gradle.kts` with Kotlin source

```kotlin
plugins {
    id("org.jmailen.kotlinter")
}
```

</details>

<details>
<summary>Groovy</summary>
Root `build.gradle`

```groovy
plugins {
    id 'org.jmailen.kotlinter' version "3.10.0" apply false
}
```

Each module `build.gradle` with Kotlin source

```groovy
plugins {
    id 'org.jmailen.kotlinter'
}
```

</details>

### Compatibility

| kotlinter version  | min kotlin version | max kotlin version | min gradle version |
|--------------------|--------------------|--------------------|--------------------|
| 3.7.0+             | 1.5.31             | -                  | 7.0                |
| 3.5.0+             | 1.5.0              | -                  | 6.8                |
| 3.0.0+             | 1.4.0              | 1.4.30             | 6.8                |
| 2.0.0+             | 1.3.0              | 1.3.30             | -                  |

### Features

- Supports Kotlin Gradle plugins:
  - [JVM](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm)
  - [Multiplatform](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.multiplatform)
  - [Android](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.android)
- Supports `.kt` and `.kts` files
- Standalone `LintTask` and `FormatTask` types for defining custom tasks
- Incremental build support and fast parallelization with Gradle Worker API
- Configures from `.editorconfig` when available
- Configurable reporters

### Tasks

When your project uses one of the supported Kotlin Gradle plugins, Kotlinter adds these tasks:

`formatKotlin`: format Kotlin source code according to `ktlint` rules or warn when auto-format not possible.

`lintKotlin`: report Kotlin lint errors and by default fail the build.

Also `check` becomes dependent on `lintKotlin`.

Granular tasks are added for each source set in the project: `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`*.

### Git Hooks

Kotlinter can install a hook to run pre-push (`installKotlinterPrePushHook`). The hook runs `lintKotlin` and, if there are errors, `formatKotlin` and exits non-zero leaving changed files to be committed.

You *must* apply the kotlinter plugin to your root project to make this task available. If using `git worktree` you must install the hook from the parent git directory.

To install the hook automatically when someone runs the build, add this to your root project `build.gradle.kts`:

<details open>
<summary>Kotlin</summary>

```kotlin
tasks.check {
    dependsOn("installKotlinterPrePushHook")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
tasks.named('check') {
    dependsOn 'installKotlinterPrePushHook'
}
```

</details>


### Configuration
Options are configured in the `kotlinter` extension. Defaults shown (you may omit the configuration block entirely if you want these defaults).

<details open>
<summary>Kotlin</summary>

```kotlin
kotlinter {
    ignoreFailures = false
    reporters = arrayOf("checkstyle", "plain")
    experimentalRules = false
    disabledRules = emptyArray()
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
kotlinter {
    ignoreFailures = false
    reporters = ['checkstyle', 'plain']
    experimentalRules = false
    disabledRules = []
}
```

</details>

Options for `reporters`: `checkstyle`, `html`, `json`, `plain`, `sarif`

Reporters behave as described at: https://github.com/pinterest/ktlint

The `experimentalRules` property enables rules which are part of ktlint's experimental rule set.

The `disabledRules` property can includes an array of rule ids you wish to disable. For example to allow wildcard imports:
```groovy
disabledRules = ["no-wildcard-imports"]
```
You must prefix rule ids not part of the standard rule set with `<rule-set-id>:<rule-id>`. For example `experimental:annotation`.

### Editorconfig

Kotlinter will configure itself using an `.editorconfig` file if one is present.

If a non-empty `disabledRules` value is specified in the `kotlinter` extension, it will take precedence over any `disabled_rules` in `.editorconfig`.

See [Ktlint editorconfig](https://github.com/pinterest/ktlint#editorconfig) for supported values.

### Customizing Tasks

The `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`* tasks inherit from [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
so you can customize includes, excludes, and source.

<details open>
<summary>Kotlin</summary>

```kotlin
tasks.lintKotlinMain {
  exclude("com/example/**/generated/*.kt")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
tasks.named('lintKotlinMain') {
    exclude 'com/example/**/generated/*.kt'
}
```

</details>

Note that exclude paths are relative to the package root.

### Custom Tasks

If you aren't using autoconfiguration from a supported plugin or otherwise need to handle additional source code, you can create custom tasks:

<details open>
<summary>Kotlin</summary>

```kotlin
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

tasks.register<LintTask>("ktLint") {
    group = "verification"
    source(files("src"))
    reports.set(
        mapOf(
            "plain" to file("build/lint-report.txt"),
            "json" to file("build/lint-report.json")
        )
    )
}

tasks.register<FormatTask>("ktFormat") {
    group = "formatting"
    source(files("src"))
    report.set(file("build/format-report.txt"))
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

tasks.register('ktLint', LintTask) {
    group 'verification'
    source files('src')
    reports = [
            'plain': file('build/lint-report.txt'),
            'json' : file('build/lint-report.json')
    ]
    disabledRules = ['import-ordering']
}


tasks.register('ktFormat', FormatTask) {
  group 'formatting'
  source files('src/test')
  report = file('build/format-report.txt')
  disabledRules = ['import-ordering']
}
```

</details>

### Custom ktlint version

If you need to use a different version of `ktlint` you can override the dependency.

<details open>
<summary>Kotlin</summary>

```kotlin
buildscript {
    configurations.classpath {
        resolutionStrategy {
            force(
                "com.pinterest.ktlint:ktlint-core:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-checkstyle:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-json:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-html:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-plain:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-sarif:0.39.0",
                "com.pinterest.ktlint:ktlint-ruleset-experimental:0.39.0",
                "com.pinterest.ktlint:ktlint-ruleset-standard:0.39.0"
            )
        }
    }
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
buildscript {
    configurations.classpath {
        resolutionStrategy {
            force(
                "com.pinterest.ktlint:ktlint-core:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-checkstyle:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-json:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-html:0.39.0",
                "com.pinterest.ktlint:ktlint-reporter-plain:0.39.0", 
                "com.pinterest.ktlint:ktlint-reporter-sarif:0.39.0",
                "com.pinterest.ktlint:ktlint-ruleset-experimental:0.39.0",
                "com.pinterest.ktlint:ktlint-ruleset-standard:0.39.0"
            )
        }
    }
}
```

</details>

### Custom Rules

You can add custom ktlint RuleSets using the `buildscript` classpath:

<details open>
<summary>Kotlin</summary>

```kotlin
buildscript {
    dependencies {
        classpath(files("libs/my-custom-ktlint-rules.jar"))
        classpath("org.other.ktlint:custom-rules:1.0")
    }
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
buildscript {
    dependencies {
        classpath files('libs/my-custom-ktlint-rules.jar')
        classpath 'org.other.ktlint:custom-rules:1.0'
    }
}
```

</details>
