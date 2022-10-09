package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.EditorConfigOverride
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import java.io.File

internal fun editorConfigOverride(ktLintParams: KtLintParams): EditorConfigOverride {
    val rules = ktLintParams.disabledRules

    return if (rules.isEmpty()) {
        EditorConfigOverride.emptyEditorConfigOverride
    } else {
        EditorConfigOverride.from(DefaultEditorConfigProperties.ktlintDisabledRulesProperty to rules.joinToString(separator = ","))
    }
}

internal fun resetEditorconfigCacheIfNeeded(
    changedEditorconfigFiles: ConfigurableFileCollection,
    logger: Logger,
) {
    val changedFiles = changedEditorconfigFiles.files
    if (changedFiles.any()) {
        logger.info("Editorconfig changed, resetting KtLint caches")
        changedFiles.map(File::toPath).forEach(KtLint::reloadEditorConfigFile)
    }
}
