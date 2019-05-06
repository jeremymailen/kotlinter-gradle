package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.RuleSet
import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData
import java.io.File
import javax.inject.Inject

/**
 * Runnable used in the Gradle Worker API to run lint on a batch of files.
 */
class LintWorkerRunnable @Inject constructor(
    parameters: LintWorkerParameters
) : Runnable {

    private val executionContext = ExecutionContextRepository.lintInstance.get(parameters.executionContextRepositoryId)
    private val reporters: List<Reporter> = executionContext.reporters
    private val logger: Logger = executionContext.logger
    private val files: List<File> = parameters.files
    private val projectDirectory: File = parameters.projectDirectory
    private val name: String = parameters.name
    private val experimentalRules: Boolean = parameters.experimentalRules
    private val indentSize: Int = parameters.indentSize
    private val continuationIndentSize: Int = parameters.continuationIndentSize

    override fun run() {
        files
            .forEach { file ->
                val relativePath = file.toRelativeString(projectDirectory)
                reporters.onEach { it.before(relativePath) }
                logger.debug("$name linting: $relativePath")

                val lintFunc = when (file.extension) {
                    "kt" -> ::lintKt
                    "kts" -> ::lintKts
                    else -> {
                        logger.debug("$name ignoring non Kotlin file: $relativePath")
                        null
                    }
                }

                lintFunc?.invoke(file, resolveRuleSets(experimentalRules)) { error ->
                    reporters.onEach { it.onLintError(relativePath, error, false) }

                    val errorStr = "$relativePath:${error.line}:${error.col}: ${error.detail}"
                    logger.quiet("Lint error > $errorStr")
                }

                reporters.onEach { it.after(relativePath) }
            }
    }

    private fun lintKt(file: File, ruleSets: List<RuleSet>, onError: (error: LintError) -> Unit) =
        KtLint.lint(
            file.readText(),
            ruleSets,
            userData(
                indentSize = indentSize,
                continuationIndentSize = continuationIndentSize,
                filePath = file.path
            ), onError)

    private fun lintKts(file: File, ruleSets: List<RuleSet>, onError: (error: LintError) -> Unit) =
        KtLint.lintScript(
            file.readText(),
            ruleSets,
            userData(
                indentSize = indentSize,
                continuationIndentSize = continuationIndentSize,
                filePath = file.path
            ), onError)
}