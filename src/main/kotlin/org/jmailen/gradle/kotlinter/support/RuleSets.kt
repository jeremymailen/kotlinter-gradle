package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import java.util.ServiceLoader

internal fun resolveRuleProviders(
    providers: Iterable<RuleSetProviderV3>,
): Set<RuleProvider> = providers
    .asSequence()
    .sortedWith(
        compareBy {
            when (it.id.value) {
                "standard" -> 0
                else -> 1
            }
        },
    )
    .map(RuleSetProviderV3::getRuleProviders)
    .flatten()
    .toSet()

// statically resolve providers from plugin classpath. ServiceLoader#load alone resolves classes lazily which fails when run in parallel
// https://github.com/jeremymailen/kotlinter-gradle/issues/101
val defaultRuleSetProviders: List<RuleSetProviderV3> =
    ServiceLoader.load(RuleSetProviderV3::class.java).toList()
