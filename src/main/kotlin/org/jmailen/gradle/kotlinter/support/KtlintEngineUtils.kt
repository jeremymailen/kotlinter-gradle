package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import java.io.File

internal val ktlintEngine = KtLintRuleEngine(
    ruleProviders = resolveRuleProviders(defaultRuleSetProviders),
)

internal fun resetEditorconfigCacheIfNeeded(changedEditorconfigFiles: ConfigurableFileCollection, logger: Logger) {
    val changedFiles = changedEditorconfigFiles.files
    if (changedFiles.any()) {
        logger.info("Editorconfig changed, resetting KtLint caches")
        changedFiles.map(File::toPath).forEach {
            ktlintEngine.reloadEditorConfigFile(it)
        }
    }
}
