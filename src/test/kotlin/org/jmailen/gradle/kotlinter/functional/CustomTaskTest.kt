package org.jmailen.gradle.kotlinter.functional

import groovy.util.GroovyTestCase.assertEquals
import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.jmailen.gradle.kotlinter.functional.utils.editorConfig
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.Before
import org.junit.Test

class CustomTaskTest : WithGradleTest.Kotlin() {

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
            @Language("kotlin")
            val validClass = """
                class CustomClass {
                     private fun go() {
                          println("go")
                     }
                }
            """.trimIndent()
            writeText(validClass)
        }
        projectRoot.resolve("build.gradle") {
            @Language("groovy")
            val buildScript = """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task ktLint(type: LintTask) {
                    source files('src')
                    reports = ['plain': file('build/lint-report.txt')]
                    indentSize = 5
                    continuationIndentSize = 7
                    experimentalRules = true
                    disabledRules = ["final-newline"]
                    editorConfigPath = project.rootProject.file(".editorconfig")
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
            @Language("groovy")
            val buildScript = """
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
            @Language("groovy")
            val buildScript = """
                import org.jmailen.gradle.kotlinter.tasks.LintTask
    
                task customizedLintTask(type: LintTask) {
                    source files('src')
                    reports = ['plain': file('build/lint-report.txt')]
                    indentSize = 123
                    continuationIndentSize = 17
                    experimentalRules = true
                    disabledRules = ["final-newline"]
                    editorConfigPath = project.rootProject.file(".editorconfig")
                    ignoreFailures = true
                    fileBatchSize = 12
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
            @Language("groovy")
            val buildScript = """
                import org.jmailen.gradle.kotlinter.tasks.FormatTask
    
                task customizedFormatTask(type: FormatTask) {
                    source files('src')
                    indentSize = 123
                    continuationIndentSize = 17
                    experimentalRules = true
                    disabledRules = ["final-newline"]
                    editorConfigPath = project.rootProject.file(".editorconfig")
                    fileBatchSize = 12
                }
                
            """.trimIndent()
            appendText(buildScript)
        }

        build("customizedFormatTask").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":customizedFormatTask")?.outcome)
        }
    }
}
