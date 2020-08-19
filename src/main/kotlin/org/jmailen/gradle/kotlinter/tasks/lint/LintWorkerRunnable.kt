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
    private val ktLintParams = parameters.ktLintParams

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

                val ruleSets = resolveRuleSets(executionContext.ruleSetProviders, ktLintParams.experimentalRules)
                lintFunc?.invoke(file, ruleSets) { error ->
                    reporters.onEach { it.onLintError(relativePath, error, false) }

                    val errorStr = "${file.path}:${error.line}:${error.col}: Lint error > [${error.ruleId}] ${error.detail}"
                    logger.quiet(errorStr)
                }

                reporters.onEach { it.after(relativePath) }
            }
    }

    private fun lintKt(file: File, ruleSets: List<RuleSet>, onError: (error: LintError) -> Unit) =
        lint(file, ruleSets, onError, false)

    private fun lintKts(file: File, ruleSets: List<RuleSet>, onError: (error: LintError) -> Unit) =
        lint(file, ruleSets, onError, true)

    private fun lint(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler, script: Boolean) =
        KtLint.lint(
            KtLint.Params(
                fileName = file.path,
                text = file.readText(),
                ruleSets = ruleSets,
                script = script,
                userData = userData(ktLintParams),
                cb = { error, _ -> onError(error) }
            )
        )
}

typealias ErrorHandler = (error: LintError) -> Unit
