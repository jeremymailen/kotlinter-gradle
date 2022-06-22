package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.disabledRulesProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.emptyEditorConfigOverride

internal fun editorConfigOverride(ktLintParams: KtLintParams): EditorConfigOverride {
    val rules = ktLintParams.disabledRules

    return if (rules.isEmpty()) {
        emptyEditorConfigOverride
    } else {
        EditorConfigOverride.from(disabledRulesProperty to rules.joinToString(separator = ","))
    }
}
