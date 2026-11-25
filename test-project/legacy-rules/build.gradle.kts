import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    compileOnly("io.github.ktlint.core:ktlint-com-pinterest-backward-compatibility:2.0.0-ALPHA-5")
    compileOnly("io.github.ktlint.core:ktlint-rule-engine-core:2.0.0-ALPHA-5")
}
