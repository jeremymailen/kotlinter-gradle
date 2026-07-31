package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class WorkerJvmArgsTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File

    @BeforeEach
    fun setUp() {
        projectRoot = testProjectDir.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                // language=groovy
                val buildScript =
                    """
                    plugins {
                        id 'kotlin'
                        id 'org.jmailen.kotlinter'
                    }

                    repositories {
                        mavenCentral()
                    }
                    """.trimIndent()
                writeText(buildScript)
            }
            resolve("src/main/kotlin/CustomClass.kt") { writeText(kotlinClass("CustomClass")) }
        }
    }

    @Test
    fun `lint task doesn't print the sun misc Unsafe deprecation warning`() {
        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertFalse(output.contains("sun.misc.Unsafe"), "unexpected JDK deprecation warning in build output")
        }
    }

    @Test
    fun `workerJvmArgs are passed to the worker jvm`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """

                import org.jmailen.gradle.kotlinter.tasks.LintTask

                tasks.withType(LintTask).configureEach {
                    workerJvmArgs.add('--not-a-jvm-option')
                }
                """.trimIndent()
            appendText(buildScript)
        }

        buildAndFail("lintKotlin").apply {
            assertTrue(output.contains("Unrecognized option: --not-a-jvm-option"))
        }
    }
}
