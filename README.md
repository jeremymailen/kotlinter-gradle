# Kotlinter Gradle

[![Build Status](https://github.com/jeremymailen/kotlinter-gradle/workflows/Build%20Project/badge.svg)](https://github.com/jeremymailen/kotlinter-gradle/actions)

[![Current release](https://img.shields.io/github/v/release/jeremymailen/kotlinter-gradle)](https://github.com/jeremymailen/kotlinter-gradle/releases)

Painless Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://ktlint.github.io) engine.

It aims to be easy to set up with _zero_ required configuration and behaves as you'd expect out of the box.

It's also fast because it integrates the ktlint _engine_ directly with Gradle's incremental build and uses the Worker API to parallelize work.

### Versions

This documentation is for version 5+.
For documentation on version 4x and earlier, see [README-4x.md](README-4x.md).

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

Replace `<release>` in the examples below with the [current release](https://github.com/jeremymailen/kotlinter-gradle/releases/latest) version or your desired version.

#### Single module

<details open>
<summary>Kotlin</summary>

```kotlin
plugins {
    id("org.jmailen.kotlinter") version "<release>"
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
plugins {
    id "org.jmailen.kotlinter" version "<release>"
}
```

</details>

#### Multi-module and Android

<details open>
<summary>Kotlin</summary>
Root `build.gradle.kts`

```kotlin
plugins {
    id("org.jmailen.kotlinter") version "<release>" apply false
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
    id 'org.jmailen.kotlinter' version "<release>" apply false
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

- Kotlin -- generally compatible so long as the language features are supported by the version of ktlint selected
- Gradle 8.4+
- Ktlint 1.0+

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
Options are configured in the `kotlinter` extension. Defaults shown (you may omit the configuration block entirely if you want the defaults).

<details open>
<summary>Kotlin</summary>

```kotlin
kotlinter {
    ktlintVersion = "1.5.0"
    ignoreFormatFailures = true
    ignoreLintFailures = false
    reporters = arrayOf("checkstyle")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
kotlinter {
    ktlintVersion = "1.5.0"
    ignoreFormatFailures = true
    ignoreLintFailures = false
    reporters = ['checkstyle']
}
```

</details>

The `ktlintVersion` property allows you to override the default version of `ktlint` used by the plugin.
Compatibility will generally be good, but ktlint consumer API changes may break compatibility in some rare cases.

Setting `ignoreFormatFailures` to `false` will configure the `formatKotlin` task to fail the build when auto-format is not able to fix a lint error.

Options for `reporters`: `checkstyle`, `html`, `json`, `plain`, `sarif`

Reporters behave as described at: https://github.com/pinterest/ktlint

### Editorconfig

Kotlinter will configure itself using an `.editorconfig` file if one is present.

This configuration includes code style and linting rules.

See [KtLint configuration](https://pinterest.github.io/ktlint/latest/rules/configuration-ktlint/) for details.

### Customizing Tasks

The `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`* tasks inherit from [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
so you can customize includes, excludes, and source.

Note that `exclude` paths are relative to the package root.
If you need to exclude at the src directory level, you can use a syntax like the below:

<details open>
<summary>Kotlin</summary>

```kotlin
tasks.withType<LintTask> {
    exclude { it.file.path.contains("/src/generated") }
}

tasks.withType<FormatTask> {
    exclude { it.file.path.contains("/src/generated") }
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
tasks.named("lintKotlinMain") {
    exclude { it.file.path.contains("/src/generated") }
}
```

</details>

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
}


tasks.register('ktFormat', FormatTask) {
  group 'formatting'
  source files('src/test')
  report = file('build/format-report.txt')
}
```

</details>

### Custom Rules

You can add custom ktlint RuleSets using the `ktlint` dependency configuration:

<details open>
<summary>Kotlin</summary>

```kotlin
dependencies {
  ktlint(project(":extra-rules"))
  ktlint("org.other.ktlint:custom-rules:1.0")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
dependencies {
  ktlint project(':extra-rules')
  ktlint 'org.other.ktlint:custom-rules:1.0'
}
```

</details>
