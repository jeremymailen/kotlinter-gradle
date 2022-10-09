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
                    
                    repositories {
                        mavenCentral()
                    }
                    
                    """.trimIndent()
                writeText(buildScript)
            }
        }
    }

    @Test
    fun `extension configures ignoreFailures`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val script =
                """
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
    fun `extension configures disabledRules`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val script =
                """
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
                
                repositories {
                    mavenCentral()
                }
                
                tasks.whenTaskAdded {
                    // configure all tasks eagerly
                }
                
                kotlinter {
                    disabledRules = ["filename"]
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

    @Test
    fun `can override ktlint version`() {
        projectRoot.resolve("build.gradle") {
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
                
                kotlinter {
                    ktlintVersion = "0.46.0"
                }
                
                """.trimIndent()
            writeText(buildScript)
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("FileName"))
        }

        buildAndFail("lintKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome) { "should fail due to incompatibility" }
            val expectedMessage = "Caused by: java.lang.NoSuchMethodError: 'void com.pinterest.ktlint.core.KtLint\$ExperimentalParams."
            assertTrue(output.contains(expectedMessage)) { "should explain the incompatibility" }
        }
        // remove `--configuration-cache-problems=warn` when upgrading to Gradle 7.6 https://github.com/gradle/gradle/issues/17470
        build("dependencies", "--configuration", "ktlint", "--configuration-cache-problems=warn").apply {
            assertTrue(output.contains("com.pinterest:ktlint:0.46.0")) {
                "should include overridden ktlin version in `ktlint` configuration"
            }
        }
    }
}
