package org.jmailen.gradle.kotlinter.tasks.format

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import java.io.File
import javax.inject.Inject
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData

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
    private val ktLintParams: KtLintParams = parameters.ktLintParams

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
                    val ruleSets = resolveRuleSets(executionContext.ruleSetProviders, ktLintParams.experimentalRules)
                    val formattedText = formatFunc.invoke(file, ruleSets) { error, corrected ->
                        val msg = when (corrected) {
                            true -> "${file.path}:${error.line}:${error.col}: Format fixed > [${error.ruleId}] ${error.detail}"
                            false -> "${file.path}:${error.line}:${error.col}: Format could not fix > [${error.ruleId}] ${error.detail}"
                        }
                        logger.log(LogLevel.QUIET, msg)
                        executionContext.fixes.add(msg)
                    }
                    if (!formattedText.contentEquals(sourceText)) {
                        logger.log(LogLevel.QUIET, "${file.path}: Format fixed")
                        file.writeText(formattedText)
                    }
                }
            }
    }

    private fun formatKt(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler) =
        format(file, ruleSets, onError, false)

    private fun formatKts(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler) =
        format(file, ruleSets, onError, true)

    private fun format(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler, script: Boolean): String {
        return KtLint.format(
            KtLint.Params(
                fileName = file.path,
                text = file.readText(),
                ruleSets = ruleSets,
                script = script,
                userData = userData(ktLintParams),
                editorConfigPath = ktLintParams.editorConfigPath,
                cb = { error, corrected ->
                    onError(error, corrected)
                }
            )
        )
    }
}

typealias ErrorHandler = (error: LintError, corrected: Boolean) -> Unit
