package org.jmailen.gradle.kotlinter.support

import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import java.util.ServiceLoader
import kotlin.comparisons.compareBy

fun resolveRuleSets(providers: Iterable<RuleSetProvider> = ServiceLoader.load(RuleSetProvider::class.java)): List<RuleSet> {
    return providers.map { it.get() }.sortedWith(compareBy {
        when (it.id) {
            "standard" -> 0
            else -> 1
        }
    })
}
