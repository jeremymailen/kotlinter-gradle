import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}

dependencies {
    compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.8.0")
}
