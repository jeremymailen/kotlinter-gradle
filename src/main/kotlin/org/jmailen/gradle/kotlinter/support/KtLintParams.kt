package org.jmailen.gradle.kotlinter.support

import java.io.Serializable
import org.jmailen.gradle.kotlinter.KotlinterExtension

data class KtLintParams(
    var indentSize: Int = KotlinterExtension.DEFAULT_INDENT_SIZE,
    var continuationIndentSize: Int = KotlinterExtension.DEFAULT_CONTINUATION_INDENT_SIZE,
    var experimentalRules: Boolean = KotlinterExtension.DEFAULT_EXPERIMENTAL_RULES,
    var disabledRules: Array<String> = KotlinterExtension.DEFAULT_DISABLED_RULES,
    var editorConfigPath: String? = null
) : Serializable
