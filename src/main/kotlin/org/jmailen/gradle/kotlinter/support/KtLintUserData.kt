package org.jmailen.gradle.kotlinter.support

import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

fun userData(ktLintParams: KtLintParams): Map<String, String> {
    val userData = mutableMapOf(
        "indent_size" to ktLintParams.indentSize.toString(),
        "continuation_indent_size" to ktLintParams.continuationIndentSize.toString()
    )
    ktLintParams.disabledRules.ifNotEmpty {
        userData["disabled_rules"] = joinToString(",")
    }
    return userData
}
