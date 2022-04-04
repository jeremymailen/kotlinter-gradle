package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

internal class EditorConfigTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File

    @Before
    fun setUp() {
        projectRoot = testProjectDir.root.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                // language=groovy
                val buildScript =
                    """
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
    fun `lintTask uses default indentation if editorconfig absent`() {
        projectRoot.resolve("src/main/kotlin/FourSpacesByDefault.kt") {
            writeText(
                """ |
                    |object FourSpacesByDefault {
                    |    val text: String
                    |}
                    |
                """.trimMargin()
            )
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `plugin respects disabled_rules set in editorconfig`() {
        projectRoot.resolve(".editorconfig") {
            appendText(
                """
                    [*.{kt,kts}]
                    disabled_rules=filename
                """.trimIndent()
            )
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `plugin respects 'indent_size' set  in editorconfig`() {
        projectRoot.resolve(".editorconfig") {
            appendText(
                """
                    [*.{kt,kts}]
                    indent_size = 6
                """.trimIndent()
            )
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            // language=kotlin
            val content =
                """
                class WrongFileName {

                  fun unnecessarySpace () = 2
                }

                """.trimIndent()

            writeText(content)
        }

        buildAndFail("lintKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome)
            assertTrue(output.contains("[indent] Unexpected indentation (2) (should be 6)"))
        }
    }
}
