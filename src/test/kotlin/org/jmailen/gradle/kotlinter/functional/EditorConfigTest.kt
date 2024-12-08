package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.KotlinterConfig
import org.jmailen.gradle.kotlinter.functional.utils.editorConfig
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.repositories
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

internal class EditorConfigTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File

    private fun setup(kotlinterConfig: KotlinterConfig) {
        projectRoot = testProjectDir.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                val buildscript = when (kotlinterConfig) {
                    KotlinterConfig.DEFAULT ->
                        """
                        plugins {
                            id 'kotlin'
                            id 'org.jmailen.kotlinter'
                        }
                        $repositories
                        """.trimIndent()
                    KotlinterConfig.FAIL_BUILD_WHEN_CANNOT_AUTO_FORMAT ->
                        """
                        plugins {
                            id 'org.jetbrains.kotlin.js'
                            id 'org.jmailen.kotlinter'
                        }
                        $repositories

                        kotlin {
                            js {
                                nodejs()
                            }
                        }

                        kotlinter {
                            failBuildWhenCannotAutoFormat = true
                        }
                        """.trimIndent()
                    KotlinterConfig.IGNORE_FAILURES ->
                        """
                        plugins {
                            id 'org.jetbrains.kotlin.js'
                            id 'org.jmailen.kotlinter'
                        }
                        $repositories

                        kotlin {
                            js {
                                nodejs()
                            }
                        }

                        kotlinter {
                            ignoreFailures = true
                            failBuildWhenCannotAutoFormat = true
                        }
                        """.trimIndent()
                }
                writeText(buildscript)
            }
        }
    }

    @Test
    fun `lintTask uses default indentation if editorconfig absent`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve("src/main/kotlin/FourSpacesByDefault.kt") {
            writeText(
                """ |package com.example
                    |
                    |object FourSpacesByDefault {
                    |    val text: String
                    |}
                    |
                """.trimMargin(),
            )
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
        }
    }

    @Test
    fun `plugin respects disabled_rules set in editorconfig`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve(".editorconfig") {
            appendText(
                // language=editorconfig
                """
                    [*.{kt,kts}]
                    ktlint_standard_filename = disabled
                """.trimIndent(),
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
    fun `plugin respects 'indent_size' set in editorconfig`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve(".editorconfig") {
            appendText(
                // language=editorconfig
                """
                    [*.{kt,kts}]
                    indent_size = 6
                """.trimIndent(),
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
            assertTrue(output.contains("[standard:indent] Unexpected indentation (2) (should be 6)"))
        }
    }

    @Test
    fun `editorconfig changes are taken into account when adds lint issues`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve(".editorconfig") {
            writeText(
                // language=editorconfig
                """
                    [*.{kt,kts}]
                    ktlint_standard_filename = disabled
                """.trimIndent(),
            )
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }
        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertFalse(output.contains("resetting KtLint caches"))
        }

        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }
        buildAndFail("lintKotlin", "--info").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome)
            assertTrue(output.contains("[standard:filename] File 'FileName.kt' contains a single top level declaration"))
            assertTrue(output.contains("resetting KtLint caches"))
        }

        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("FileName"))
        }
        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertFalse(output.contains("resetting KtLint caches"))
        }
        build("lintKotlin").apply {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":lintKotlinMain")?.outcome)
            assertFalse(output.contains("resetting KtLint caches"))
        }
    }

    @Test
    fun `editorconfig changes are taken into account when removes lint issues`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }
        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }
        buildAndFail("lintKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome)
            assertTrue(output.contains("[standard:filename] File 'FileName.kt' contains a single top level declaration"))
        }

        projectRoot.resolve(".editorconfig") {
            writeText(
                // language=editorconfig
                """
                    [*.{kt,kts}]
                    ktlint_standard_filename = disabled
                """.trimIndent(),
            )
        }
        build("lintKotlin", "--info").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlinMain")?.outcome)
            assertTrue(output.contains("resetting KtLint caches"))
        }
    }

    @Test
    fun `editorconfig changes are taken for format task re-runs`() {
        setup(KotlinterConfig.DEFAULT)
        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }

        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }
        build("formatKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinMain")?.outcome)
            assertTrue(
                output.contains("Format could not fix > [standard:filename] File 'FileName.kt' contains a single top level declaration"),
            )
        }

        projectRoot.resolve(".editorconfig") {
            writeText(
                // language=editorconfig
                """
                    [*.{kt,kts}]
                    ktlint_standard_filename = disabled
                """.trimIndent(),
            )
        }
        build("formatKotlin", "--info").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinMain")?.outcome)
            assertFalse(output.contains("Format could not fix"))
        }
    }

    @Test
    fun `editorconfig changes are taken for format task re-runs when failBuildWhenCannotAutoFormat configured`() {
        setup(KotlinterConfig.FAIL_BUILD_WHEN_CANNOT_AUTO_FORMAT)
        projectRoot.resolve(".editorconfig") {
            writeText(editorConfig)
        }

        projectRoot.resolve("src/main/kotlin/FileName.kt") {
            writeText(kotlinClass("DifferentClassName"))
        }
        buildAndFail("formatKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":formatKotlinMain")?.outcome)
            assertTrue(
                output.contains("Format could not fix > [standard:filename] File 'FileName.kt' contains a single top level declaration"),
            )
        }

        projectRoot.resolve(".editorconfig") {
            writeText(
                // language=editorconfig
                """
                    [*.{kt,kts}]
                    ktlint_standard_filename = disabled
                """.trimIndent(),
            )
        }
        build("formatKotlin", "--info").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":formatKotlinMain")?.outcome)
        }
    }
}
