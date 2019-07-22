package org.jmailen.gradle.kotlinter.support

fun userData(ktLintParams: KtLintParams) = mapOf(
    "indent_size" to ktLintParams.indentSize.toString(),
    "continuation_indent_size" to ktLintParams.continuationIndentSize.toString(),
    "disabled_rules" to ktLintParams.disabledRules.joinToString(",")
)
