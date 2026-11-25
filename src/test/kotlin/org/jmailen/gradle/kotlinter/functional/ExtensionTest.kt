package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.repositories
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class ExtensionTest : WithGradleTest.Kotlin() {

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
                    $repositories
                    """.trimIndent()
                writeText(buildScript)
            }
        }
    }

    @Test
    fun `ktlint 2 runs lint and format with all reporters`() {
        projectRoot.resolve("build.gradle") {
            appendText(
                """

                kotlinter {
                    ktlintVersion = '2.0.0-ALPHA-5'
                    reporters = ['checkstyle', 'html', 'json', 'plain', 'sarif']
                    ignoreLintFailures = true
                }
                """.trimIndent(),
            )
        }
        val source = projectRoot.resolve("src/main/kotlin/KotlinClass.kt") {
            writeText("class KotlinClass{ }\n")
        }
        build("lintKotlinMain").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertTrue(output.contains("Lint error"))
        }
        listOf("xml", "html", "json", "txt", "sarif.json").forEach { extension ->
            val report = projectRoot.resolve("build/reports/ktlint/main-lint.$extension")
            assertTrue(report.isFile && report.readText().contains("KotlinClass.kt"), report.path)
        }
        build("formatKotlinMain").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinMain")?.outcome)
        }
        assertTrue(!source.readText().contains("Class{"))
        build("lintKotlinMain").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertTrue(!output.contains("Lint error"))
        }
    }

    @Test
    fun `ktlint version below alpha 5 fails resolution`() {
        projectRoot.resolve("build.gradle") {
            appendText("\nkotlinter { ktlintVersion = '2.0.0-ALPHA-4' }\n")
        }
        projectRoot.resolve("src/main/kotlin/KotlinClass.kt") {
            writeText(kotlinClass("KotlinClass"))
        }
        buildAndFail("lintKotlinMain").apply {
            assertTrue(output.contains("Kotlinter requires KtLint 2.x starting with 2.0.0-ALPHA-5"))
        }
    }

    @Test
    fun `extension configures ignoreLintFailures`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val script =
                """
                kotlinter {
                    ignoreLintFailures = true
                }
                """.trimIndent()
            appendText(script)
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `extension configures reporters`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val script =
                """
                kotlinter {
                    reporters = ['html'] 
                }
                """.trimIndent()
            appendText(script)
        }
        projectRoot.resolve("src/main/kotlin/KotlinClass.kt") {
            writeText(kotlinClass("KotlinClass"))
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
        val report = projectRoot.resolve("build/reports/ktlint/main-lint.html")
        assertTrue(report.readText().isNotEmpty())
    }

    @Test
    fun `extension properties are evaluated only during task execution`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                plugins {
                    id 'kotlin'
                    id 'org.jmailen.kotlinter'
                }
                $repositories
                
                tasks.whenTaskAdded {
                    // configure all tasks eagerly
                }
                
                kotlinter {
                    ignoreLintFailures = true
                }
                
                """.trimIndent()
            writeText(buildScript)
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }
}
