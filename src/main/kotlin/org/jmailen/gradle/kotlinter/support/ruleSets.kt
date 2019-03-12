package org.jmailen.gradle.kotlinter.support

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import com.github.shyiko.ktlint.ruleset.experimental.ExperimentalRuleSetProvider
import java.util.ServiceLoader
import kotlin.comparisons.compareBy

fun resolveRuleSets(
    includeExperimentalRules: Boolean = false,
    providers: Iterable<RuleSetProvider> = ServiceLoader.load(RuleSetProvider::class.java)
): List<RuleSet> {
    return providers
        .filter { includeExperimentalRules || it !is ExperimentalRuleSetProvider }
        .map { it.get() }
        .sortedWith(compareBy {
            when (it.id) {
                "standard" -> 0
                else -> 1
            }
        })
}
