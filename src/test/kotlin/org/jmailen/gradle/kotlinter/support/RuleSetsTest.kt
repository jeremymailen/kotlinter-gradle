package org.jmailen.gradle.kotlinter.support

import io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider
import io.github.ktlint.core.rule.engine.api.Code
import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleSetId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider
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

class TestRuleSetProvider(id: String, val ruleSet: Set<RuleV2>) : RuleSetV2Provider(RuleSetId(id)) {
    override fun getRuleProviders() = ruleSet.map { rule -> RuleV2Provider(provider = { rule }) }.toSet()
}

class TestRule(id: String) : RuleV2(RuleId(id), About("maintainer"))
