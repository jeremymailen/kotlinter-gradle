package org.jmailen.gradle.kotlinter.functional

import java.io.File
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.jmailen.gradle.kotlinter.tasks.InstallHookTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

abstract class InstallHookTaskTest(
    private val taskName: String,
    private val hookFile: String
) : WithGradleTest.Kotlin() {
    private lateinit var projectRoot: File

    @Before
    fun setup() {
        projectRoot = testProjectDir.root.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                writeText(
                    """
                        plugins {
                            id("kotlin")
                            id("org.jmailen.kotlinter")
                        }
                    """.trimIndent()
                )
            }

            build("wrapper")
        }
    }

    @Test
    fun `fails when dotgit dir not found`() {
        buildAndFail(taskName).apply {
            assertTrue(output.contains(Regex("\\.git directory not found at .*/\\.git")))
            assertEquals(FAILED, task(":$taskName")?.outcome)
        }
    }

    @Test
    fun `installs hook in project without hook directory`() {
        File(testProjectDir.root, ".git").apply { mkdir() }

        build(taskName).apply {
            assertEquals(SUCCESS, task(":$taskName")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/$hookFile") {
                    assertTrue(readText().contains("${'$'}GRADLEW formatKotlin"))
                    assertTrue(canExecute())
                }
            }
        }
    }

    @Test
    fun `installs hook in project with existing hook`() {
        val existingHook =
            """
                #!/bin/bash
                This is some existing hook
            """.trimIndent()
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks/$hookFile").apply {
            writeText(existingHook)
        }

        build(taskName).apply {
            assertEquals(SUCCESS, task(":$taskName")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/$hookFile") {
                    val hookContents = readText()
                    assertTrue(hookContents.startsWith(existingHook))
                    assertTrue(hookContents.contains("${'$'}GRADLEW formatKotlin"))
                }
            }
        }
    }

    @Test
    fun `updates previously installed kotlinter hook`() {
        val placeholder = "Not actually the hook, just a placeholder"
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks/$hookFile").apply {
            writeText(
                """
                ${InstallHookTask.startHook}
                $placeholder
                ${InstallHookTask.endHook}
                """.trimIndent()
            )
        }

        build(taskName).apply {
            assertEquals(SUCCESS, task(":$taskName")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/$hookFile") {
                    val hookContents = readText()
                    assertTrue(hookContents.contains("${'$'}GRADLEW formatKotlin"))
                    assertFalse(hookContents.contains(placeholder))
                }
            }
        }
    }

    @Test
    fun `Updates changed hook file`() {
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }

        build(taskName).apply {
            assertEquals(SUCCESS, task(":$taskName")?.outcome)
        }

        File(testProjectDir.root, ".git/hooks/$hookFile").apply {
            writeText(readText().replace("GRADLEW", "FOOBAR"))
        }

        build(taskName).apply {
            assertEquals(SUCCESS, task(":$taskName")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/$hookFile") {
                    assertTrue(readText().contains("${'$'}GRADLEW formatKotlin"))
                }
            }
        }
    }

    @Test
    fun `Repeatedly updating doesn't change hook`() {
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }

        lateinit var hookContent: String
        build(taskName).apply {
            assertEquals(SUCCESS, task(":$taskName")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/$hookFile") {
                    hookContent = readText()
                    assertTrue(hookContent.contains("${'$'}GRADLEW formatKotlin"))
                    assertTrue(canExecute())
                }
            }
        }

        val hookLastModified = File(testProjectDir.root, ".git/hooks/$hookFile").lastModified()

        build(taskName).apply {
            assertEquals(UP_TO_DATE, task(":$taskName")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/$hookFile") {
                    assertEquals(hookContent, readText())
                    assertEquals(hookLastModified, lastModified())
                }
            }
        }
    }
}

class InstallPrePushHookTaskTest : InstallHookTaskTest("installKotlinterPrePushHook", "pre-push")
