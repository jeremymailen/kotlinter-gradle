package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.editorConfig
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

class CustomTaskTest : WithGradleTest.Kotlin() {

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
        }
        projectRoot.resolve("src/main/kotlin/CustomClass.kt") {
            writeText(kotlinClass("CustomClass"))
        }
    }

    @Test
    fun `ktLint custom task succeeds when no lint errors detected`() {
        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }
        projectRoot.resolve("src/main/kotlin/CustomClass.kt") {
            // language=kotlin
            val validClass =
                """
                class CustomClass {
                    private fun go() {
                        println("go")
                    }
                }
                """.trimIndent()
            writeText(validClass)
        }
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task ktLint(type: LintTask) {
                    source files('src')
                    reports = ['plain': file('build/lint-report.txt')]
                    experimentalRules = true
                    disabledRules = ["final-newline"]
                }
                
                """
            appendText(buildScript)
        }

        build("ktLint").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":ktLint")?.outcome)
        }
    }

    @Test
    fun `ktLint custom task succeeds with default configuration`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task minimalCustomTask(type: LintTask) {
                    source files('src')
                }
                
                """.trimIndent()
            appendText(buildScript)
        }

        build("minimalCustomTask").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":minimalCustomTask")?.outcome)
        }
    }

    @Test
    fun `ktLint custom task succeeds with fully provided configuration`() {
        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task customizedLintTask(type: LintTask) {
                    source files('src')
                    reports = ['plain': file('build/lint-report.txt')]
                    experimentalRules = true
                    disabledRules = ["final-newline"]
                    ignoreFailures = true
                }
                
                """.trimIndent()
            appendText(buildScript)
        }

        build("customizedLintTask").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":customizedLintTask")?.outcome)
        }
    }

    @Test
    fun `ktLintFormat custom task succeeds with fully provided configuration`() {
        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.FormatTask
    
                task customizedFormatTask(type: FormatTask) {
                    source files('src')
                    experimentalRules = true
                    disabledRules = ["final-newline"]
                }
                
                """.trimIndent()
            appendText(buildScript)
        }

        build("customizedFormatTask").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":customizedFormatTask")?.outcome)
        }
    }

    @Test
    fun `ktLint custom task skips reports generation if reports not configured`() {
        projectRoot.resolve("src/main/kotlin/MissingNewLine.kt") {
            // language=kotlin
            val validClass =
                """
                class MissingNewLine
                """.trimIndent()
            writeText(validClass)
        }
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task reportsNotConfigured(type: LintTask) {
                    source files('src')
                }
                
                task reportsEmpty(type: LintTask) {
                    source files('src')
                    reports = [:]
                }
                
                """
            appendText(buildScript)
        }

        buildAndFail("reportsEmpty").apply {
            assertEquals(TaskOutcome.FAILED, task(":reportsEmpty")?.outcome)
            assertTrue(output.contains("[final-newline] File must end with a newline (\\n)"))
            assertArrayEquals(emptyArray<String>(), projectRoot.resolve("build/reports/ktlint").list().orEmpty())
        }
        buildAndFail("reportsNotConfigured").apply {
            assertEquals(TaskOutcome.FAILED, task(":reportsNotConfigured")?.outcome)
            assertTrue(output.contains("[final-newline] File must end with a newline (\\n)"))
            assertArrayEquals(emptyArray<String>(), projectRoot.resolve("build/reports/ktlint").list().orEmpty())
        }
    }

    @Test
    fun `ktLint custom task became up-to-date on second run if reports not configured`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task reportsNotConfigured(type: LintTask) {
                    source files('src')
                }
                
                task reportsEmpty(type: LintTask) {
                    source files('src')
                    reports = [:]
                }
                
                """
            appendText(buildScript)
        }

        build("reportsEmpty").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":reportsEmpty")?.outcome)
        }
        build("reportsEmpty").apply {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":reportsEmpty")?.outcome)
        }
        build("reportsNotConfigured").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":reportsNotConfigured")?.outcome)
        }
        build("reportsNotConfigured").apply {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":reportsNotConfigured")?.outcome)
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "https://github.com/gradle/gradle/issues/21964")
    fun `ktLint custom task treats reports as input parameter`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task ktLintWithReports(type: LintTask) {
                    source files('src')
                    reports = reports = ['plain': file('build/lint-report.txt')]
                }
                
                """
            appendText(buildScript)
        }

        build("ktLintWithReports").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":ktLintWithReports")?.outcome)
            assertTrue(projectRoot.resolve("build/lint-report.txt").exists())
        }
        build("ktLintWithReports").apply {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":ktLintWithReports")?.outcome)
        }

        projectRoot.resolve("build/lint-report.txt").appendText("CHANGED REPORT FILE")

        build("ktLintWithReports").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":ktLintWithReports")?.outcome)
            assertTrue(projectRoot.resolve("build/lint-report.txt").exists())
        }
    }
}
