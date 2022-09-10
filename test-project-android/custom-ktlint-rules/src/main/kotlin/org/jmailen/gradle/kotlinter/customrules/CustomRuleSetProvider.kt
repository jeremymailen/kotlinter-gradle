package org.jmailen.gradle.kotlinter.customrules

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.core.RuleSetProviderV2

class CustomRuleSetProvider : RuleSetProviderV2(
    "custom-ktlint-rules",
    about = NO_ABOUT,
) {

    override fun getRuleProviders() = setOf(
        RuleProvider { NoNewLineBeforeReturnTypeRule() },
    )
}
