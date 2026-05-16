// Test project without Kotlin plugin
plugins {
    // No Kotlin plugin applied
    id("org.jmailen.kotlinter")
}

// Configure Kotlinter
configure<org.jmailen.gradle.kotlinter.KotlinterExtension> {
    ktlintVersion = "1.7.1"
    ignoreFormatFailures = true
    ignoreLintFailures = false
    reporters = arrayOf("plain", "checkstyle")
}

// Create a custom format task using Task type
tasks.register("customFormatTask", org.jmailen.gradle.kotlinter.tasks.FormatTask::class) {
    group = "formatting"
    description = "Format Kotlin code with a custom task"
    source(fileTree("src"))
    report.set(layout.buildDirectory.file("reports/ktlint/custom-format.txt"))
}

// Create a custom lint task
tasks.register("customLintTask", org.jmailen.gradle.kotlinter.tasks.LintTask::class) {
    group = "formatting"
    description = "Lint Kotlin code with a custom task"
    source(fileTree("src"))
    ignoreLintFailures.set(true) // Ignore failures to ensure report is generated
    reports.set(
        mapOf(
            "plain" to layout.buildDirectory.file("reports/ktlint/custom-lint.txt").get().asFile
        )
    )
}
