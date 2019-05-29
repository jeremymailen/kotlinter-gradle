package org.jmailen.gradle.kotlinter.tasks.format

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.RuleSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData
import java.io.File
import javax.inject.Inject

/**
 * Runnable used in the Gradle Worker API to run format on a batch of files.
 */
class FormatWorkerRunnable @Inject constructor(
    parameters: FormatWorkerParameters
) : Runnable {

    private val executionContext = ExecutionContextRepository.formatInstance.get(parameters.executionContextRepositoryId)
    private val logger: Logger = executionContext.logger
    private val files: List<File> = parameters.files
    private val projectDirectory: File = parameters.projectDirectory
    private val experimentalRules: Boolean = parameters.experimentalRules
    private val allowWildcardImports: Boolean = parameters.allowWildcardImports
    private val indentSize: Int = parameters.indentSize
    private val continuationIndentSize: Int = parameters.continuationIndentSize

    override fun run() {
        files
            .forEach { file ->
                val sourceText = file.readText()
                val relativePath = file.toRelativeString(projectDirectory)

                logger.log(LogLevel.DEBUG, "checking format: $relativePath")

                when (file.extension) {
                    "kt" -> this::formatKt
                    "kts" -> this::formatKts
                    else -> {
                        logger.log(LogLevel.DEBUG, "ignoring non Kotlin file: $relativePath")
                        null
                    }
                }?.let { formatFunc ->
                    val ruleSets = resolveRuleSets(executionContext.ruleSetProviders, experimentalRules, allowWildcardImports)
                    val formattedText = formatFunc.invoke(file, ruleSets) { line, col, detail, corrected ->
                        val errorStr = "$relativePath:$line:$col: $detail"
                        val msg = when (corrected) {
                            true -> "Format fixed > $errorStr"
                            false -> "Format could not fix > $errorStr"
                        }
                        logger.log(LogLevel.QUIET, msg)
                        executionContext.fixes.add(msg)
                    }
                    if (!formattedText.contentEquals(sourceText)) {
                        logger.log(LogLevel.QUIET, "Format fixed > $relativePath")
                        file.writeText(formattedText)
                    }
                }
        }
    }

    private fun formatKt(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String, corrected: Boolean) -> Unit): String {
        return KtLint.format(
            file.readText(),
            ruleSets,
            userData(
                indentSize = indentSize,
                continuationIndentSize = continuationIndentSize,
                filePath = file.path
            )) { error, corrected ->
            onError(error.line, error.col, error.detail, corrected)
        }
    }

    private fun formatKts(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String, corrected: Boolean) -> Unit): String {
        return KtLint.formatScript(
            file.readText(),
            ruleSets,
            userData(
                indentSize = indentSize,
                continuationIndentSize = continuationIndentSize,
                filePath = file.path
            )) { error, corrected ->
            onError(error.line, error.col, error.detail, corrected)
        }
    }
}
