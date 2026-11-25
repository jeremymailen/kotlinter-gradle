package org.jmailen.gradle.kotlinter.functional

import org.jmailen.gradle.kotlinter.functional.utils.repositories
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LegacyRulesTest : WithGradleTest.Kotlin() {
    @Test
    fun `incompatible precompiled rules report migration guidance`() {
        testProjectDir.resolve("settings.gradle") {
            writeText("rootProject.name = 'legacy-test'\ninclude 'rules'\n")
        }
        testProjectDir.resolve("build.gradle") {
            writeText(
                """
                plugins {
                    id 'kotlin'
                    id 'org.jmailen.kotlinter'
                }
                $repositories
                dependencies { ktlint(project(':rules')) }
                """.trimIndent(),
            )
        }
        testProjectDir.resolve("rules/build.gradle") {
            writeText(
                """
                plugins { id 'kotlin' }
                $repositories
                dependencies { compileOnly 'com.pinterest.ktlint:ktlint-cli-ruleset-core:1.8.0' }
                """.trimIndent(),
            )
        }
        testProjectDir.resolve("rules/src/main/kotlin/Legacy.kt") {
            writeText(
                """
                package fixture
                import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
                import com.pinterest.ktlint.rule.engine.core.api.*
                import org.jetbrains.kotlin.com.intellij.lang.ASTNode

                class Provider : RuleSetProviderV3(RuleSetId("legacy")) {
                    override fun getRuleProviders() = setOf(RuleProvider { OldRule() })
                }
                class OldRule : Rule(RuleId("legacy:test"), About("fixture")), RuleAutocorrectApproveHandler {
                    override fun beforeVisitChildNodes(node: ASTNode, emit: (Int, String, Boolean) -> AutocorrectDecision) {
                        emit(node.startOffset, "legacy", false)
                    }
                }
                """.trimIndent(),
            )
        }
        testProjectDir.resolve("rules/src/main/resources/META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3") {
            writeText("fixture.Provider\n")
        }
        testProjectDir.resolve("src/main/kotlin/Example.kt") {
            writeText("class Example\n")
        }
        buildAndFail("lintKotlinMain").apply {
            assertTrue(output.contains("migrate the ruleset to the 2.x API"), output)
        }
    }
}
