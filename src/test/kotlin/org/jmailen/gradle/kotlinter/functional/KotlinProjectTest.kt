package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

internal class KotlinProjectTest : WithGradleTest.Kotlin() {

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var sourceDir: File
    private val pathPattern = "(/.*\\.kt):\\d+:\\d+".toRegex()

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
        kotlinSourceFile(
            "$className.kt",
            """
            class $className {
                private fun hi(){
                    println ("hi")
                }
            }

            """.trimIndent()
        )

        buildAndFail("lintKotlinMain").apply {
            assertTrue(output.contains(".*$className.kt.* Lint error > \\[.*] Missing spacing before \"\\{\"".toRegex()))
            assertTrue(output.contains(".*$className.kt.* Lint error > \\[.*] Unexpected spacing before \"\\(\"".toRegex()))
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

            """.trimIndent()
        )

        build("lintKotlinMain").apply {
            assertEquals(SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `formatKotlin reports formatted and unformatted files`() {
        settingsFile()
        buildFile()
        @Language("kotlin")
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
            
            """.trimIndent()
        )

        build("check").apply {
            assertEquals(SUCCESS, task(":lintKotlin")?.outcome)
        }
    }

    @Test
    fun `tasks up-to-date checks`() {
        settingsFile()
        buildFile()
        kotlinSourceFile(
            "CustomObject.kt",
            """
            object CustomObject
            
            """.trimIndent()
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
    }

    @Test
    fun `plugin is compatible with configuration cache`() {
        settingsFile()
        buildFile()
        kotlinSourceFile(
            "CustomObject.kt",
            """
            object CustomObject
            
            """.trimIndent()
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

    private fun buildFile() = buildFile.apply {
        @Language("groovy")
        val buildscript =
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.4.32'
                id 'org.jmailen.kotlinter'
            }

            repositories {
                mavenCentral()
            }
            """.trimIndent()
        writeText(buildscript)
    }

    private fun kotlinSourceFile(name: String, content: String) = File(sourceDir, name).apply {
        writeText(content)
    }
}
