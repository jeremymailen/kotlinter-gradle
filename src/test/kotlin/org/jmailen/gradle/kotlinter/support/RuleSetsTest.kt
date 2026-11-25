@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.jmailen.gradle.kotlinter.support

import com.intellij.lang.ASTNode
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider
import io.github.ktlint.core.rule.engine.api.Code
import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleSetId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.pinterest.ktlint.rule.engine.core.api.Rule as LegacyRule
import com.pinterest.ktlint.rule.engine.core.api.RuleId as LegacyRuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider as LegacyRuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId as LegacyRuleSetId

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

    @Test
    fun `legacy and native rules run together`() {
        val errors = mutableListOf<String>()
        KtLintRuleEngine(
            ruleProviders = resolveRuleProviders(defaultRuleSetProviders, listOf(TestLegacyProvider())),
        ).lint(Code.fromSnippet("var legacyValue = 1\n")) {
            errors.add(it.ruleId.value)
        }
        assertTrue(errors.contains("legacy-test:no-var"))
    }

    @Test
    fun `legacy linkage failures explain migration requirement`() {
        val provider = object : RuleSetProviderV3(LegacyRuleSetId("broken")) {
            override fun getRuleProviders(): Set<LegacyRuleProvider> = throw NoClassDefFoundError("shaded ASTNode")
        }
        val error = assertThrows(IllegalStateException::class.java) {
            resolveRuleProviders(defaultRuleSetProviders, listOf(provider))
        }
        assertTrue(error.message!!.contains("migrate the ruleset to the 2.x API"))
    }
}

class TestRuleSetProvider(id: String, val ruleSet: Set<RuleV2>) : RuleSetV2Provider(RuleSetId(id)) {
    override fun getRuleProviders() = ruleSet.map { rule -> RuleV2Provider(provider = { rule }) }.toSet()
}

class TestRule(id: String) : RuleV2(RuleId(id), About("maintainer"))

class TestLegacyProvider : RuleSetProviderV3(LegacyRuleSetId("legacy-test")) {
    override fun getRuleProviders() = setOf(LegacyRuleProvider { TestLegacyRule() })
}

class TestLegacyRule :
    LegacyRule(LegacyRuleId("legacy-test:no-var"), About("maintainer")),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(node: ASTNode, emit: (Int, String, Boolean) -> AutocorrectDecision) {
        if (node.elementType == ElementType.VAR_KEYWORD) emit(node.startOffset, "legacy var", true)
    }
}
