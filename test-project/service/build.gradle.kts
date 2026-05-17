import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
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
    exclude { it.file.path.contains("/src/main/kotlin/generated") }
}
