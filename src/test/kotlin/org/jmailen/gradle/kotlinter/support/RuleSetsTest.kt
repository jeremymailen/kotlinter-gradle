package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleSetsTest {

    @Test
    fun `resolveRuleSets loads from classpath providers`() {
        val standardOnly = resolveRuleProviders(defaultRuleSetProviders, includeExperimentalRules = false)
        val withExperimentalRules = resolveRuleProviders(defaultRuleSetProviders, includeExperimentalRules = true)

        assertTrue(standardOnly.isNotEmpty())
        assertTrue(standardOnly.size < withExperimentalRules.size)
    }

    @Test
    fun `resolveRuleSets puts standard rules first`() {
        val standard = TestRuleSetProvider("standard", setOf(TestRule("one")))
        val extra1 = TestRuleSetProvider("extra-one", setOf(TestRule("two")))
        val extra2 = TestRuleSetProvider("extra-two", setOf(TestRule("three")))

        val result = resolveRuleProviders(providers = listOf(extra2, standard, extra1)).map { it.createNewRuleInstance() }

        assertEquals(3, result.size)
        assertEquals(standard.ruleSet.single(), result.first())
        assertTrue(result.containsAll(listOf(extra1.ruleSet.single(), extra2.ruleSet.single())))
    }

    @Test
    fun `test compatibility`() {
        KtLint.lint(
            KtLint.ExperimentalParams(
                fileName = "/tmp/src/test/KotlinClass.kt",
                text = """
                    package test

                    class KotlinClass {
                        private fun hi() {
                            println("hi")
                        }
                    }

                """.trimIndent(),
                ruleProviders = resolveRuleProviders(defaultRuleSetProviders),
                cb = { _, _ -> },
            ),
        )
    }
}

class TestRuleSetProvider(id: String, val ruleSet: Set<Rule>) : RuleSetProviderV2(
    id = id,
    about = About(
        maintainer = "stub-maintainer",
        description = "stub-description",
        license = "stub-license",
        repositoryUrl = "stub-repositoryUrl",
        issueTrackerUrl = "stub-issueTrackerUrl",
    ),
) {
    override fun getRuleProviders() = ruleSet.map { rule -> RuleProvider(provider = { rule }) }.toSet()
}

class TestRule(id: String) : Rule(id)
