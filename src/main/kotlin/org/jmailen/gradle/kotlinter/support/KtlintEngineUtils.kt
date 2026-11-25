package org.jmailen.gradle.kotlinter.support

import io.github.ktlint.core.rule.engine.api.KtLintRuleEngine
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import java.io.File

internal val ktlintEngine = try {
    KtLintRuleEngine(ruleProviders = resolveRuleProviders(defaultRuleSetProviders, legacyRuleSetProviders))
} catch (error: LinkageError) {
    if (legacyRuleSetProviders.isEmpty()) throw error
    throw IllegalStateException(LEGACY_ERROR, error)
}

internal fun resetEditorconfigCacheIfNeeded(changedEditorconfigFiles: ConfigurableFileCollection, logger: Logger) {
    val changedFiles = changedEditorconfigFiles.files
    if (changedFiles.any()) {
        logger.info("Editorconfig changed, resetting KtLint caches")
        changedFiles.map(File::toPath).forEach {
            ktlintEngine.reloadEditorConfigFile(it)
        }
    }
}
