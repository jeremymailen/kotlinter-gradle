package org.jmailen.rulesets

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.ruleset.standard.NoWildcardImportsRule

class NoWildcardImportsRuleSetProvider : RuleSetProvider {

    override fun get() = RuleSet(
        "no-wildcard-imports",
        NoWildcardImportsRule()
    )
}
