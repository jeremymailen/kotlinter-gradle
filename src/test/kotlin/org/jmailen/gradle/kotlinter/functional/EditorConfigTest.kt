package org.jmailen.gradle.kotlinter.functional

import groovy.util.GroovyTestCase.assertEquals
import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class EditorConfigTest : WithGradleTest.Kotlin() {

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
    }

    @Test
    fun `lintTask uses default indentation if editorconfig absent`() {
        projectRoot.resolve("src/main/kotlin/FourSpacesByDefault.kt") {
            writeText(
                """ |
                    |    object FourSpacesByDefault
                    |        
                """.trimMargin()
            )
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `plugin respects indentSize set in editorconfig`() {
        projectRoot.resolve(".editorconfig") {
            appendText(
                """
                    [*.{kt,kts}]
                    indent_size = 2
                """.trimIndent()
            )
        }
        projectRoot.resolve("src/main/kotlin/TwoSpaces.kt") {
            writeText(
                """ |
                    |  object TwoSpaces
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
    fun `plugin extension properties take precedence over editorconfig values`() {
        projectRoot.resolve(".editorconfig") {
            appendText(
                """
                    [*.{kt,kts}]
                    disabled_rules=filename
                    indent_size = 2
                """.trimIndent()
            )
        }
        projectRoot.resolve("build.gradle") {
            appendText(
                """
                    kotlinter {
                        disabledRules = ['paren-spacing']  
                        indentSize = 6
                    }
                
                """.trimIndent()
            )
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            @Language("kotlin")
            val content = """
                class WrongFileName {

                  fun unnecessarySpace () = 2
                }

                """.trimIndent()

            writeText(content)
        }

        buildAndFail("lintKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome)
            assertTrue(output.contains("[filename] class WrongFileName should be declared in a file named WrongFileName.kt"))
            assertTrue(output.contains("[indent] Unexpected indentation (2) (it should be 6)"))
        }
    }
}
