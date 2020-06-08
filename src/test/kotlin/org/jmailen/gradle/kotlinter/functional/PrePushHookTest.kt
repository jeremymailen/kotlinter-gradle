package org.jmailen.gradle.kotlinter.functional

import java.io.File
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.jmailen.gradle.kotlinter.tasks.InstallPrePushHookTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class PrePushHookTest : WithGradleTest.Kotlin() {
    lateinit var projectRoot: File

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
    fun `installKotlinterPrePushHook fails when dotgit dir not found`() {
        buildAndFail("installKotlinterPrePushHook").apply {
            assertTrue(output.contains(Regex("\\.git directory not found at .*/\\.git")))
            assertEquals(FAILED, task(":installKotlinterPrePushHook")?.outcome)
        }
    }

    @Test
    fun `installKotlinterPrePushHook installs hook in project without hook directory`() {
        File(testProjectDir.root, ".git").apply { mkdir() }

        build("installKotlinterPrePushHook").apply {
            assertEquals(SUCCESS, task(":installKotlinterPrePushHook")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/pre-push") {
                    assertTrue(readText().contains("${'$'}GRADLEW lintKotlin"))
                    assertTrue(canExecute())
                }
            }
        }
    }

    @Test
    fun `installKotlinterPrePushHook installs hook in project with existing pre-push hook`() {
        val existingHook = """
                #!/bin/bash
                This is some existing hook
            """.trimIndent()
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks/pre-push").apply {
            writeText(existingHook)
        }

        build("installKotlinterPrePushHook").apply {
            assertEquals(SUCCESS, task(":installKotlinterPrePushHook")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/pre-push") {
                    val prePushHookContents = readText()
                    assertTrue(prePushHookContents.startsWith(existingHook))
                    assertTrue(prePushHookContents.contains("${'$'}GRADLEW lintKotlin"))
                }
            }
        }
    }

    @Test
    fun `installKotlinterPrePushHook updates previously installed kotlinter hook`() {
        val placeholder = "Not actually the hook, just a placeholder"
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks/pre-push").apply {
            writeText("""
                ${InstallPrePushHookTask.startHook}
                $placeholder
                ${InstallPrePushHookTask.endHook}
            """.trimIndent())
        }

        build("installKotlinterPrePushHook").apply {
            assertEquals(SUCCESS, task(":installKotlinterPrePushHook")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/pre-push") {
                    val prePushHookContents = readText()
                    assertTrue(prePushHookContents.contains("${'$'}GRADLEW lintKotlin"))
                    assertFalse(prePushHookContents.contains(placeholder))
                }
            }
        }
    }

    @Test
    fun `Repeatedly updating doesn't change hook`() {
        File(testProjectDir.root, ".git").apply { mkdir() }
        File(testProjectDir.root, ".git/hooks").apply { mkdir() }

        lateinit var hookContent: String
        build("installKotlinterPrePushHook").apply {
            assertEquals(SUCCESS, task(":installKotlinterPrePushHook")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/pre-push") {
                    hookContent = readText()
                    println(hookContent)
                    assertTrue(hookContent.contains("${'$'}GRADLEW lintKotlin"))
                    assertTrue(canExecute())
                }
            }
        }

        build("installKotlinterPrePushHook").apply {
            assertEquals(SUCCESS, task(":installKotlinterPrePushHook")?.outcome)
            testProjectDir.root.apply {
                resolve(".git/hooks/pre-push") {
                    assertEquals(hookContent, readText())
                }
            }
        }
    }
}
