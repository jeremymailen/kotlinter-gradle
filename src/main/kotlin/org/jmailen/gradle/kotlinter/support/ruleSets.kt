package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.ruleset.experimental.ExperimentalRuleSetProvider
import java.util.ServiceLoader
import kotlin.comparisons.compareBy

fun resolveRuleSets(
    providers: Iterable<RuleSetProvider>,
    includeExperimentalRules: Boolean = false
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

fun defaultProviders() = ServiceLoader.load(RuleSetProvider::class.java)
