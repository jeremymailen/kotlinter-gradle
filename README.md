# Kotlinter Gradle

[![Build Status](https://api.travis-ci.org/jeremymailen/kotlinter-gradle.svg?branch=master)](https://travis-ci.org/jeremymailen/kotlinter-gradle)

Painless gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://ktlint.github.io) engine.

It aims to be easy to set up with zero _required_ configuration and behaves as you'd expect out of the box.

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

#### Single module

<details open>
<summary>Kotlin</summary>

```kotlin
plugins {
    id("org.jmailen.kotlinter") version "2.3.2"
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
plugins {
    id "org.jmailen.kotlinter" version "2.3.2"
}
```

</details>

#### Multi-module and Android

<details open>
<summary>Kotlin</summary>
Root `build.gradle.kts`

```kotlin
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.jmailen.gradle:kotlinter-gradle:2.3.2")
    }
}
```

Each module `build.gradle.kts` with Kotlin source

```kotlin
apply(plugin = "org.jmailen.kotlinter")
```

</details>

<details>
<summary>Groovy</summary>
Root `build.gradle`

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "org.jmailen.gradle:kotlinter-gradle:2.3.2"
    }
}
```

Each module `build.gradle` with Kotlin source

```groovy
apply plugin: "org.jmailen.kotlinter"
```

</details>

### Compatibility

Kotlinter is compatible with Kotlin Gradle plugins 1.3.30+ and Java 13/12/11/10/9/8.

### Features

- Supports `.kt` and `.kts` files
- Supports Kotlin JVM and Android projects (autoconfiguration with these plugins)
- Standalone `LintTask` and `FormatTask` types for defining custom tasks
- Incremental build support and fast parallelization with Gradle workers
- Configures from `.editorconfig` when available
- Configurable reporters

### Tasks

If your project uses the JetBrains Kotlin JVM or Android Gradle plugins, standard tasks are created:

`formatKotlin`: format Kotlin source code according to `ktlint` rules or warn when auto-format not possible.

`lintKotlin`: report Kotlin lint errors and by default fail the build.

Also `check` becomes dependent on `lintKotlin`.

Granular tasks exist for each source set in the project: `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`*.

### Configuration
Options are configured in the `kotlinter` extension. Defaults shown (you may omit the configuration block entirely if you want these defaults).

<details open>
<summary>Kotlin</summary>

```kotlin
kotlinter {
    ignoreFailures = false
    indentSize = 4
    reporters = arrayOf("checkstyle", "plain")
    experimentalRules = false
    disabledRules = emptyArray<String>()
    fileBatchSize = 30
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
kotlinter {
    ignoreFailures = false
    indentSize = 4
    reporters = ['checkstyle', 'plain']
    experimentalRules = false
    disabledRules = []
    fileBatchSize = 30
}
```

</details>

Options for `reporters`: `checkstyle`, `html`, `json`, `plain`

Reporters behave as described at: https://github.com/pinterest/ktlint

The `experimentalRules` property enables rules which are part of ktlint's experimental rule set.

The `disabledRules` property can includes an array of rule ids you wish to disable. For example to allow wildcard imports:
```groovy
disabledRules = ["no-wildcard-imports"]
```
You must prefix rule ids not part of the standard rule set with `<rule-set-id>:<rule-id>`. For example `experimental:annotation`.

The `fileBatchSize` property configures the number of files that are processed in one Gradle Worker API call.

### Editorconfig

Kotlinter will configure itself using an `.editorconfig` file if one is present in your root project directory.

If a non-empty `disabledRules` value is specified in the `kotlinter` extension, it will take precedence over any `disabled_rules` in `.editorconfig`.

See [Ktlint editorconfig](https://github.com/pinterest/ktlint#editorconfig) for supported values.

### Customizing Tasks

The `formatKotlin`*`SourceSet`* and `lintKotlin`*`SourceSet`* tasks inherit from [SourceTask](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceTask.html)
so you can customize includes, excludes, and source.

<details open>
<summary>Kotlin</summary>

```kotlin
import org.jmailen.gradle.kotlinter.tasks.LintTask

tasks {
    "lintKotlinMain"(LintTask::class) {
        exclude("**/*Generated.kt")
    }
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
lintKotlinMain {
    exclude '**/*Generated.kt'
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

val ktLint by tasks.creating(LintTask::class) {
    group = "verification"
    source(files("src"))
    reports = mapOf(
        "plain" to file("build/lint-report.txt"),
        "json" to file("build/lint-report.json")
    )
}

val ktFormat by tasks.creating(FormatTask::class) {
    group = "formatting"
    source(files("src"))
    report = file("build/format-report.txt")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

task ktLint(type: LintTask, group: 'verification') {
    source files('src')
    reports = [
            'plain': file('build/lint-report.txt'),
            'json': file('build/lint-report.json')
    ]
    disabledRules = ["import-ordering"]
}

task ktFormat(type: FormatTask, group: 'formatting') {
    source files('src')
    report = file('build/format-report.txt')
    disabledRules = ["import-ordering"]
}
```

</details>

### Custom ktlint version

If you need to use a different version of `ktlint` you can override the dependency.

<details open>
<summary>Kotlin</summary>

```kotlin
buildscript {
    configurations.classpath
        .resolutionStrategy.force("com.github.pinterest:ktlint:0.31.0")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
buildscript {
    configurations.classpath {
        resolutionStrategy { force 'com.github.pinterest:ktlint:0.31.0' }
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
