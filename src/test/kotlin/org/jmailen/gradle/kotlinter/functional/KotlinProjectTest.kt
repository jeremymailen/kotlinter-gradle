package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.jmailen.gradle.kotlinter.functional.utils.editorConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

internal class KotlinProjectTest : WithGradleTest.Kotlin() {

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var sourceDir: File
    private lateinit var editorconfigFile: File
    private val pathPattern = "(.*\\.kt):\\d+:\\d+".toRegex()

    @BeforeEach
    fun setup() {
        settingsFile = testProjectDir.resolve("settings.gradle")
        buildFile = testProjectDir.resolve("build.gradle")
        sourceDir = testProjectDir.resolve("src/main/kotlin/").also(File::mkdirs)
        editorconfigFile = testProjectDir.resolve(".editorconfig")
    }

    @Test
    fun `lintKotlinMain fails when lint errors detected`() {
        settingsFile()
        buildFile()

        val className = "KotlinClass"
        kotlinSourceFile(
            "$className.kt",
            """
            class $className {
                private fun hi(){
                    println ("hi")
                }
            }

            """.trimIndent(),
        )

        buildAndFail("lintKotlinMain").apply {
            assertTrue(output.contains("$className.kt:2:21: Lint error > [standard:curly-spacing] Missing spacing before"))
            assertTrue(output.contains("$className.kt:3:16: Lint error > [standard:paren-spacing] Unexpected spacing before"))
            output.lines().filter { it.contains("Lint error") }.forEach { line ->
                val filePath = pathPattern.find(line)?.groups?.get(1)?.value.orEmpty()
                assertTrue(File(filePath).exists())
            }
            assertEquals(FAILED, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `lintKotlinMain succeeds when no lint errors detected`() {
        settingsFile()
        buildFile()
        kotlinSourceFile(
            "KotlinClass.kt",
            """
            class KotlinClass {
                private fun hi() {
                    println("hi")
                }
            }

            """.trimIndent(),
        )

        build("lintKotlinMain").apply {
            assertEquals(SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `formatKotlin reports formatted and unformatted files`() {
        settingsFile()
        buildFile()
        // language=kotlin
        val kotlinClass =
            """
            import System.*
            
            class KotlinClass{
                private fun hi() {
                    out.println("Hello")
                }
            }
            """.trimIndent()
        kotlinSourceFile("KotlinClass.kt", kotlinClass)

        build("formatKotlin").apply {
            assertEquals(SUCCESS, task(":formatKotlinMain")?.outcome)
            output.lines().filter { it.contains("Format could not fix") }.forEach { line ->
                val filePath = pathPattern.find(line)?.groups?.get(1)?.value.orEmpty()
                assertTrue(File(filePath).exists())
            }
        }
    }

    @Test
    fun `formatKotlin fails when lint errors not automatically fixed and failBuildWhenCannotAutoFormat enabled`() {
        settingsFile()
        buildFileFailBuildWhenCannotAutoFormat()
        // language=kotlin
        val kotlinClass =
            """
            import System.*
            
            class KotlinClass{
                private fun hi() {
                    out.println("Hello")
                }
            }
            """.trimIndent()
        kotlinSourceFile("KotlinClass.kt", kotlinClass)

        buildAndFail("formatKotlin").apply {
            assertEquals(FAILED, task(":formatKotlinMain")?.outcome)
            output.lines().filter { it.contains("Format could not fix") }.forEach { line ->
                val filePath = pathPattern.find(line)?.groups?.get(1)?.value.orEmpty()
                assertTrue(File(filePath).exists())
            }
        }
    }

    @Test
    fun `formatKotlin fails reports formatted and unformatted files when ignoreFailures and failBuildWhenCannotAutoFormat enabled`() {
        settingsFile()
        buildFileIgnoreFailures()
        // language=kotlin
        val kotlinClass =
            """
            import System.*
            
            class KotlinClass{
                private fun hi() {
                    out.println("Hello")
                }
            }
            """.trimIndent()
        kotlinSourceFile("KotlinClass.kt", kotlinClass)

        build("formatKotlin").apply {
            assertEquals(SUCCESS, task(":formatKotlinMain")?.outcome)
            output.lines().filter { it.contains("Format could not fix") }.forEach { line ->
                val filePath = pathPattern.find(line)?.groups?.get(1)?.value.orEmpty()
                assertTrue(File(filePath).exists())
            }
        }
    }

    @Test
    fun `check task runs lintFormat`() {
        settingsFile()
        buildFile()
        kotlinSourceFile(
            "CustomObject.kt",
            """
            object CustomObject
            
            """.trimIndent(),
        )

        build("check").apply {
            assertEquals(SUCCESS, task(":lintKotlin")?.outcome)
        }
    }

    @Test
    fun `tasks up-to-date checks`() {
        settingsFile()
        buildFile()
        editorConfig()
        kotlinSourceFile(
            "CustomObject.kt",
            """
            object CustomObject
            
            """.trimIndent(),
        )

        build("lintKotlin").apply {
            assertEquals(SUCCESS, task(":lintKotlin")?.outcome)
        }
        build("lintKotlin").apply {
            assertEquals(UP_TO_DATE, task(":lintKotlin")?.outcome)
        }

        build("formatKotlin").apply {
            assertEquals(SUCCESS, task(":formatKotlin")?.outcome)
        }
        build("formatKotlin").apply {
            assertEquals(SUCCESS, task(":formatKotlin")?.outcome)
        }

        editorconfigFile.appendText("content=updated")
        build("lintKotlin").apply {
            assertEquals(SUCCESS, task(":lintKotlin")?.outcome)
        }
        build("lintKotlin").apply {
            assertEquals(UP_TO_DATE, task(":lintKotlin")?.outcome)
        }
    }

    @Test
    fun `plugin is compatible with configuration cache`() {
        settingsFile()
        buildFile()
        kotlinSourceFile(
            "CustomObject.kt",
            """
            object CustomObject
            
            """.trimIndent(),
        )

        build("lintKotlin", "--configuration-cache").apply {
            assertEquals(SUCCESS, task(":lintKotlin")?.outcome)
            assertTrue(output.contains("Configuration cache entry stored"))
        }
        build("lintKotlin", "--configuration-cache").apply {
            assertEquals(UP_TO_DATE, task(":lintKotlin")?.outcome)
            assertTrue(output.contains("Configuration cache entry reused."))
        }

        build("formatKotlin", "--configuration-cache").apply {
            assertEquals(SUCCESS, task(":formatKotlin")?.outcome)
            assertTrue(output.contains("Configuration cache entry stored"))
        }
        build("formatKotlin", "--configuration-cache").apply {
            assertEquals(SUCCESS, task(":formatKotlin")?.outcome)
            assertTrue(output.contains("Configuration cache entry reused."))
        }
    }

    private fun settingsFile() = settingsFile.apply {
        writeText("rootProject.name = 'kotlinter'")
    }

    private fun editorConfig() = editorconfigFile.apply {
        writeText(editorConfig)
    }

    private fun buildFile() = buildFile.apply {
        // language=groovy
        val buildscript =
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm'
                id 'org.jmailen.kotlinter'
            }

            repositories {
                mavenCentral()
            }
            """.trimIndent()
        writeText(buildscript)
    }

    private fun buildFileFailBuildWhenCannotAutoFormat() = buildFile.apply {
        // language=groovy
        val buildscript =
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm'
                id 'org.jmailen.kotlinter'
            }

            repositories {
                mavenCentral()
            }
            
            kotlinter {
                failBuildWhenCannotAutoFormat = true
            }
            """.trimIndent()
        writeText(buildscript)
    }

    private fun buildFileIgnoreFailures() = buildFile.apply {
        // language=groovy
        val buildscript =
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm'
                id 'org.jmailen.kotlinter'
            }

            repositories {
                mavenCentral()
            }
            
            kotlinter {
                ignoreFailures = true
                failBuildWhenCannotAutoFormat = true
            }
            """.trimIndent()
        writeText(buildscript)
    }

    private fun kotlinSourceFile(name: String, content: String) = File(sourceDir, name).apply {
        writeText(content)
    }

    private fun fileWithFailingExperimentalRule() {
        kotlinSourceFile(
            "ExperimentalRuleViolations.kt",
            """
            val variable = "should not contain '()'".count() { it == 'x' }
    
            """.trimIndent(),
        )
    }
}
