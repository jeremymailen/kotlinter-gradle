package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.KotlinterConfig
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
class KotlinJsProjectTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File
    private fun setup(kotlinterConfig: KotlinterConfig) {
        val kotlinMultiplatformJs =
            """
            plugins {
                id 'org.jetbrains.kotlin.multiplatform'
                id 'org.jmailen.kotlinter'
            }

            repositories.mavenCentral()

            kotlin {
                js {
                    browser()
                    binaries.executable()
                }
            }
            """.trimIndent()

        projectRoot = testProjectDir.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                val buildscript = when (kotlinterConfig) {
                    KotlinterConfig.DEFAULT -> kotlinMultiplatformJs

                    KotlinterConfig.FAIL_FORMAT_FAILURES ->
                        """
                        $kotlinMultiplatformJs
                        
                        kotlinter {
                            ignoreFormatFailures = false
                        }
                        """.trimIndent()

                    KotlinterConfig.IGNORE_LINT_FAILURES ->
                        """
                        $kotlinMultiplatformJs
                        
                        kotlinter {
                            ignoreLintFailures = true
                        }
                        """.trimIndent()
                }
                writeText(buildscript)
            }
        }
    }

    @Test
    fun `lintKotlin passes when on valid kotlin files`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve("src/jsMain/kotlin/FixtureFileName.kt") {
            writeText(kotlinClass("FixtureFileName"))
        }
        projectRoot.resolve("src/jsTest/kotlin/TestFileName.kt") {
            writeText(kotlinClass("TestFileName"))
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinJsMain")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinJsTest")?.outcome)
        }
    }

    @Test
    fun `lintKotlin fails when lint errors detected`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve("src/jsMain/kotlin/FixtureFileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }
        projectRoot.resolve("src/jsTest/kotlin/FixtureTestFileName.kt") {
            writeText(kotlinClass("DifferentTestClassName"))
        }

        buildAndFail("lintKotlin", "--continue").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinJsMain")?.outcome)
            assertTrue(
                output.contains("Lint error > [standard:filename] File 'FixtureFileName.kt' contains a single top level declaration"),
            )
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinJsTest")?.outcome)
            assertTrue(
                output.contains("Lint error > [standard:filename] File 'FixtureTestFileName.kt' contains a single top level declaration"),
            )
        }
    }

    @Test
    fun `formatKotlin reports formatted and unformatted files`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve("src/jsMain/kotlin/FixtureClass.kt") {
            // language=kotlin
            val kotlinClass =
                """
                import System.*
                
                class FixtureClass{
                    private fun hi() {
                        out.println("Hello")
                    }
                }
                """.trimIndent()
            writeText(kotlinClass)
        }
        projectRoot.resolve("src/jsTest/kotlin/FixtureTestClass.kt") {
            // language=kotlin
            val kotlinClass =
                """
                import System.*
                
                class FixtureTestClass{
                    private fun hi() {
                        out.println("Hello")
                    }
                }
                """.trimIndent()
            writeText(kotlinClass)
        }
        build("formatKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinJsMain")?.outcome)
            assertTrue(output.contains("FixtureClass.kt:3:19: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinJsTest")?.outcome)
            assertTrue(output.contains("FixtureTestClass.kt:3:23: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureTestClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
        }
    }

    @Test
    fun `formatKotlin fails when lint errors not automatically fixed and ignoreFormatFailures false`() {
        setup(KotlinterConfig.FAIL_FORMAT_FAILURES)
        projectRoot.resolve("src/jsMain/kotlin/FixtureClass.kt") {
            // language=kotlin
            val kotlinClass =
                """
                import System.*
                
                class FixtureClass{
                    private fun hi() {
                        out.println("Hello")
                    }
                }
                """.trimIndent()
            writeText(kotlinClass)
        }
        projectRoot.resolve("src/jsTest/kotlin/FixtureTestClass.kt") {
            // language=kotlin
            val kotlinClass =
                """
                import System.*
                
                class FixtureTestClass{
                    private fun hi() {
                        out.println("Hello")
                    }
                }
                """.trimIndent()
            writeText(kotlinClass)
        }
        buildAndFail("formatKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":formatKotlinJsMain")?.outcome)
            assertTrue(output.contains("FixtureClass.kt:3:19: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
            assertEquals(TaskOutcome.FAILED, task(":formatKotlinJsTest")?.outcome)
            assertTrue(output.contains("FixtureTestClass.kt:3:23: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureTestClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
        }
    }
}
