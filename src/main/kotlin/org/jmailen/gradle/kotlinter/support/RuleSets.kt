package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import java.util.ServiceLoader

internal fun resolveRuleProviders(
    includeExperimentalRules: Boolean = false,
): Set<RuleProvider> = defaultRuleSetProviders()
    .asSequence()
    .filter { includeExperimentalRules || it.id != "experimental" }
    .sortedWith(
        compareBy {
            when (it.id) {
                "standard" -> 0
                else -> 1
            }
        },
    )
    .map(RuleSetProviderV2::getRuleProviders)
    .flatten()
    .toSet()

/**
 * Make sure this gets called with proper classpath (i.e. within Gradle Worker class)
 * `toList()` call prevents concurrency issues: https://github.com/jeremymailen/kotlinter-gradle/issues/101
 */
private fun defaultRuleSetProviders(): List<RuleSetProviderV2> =
    ServiceLoader.load(RuleSetProviderV2::class.java).toList()
