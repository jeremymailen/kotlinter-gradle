package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleSetsTest {

    @Test
    fun `resolveRuleSets loads from classpath providers`() {
        val rules = resolveRuleProviders(defaultRuleSetProviders)

        assertTrue(rules.isNotEmpty())
    }

    @Test
    fun `resolveRuleSets puts standard rules first`() {
        val standard = TestRuleSetProvider("standard", setOf(TestRule("custom:one")))
        val extra1 = TestRuleSetProvider("extra-one", setOf(TestRule("custom:two")))
        val extra2 = TestRuleSetProvider("extra-two", setOf(TestRule("custom:three")))

        val result = resolveRuleProviders(providers = listOf(extra2, standard, extra1)).map { it.createNewRuleInstance() }

        assertEquals(3, result.size)
        assertEquals(standard.ruleSet.single(), result.first())
        assertTrue(result.containsAll(listOf(extra1.ruleSet.single(), extra2.ruleSet.single())))
    }

    @Test
    fun `test compatibility`() {
        KtLintRuleEngine(
            ruleProviders = resolveRuleProviders(defaultRuleSetProviders),
        ).lint(
            Code.fromSnippet(
                """
                package test

                class KotlinClass {
                    private fun hi() {
                        println("hi")
                    }
                }

                """.trimIndent(),
            ),
            callback = { _ -> },
        )
    }
}

class TestRuleSetProvider(id: String, val ruleSet: Set<Rule>) : RuleSetProviderV3(RuleSetId(id)) {
    override fun getRuleProviders() = ruleSet.map { rule -> RuleProvider(provider = { rule }) }.toSet()
}

class TestRule(id: String) : Rule(RuleId(id), About("maintainer"), emptySet(), emptySet())
