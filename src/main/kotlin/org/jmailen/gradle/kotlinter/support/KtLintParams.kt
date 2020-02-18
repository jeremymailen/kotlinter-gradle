package org.jmailen.gradle.kotlinter.support

import java.io.Serializable

data class KtLintParams(
    val indentSize: Int?,
    val experimentalRules: Boolean,
    val disabledRules: List<String>,
    val editorConfigPath: String?
) : Serializable
