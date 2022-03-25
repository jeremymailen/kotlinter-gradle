package org.jmailen.gradle.kotlinter.support

fun userData(ktLintParams: KtLintParams): Map<String, String> {
    val userData = mutableMapOf<String, String>()

    ktLintParams.disabledRules.takeIf { it.isNotEmpty() }?.let { rules ->
        userData["disabled_rules"] = rules.joinToString(",")
    }
    return userData
}
