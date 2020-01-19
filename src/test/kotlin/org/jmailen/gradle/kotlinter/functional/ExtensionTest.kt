package org.jmailen.gradle.kotlinter.functional

import groovy.util.GroovyTestCase.assertEquals
import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class ExtensionTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File

    @Before
    fun setUp() {
        projectRoot = testProjectDir.root.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                @Language("groovy")
                val buildScript = """
                plugins {
                    id 'kotlin'
                    id 'org.jmailen.kotlinter'
                }
                
            """.trimIndent()
                writeText(buildScript)
            }
        }
    }

    @Test
    fun `extension configures ignoreFailures`() {
        projectRoot.resolve("build.gradle") {
            @Language("groovy")
            val script = """
                kotlinter {
                    ignoreFailures = true
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
    fun `extension configures indentSize`() {
        projectRoot.resolve("build.gradle") {
            @Language("groovy")
            val script = """
                kotlinter {
                    indentSize = 2
                }
            """.trimIndent()
            appendText(script)
        }
        projectRoot.resolve("src/main/kotlin/TwoSpaces.kt") {
            writeText(
                """ |
                    |  object TwoSpaces
                    |        
                """.trimMargin()
            )
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `extension configures reporters`() {
        projectRoot.resolve("build.gradle") {
            @Language("groovy")
            val script = """
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
    fun `extension configures disabledRules`() {
        projectRoot.resolve("build.gradle") {
            @Language("groovy")
            val script = """
                kotlinter {
                    disabledRules = ["filename"]
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
}
