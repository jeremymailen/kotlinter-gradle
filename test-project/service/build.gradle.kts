import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

dependencies {
    ktlint(project(":rules"))
}

kotlinter {
    reporters = arrayOf("plain", "checkstyle")
    ignoreLintFailures = true
}

tasks.withType<LintTask> {
    exclude { it.file.path.contains("/src/main/kotlin/generated") }
}

tasks.withType<FormatTask> {
    exclude { it.file.path.contains("src/main/kotlin/generated") }
}
