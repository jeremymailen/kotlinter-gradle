package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.KtLintRuleEngine
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import java.io.File

internal fun createKtlintEngine(ktLintParams: KtLintParams) = KtLintRuleEngine(
    ruleProviders = resolveRuleProviders(defaultRuleSetProviders),
    editorConfigOverride = editorConfigOverride(ktLintParams),
    editorConfigDefaults = editorConfigDefaults(ktLintParams),
)

internal fun KtLintRuleEngine.resetEditorconfigCacheIfNeeded(
    changedEditorconfigFiles: ConfigurableFileCollection,
    logger: Logger,
) {
    val changedFiles = changedEditorconfigFiles.files
    if (changedFiles.any()) {
        logger.info("Editorconfig changed, resetting KtLint caches")
        changedFiles.map(File::toPath).forEach(::reloadEditorConfigFile)
    }
}
