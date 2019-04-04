# Kotlinter Gradle

[![Build Status](https://api.travis-ci.org/jeremymailen/kotlinter-gradle.svg?branch=master)](https://travis-ci.org/jeremymailen/kotlinter-gradle)

Gradle plugin for linting and formatting Kotlin source files using the awesome [ktlint](https://ktlint.github.io) engine.

### Installation

Available on the Gradle Plugins Portal: https://plugins.gradle.org/plugin/org.jmailen.kotlinter

#### Single module

<details open>
<summary>Kotlin</summary>

```kotlin
plugins {
    id("org.jmailen.kotlinter") version "1.23.0"
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
plugins {
    id "org.jmailen.kotlinter" version "1.23.0"
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
        classpath("org.jmailen.gradle:kotlinter-gradle:1.23.0")
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
        classpath "org.jmailen.gradle:kotlinter-gradle:1.23.0"
    }
}
```

Each module `build.gradle` with Kotlin source

```groovy
apply plugin: "org.jmailen.kotlinter"
```

</details>

### Compatibility
Kotlinter 1.21.0 and later compatible with Kotlin Gradle plugins 1.3.20+ and Java 11/10/9/8.

Kotlinter 1.12.0 and later compatible with Kotlin Gradle plugins 1.2.41+ and Java 9/8.

Kotlinter 1.8.0 and later compatible with Kotlin Gradle plugins 1.2.21+ and Java 9/8.

Kotlinter 1.7.0 and later compatible with Kotlin Gradle plugins 1.2.20+

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
}

task ktFormat(type: FormatTask, group: 'formatting') {
    source files('src')
    report = file('build/format-report.txt')
}
```

</details>

### Configuration
Options are configured in the `kotlinter` extension. Defaults shown (you may omit the configuration block entirely if you want these values).

<details open>
<summary>Kotlin</summary>

```kotlin
kotlinter {
    ignoreFailures = false
    indentSize = 4
    continuationIndentSize = 4
    reporter = arrayOf("checkstyle", "plain")
    experimentalRules = false
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
    continuationIndentSize = 4
    reporters = ['checkstyle', 'plain']
    experimentalRules = false
    fileBatchSize = 30
}
```

</details>

Options for `reporters`: checkstyle, html, json, plain

The html reporter is provided by [ktlint-html-reporter](https://github.com/mcassiano/ktlint-html-reporter).

Reporters behave as described at: https://github.com/shyiko/ktlint

*Note: `reporter` with a single value is deprecated but supported for backwards compatibility.

The `experimentalRules` property enables rules which are part of ktlint's experimental rule set.

The `fileBatchSize` property configures the number of files that are processed in one Gradle Worker API call.

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

### Custom ktlint version

If you need to use a different version of `ktlint` you can override the dependency.

<details open>
<summary>Kotlin</summary>

```kotlin
buildscript {
    configurations.classpath
        .resolutionStrategy.force("com.github.shyiko:ktlint:0.28.0")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
buildscript {
    configurations.classpath {
        resolutionStrategy { force 'com.github.shyiko:ktlint:0.28.0' }
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
