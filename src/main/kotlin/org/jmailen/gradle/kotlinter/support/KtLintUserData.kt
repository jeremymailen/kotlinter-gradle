package org.jmailen.gradle.kotlinter.support

fun userData(ktLintParams: KtLintParams): Map<String, String> {
    val userData = mutableMapOf<String, String>()

    ktLintParams.indentSize?.let { indentSize ->
        userData["indent_size"] = indentSize.toString()
    }
    ktLintParams.disabledRules.takeIf { it.isNotEmpty() }?.let { rules ->
        userData["disabled_rules"] = rules.joinToString(",")
    }
    return userData
}
