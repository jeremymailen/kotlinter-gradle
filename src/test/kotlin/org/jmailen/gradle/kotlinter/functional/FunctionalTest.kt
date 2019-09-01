package org.jmailen.gradle.kotlinter.functional

import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FunctionalTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var sourceDir: File

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle")
        buildFile = testProjectDir.newFile("build.gradle")
        sourceDir = testProjectDir.newFolder("src", "main", "kotlin")
    }

    @Test
    fun `lintKotlinMain fails when lint errors detected`() {
        settingsFile()
        buildFile()

        val className = "KotlinClass"
        kotlinSourceFile("$className.kt", """
            class $className {
                private fun hi(){
                    println ("hi")
                }
            }

        """.trimIndent()
        )

        buildAndFail("lintKotlinMain").apply {
            assertTrue(output.contains("Lint error >.*$className.kt.*Missing spacing before \"\\{\"".toRegex()))
            assertTrue(output.contains("Lint error >.*$className.kt.*Unexpected spacing before \"\\(\"".toRegex()))
            assertEquals(FAILED, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `lintKotlinMain succeeds when no lint errors detected`() {
        settingsFile()
        buildFile()
        kotlinSourceFile("KotlinClass.kt", """
            class KotlinClass {
                private fun hi() {
                    println("hi")
                }
            }

        """.trimIndent()
        )

        build("lintKotlinMain").apply {
            assertEquals(SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `ktLint custom task succeeds when no lint errors detected`() {
        settingsFile()
        buildFile()
        kotlinSourceFile("CustomClass.kt", """
            class CustomClass {
                 private fun go() {
                      println("go")
                 }
            }
        """.trimIndent())

        build("ktLint").apply {
            assertEquals(SUCCESS, task(":ktLint")?.outcome)
        }
    }

    private fun build(vararg args: String): BuildResult = gradleRunnerFor(*args).build()

    private fun buildAndFail(vararg args: String): BuildResult = gradleRunnerFor(*args).buildAndFail()

    private fun gradleRunnerFor(vararg args: String): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(args.toList() + "--stacktrace")
            .withPluginClasspath()
    }

    private fun settingsFile() = settingsFile.apply {
        writeText("rootProject.name = 'kotlinter'")
    }

    private fun buildFile() = buildFile.apply {
        writeText("""
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.3.50'
                id 'org.jmailen.kotlinter'
            }

            repositories {
                jcenter()
            }

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
        """.trimIndent())
    }

    private fun kotlinSourceFile(name: String, content: String) = File(sourceDir, name).apply {
        writeText(content)
    }
}
