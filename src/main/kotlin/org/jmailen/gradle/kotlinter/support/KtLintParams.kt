package org.jmailen.gradle.kotlinter.support

import java.io.File
import java.io.Serializable

data class KtLintParams(
    val experimentalRules: Boolean,
    val disabledRules: List<String>,
    val baselineFile: File?
) : Serializable
