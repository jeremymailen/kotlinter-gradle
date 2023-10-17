package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class KotlinJsProjectTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File

    @BeforeEach
    fun setup() {
        projectRoot = testProjectDir.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                // language=groovy
                writeText(
                    """
                    plugins {
                        id 'org.jetbrains.kotlin.js'
                        id 'org.jmailen.kotlinter'
                    }
                    
                    repositories.mavenCentral()
                    
                    kotlin {
                        js(IR) {
                            browser()
                            binaries.executable()
                        }
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `lintKotlin passes when on valid kotlin files`() {
        projectRoot.resolve("src/main/kotlin/FixtureFileName.kt") {
            writeText(kotlinClass("FixtureFileName"))
        }
        projectRoot.resolve("src/test/kotlin/TestFileName.kt") {
            writeText(kotlinClass("TestFileName"))
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinTest")?.outcome)
        }
    }

    @Test
    fun `lintKotlin fails when lint errors detected`() {
        projectRoot.resolve("src/main/kotlin/FixtureFileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }
        projectRoot.resolve("src/test/kotlin/FixtureTestFileName.kt") {
            writeText(kotlinClass("DifferentTestClassName"))
        }

        buildAndFail("lintKotlin", "--continue").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome)
            assertTrue(
                output.contains("Lint error > [standard:filename] File 'FixtureFileName.kt' contains a single top level declaration"),
            )
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinTest")?.outcome)
            assertTrue(
                output.contains("Lint error > [standard:filename] File 'FixtureTestFileName.kt' contains a single top level declaration"),
            )
        }
    }

    @Test
    fun `formatKotlin reports formatted and unformatted files`() {
        projectRoot.resolve("src/main/kotlin/FixtureClass.kt") {
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
        projectRoot.resolve("src/test/kotlin/FixtureTestClass.kt") {
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
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinMain")?.outcome)
            assertTrue(output.contains("FixtureClass.kt:3:19: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinTest")?.outcome)
            assertTrue(output.contains("FixtureTestClass.kt:3:23: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureTestClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
        }
    }

    @Test
    fun `formatKotlin fails when lint errors not automatically fixed and failBuildWhenCannotAutoFormat enabled`() {
        projectRoot.resolve("src/main/kotlin/FixtureClass.kt") {
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
        projectRoot.resolve("src/test/kotlin/FixtureTestClass.kt") {
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
            assertEquals(TaskOutcome.FAILED, task(":formatKotlinMain")?.outcome)
            assertTrue(output.contains("FixtureClass.kt:3:19: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
            assertEquals(TaskOutcome.FAILED, task(":formatKotlinTest")?.outcome)
            assertTrue(output.contains("FixtureTestClass.kt:3:23: Format fixed > [standard:curly-spacing] Missing spacing before \"{\""))
            assertTrue(output.contains("FixtureTestClass.kt:1:1: Format could not fix > [standard:no-wildcard-imports] Wildcard import"))
        }
    }
}
