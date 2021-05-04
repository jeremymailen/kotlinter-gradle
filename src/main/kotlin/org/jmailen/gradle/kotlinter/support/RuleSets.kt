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
        .sortedWith(
            compareBy {
                when (it.id) {
                    "standard" -> 0
                    else -> 1
                }
            }
        )
}

// statically resolve providers from plugin classpath. ServiceLoader#load alone resolves classes lazily which fails when run in parallel
// https://github.com/jeremymailen/kotlinter-gradle/issues/101
val defaultRuleSetProviders: List<RuleSetProvider> =
    ServiceLoader.load(RuleSetProvider::class.java).toList()
