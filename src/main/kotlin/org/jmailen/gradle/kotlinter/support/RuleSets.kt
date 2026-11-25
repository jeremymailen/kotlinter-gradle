@file:Suppress("DEPRECATION")

package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import io.github.ktlint.core.cli.ruleset.core.api.RuleSetV2Provider
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider
import org.gradle.api.logging.Logging
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

internal fun resolveRuleProviders(
    providers: Iterable<RuleSetV2Provider>,
    legacyProviders: Iterable<RuleSetProviderV3> = emptyList(),
): Set<RuleV2Provider> {
    val native = providers.sortedBy { if (it.id.value == "standard") 0 else 1 }.flatMap { it.getRuleProviders() }
    val legacy = try {
        legacyProviders.flatMap { it.getRuleProviders() }.map { provider ->
            var ruleClass: Class<*>? = provider.createNewRuleInstance().javaClass
            while (ruleClass != null && ruleClass.name != "com.pinterest.ktlint.rule.engine.core.api.Rule") {
                if (ruleClass.declaredMethods.any { method ->
                        method.name in setOf("beforeVisitChildNodes", "afterVisitChildNodes") &&
                            method.parameterTypes.any { it.name == "org.jetbrains.kotlin.com.intellij.lang.ASTNode" }
                    }
                ) {
                    throw IllegalStateException(LEGACY_ERROR)
                }
                ruleClass = ruleClass.superclass
            }
            provider.toRuleV2Provider()
        }
    } catch (error: LinkageError) {
        throw IllegalStateException(LEGACY_ERROR, error)
    }
    return (native + legacy).toSet()
}

// statically resolve providers from plugin classpath. ServiceLoader#load alone resolves classes lazily which fails when run in parallel
// https://github.com/jeremymailen/kotlinter-gradle/issues/101
val defaultRuleSetProviders: List<RuleSetV2Provider> =
    ServiceLoader.load(RuleSetV2Provider::class.java).toList()

internal const val LEGACY_ERROR =
    "Unable to load a legacy KtLint ruleset. Its binary may be incompatible with KtLint 2.x; migrate the ruleset to the 2.x API."

internal val legacyRuleSetProviders: List<RuleSetProviderV3> = try {
    ServiceLoader.load(RuleSetProviderV3::class.java).toList().also {
        if (it.isNotEmpty()) {
            Logging.getLogger("org.jmailen.gradle.kotlinter.rules").warn(
                "Legacy KtLint rulesets are loaded through the upstream compatibility bridge. " +
                    "KtLint 2.0.0-ALPHA-5 may omit non-autocorrectable violations; migrate these rulesets to the 2.x API.",
            )
        }
    }
} catch (error: ServiceConfigurationError) {
    throw IllegalStateException(LEGACY_ERROR, error)
} catch (error: LinkageError) {
    throw IllegalStateException(LEGACY_ERROR, error)
}
