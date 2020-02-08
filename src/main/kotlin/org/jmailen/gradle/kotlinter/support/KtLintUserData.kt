package org.jmailen.gradle.kotlinter.support

fun userData(ktLintParams: KtLintParams): Map<String, String> {
    val userData = mutableMapOf(
        "indent_size" to ktLintParams.indentSize.toString(),
        "continuation_indent_size" to ktLintParams.continuationIndentSize.toString()
    )
    ktLintParams.disabledRules.takeIf { it.isNotEmpty() }?.let { rules ->
        userData["disabled_rules"] = rules.joinToString(",")
    }
    return userData
}
