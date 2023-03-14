package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.KtLintRuleEngine
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import java.io.File

// You must create a new KtLintRuleEngine per file with fresh rule providers.
// Otherwise, KtLint errors on resolving rule enable/disable statements in .editorconfig
internal fun createKtlintEngine() = KtLintRuleEngine(
    ruleProviders = resolveRuleProviders(defaultRuleSetProviders),
)

internal fun resetEditorconfigCacheIfNeeded(
    changedEditorconfigFiles: ConfigurableFileCollection,
    logger: Logger,
) {
    val engine = createKtlintEngine()
    val changedFiles = changedEditorconfigFiles.files
    if (changedFiles.any()) {
        logger.info("Editorconfig changed, resetting KtLint caches")
        changedFiles.map(File::toPath).forEach {
            engine.reloadEditorConfigFile(it)
        }
    }
}
