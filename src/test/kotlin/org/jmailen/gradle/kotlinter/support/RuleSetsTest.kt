package org.jmailen.gradle.kotlinter.support

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSetsTest {

    @Test
    fun `resolveRuleSets loads from classpath providers`() {
        val result = resolveRuleSets()

        assertEquals(listOf("standard"), result.map { it.id })
    }

    @Test
    fun `resolveRuleSets loads from classpath providers including experimental rules`() {
        val result = resolveRuleSets(includeExperimentalRules = true)

        assertEquals(listOf("standard", "experimental"), result.map { it.id })
    }

    @Test
    fun `resolveRuleSets puts standard rules first`() {
        val standard = TestRuleSetProvider(RuleSet("standard", TestRule("one")))
        val extra1 = TestRuleSetProvider(RuleSet("extra-one", TestRule("two")))
        val extra2 = TestRuleSetProvider(RuleSet("extra-two", TestRule("three")))

        val result = resolveRuleSets(providers = listOf(extra2, standard, extra1))

        assertEquals(3, result.size)
        assertEquals(standard.ruleSet, result.first())
        assertTrue(result.containsAll(listOf(extra1.ruleSet, extra2.ruleSet)))
    }

    @Test
    fun `test compatibility`() {
        KtLint.lint("""fun someFunc() = """"", resolveRuleSets(), {})
    }
}

class TestRuleSetProvider(val ruleSet: RuleSet) : RuleSetProvider {
    override fun get() = ruleSet
}

class TestRule(id: String) : Rule(id) {
    override fun visit(node: ASTNode, autoCorrect: Boolean, emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {}
}
