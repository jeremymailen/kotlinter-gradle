package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.LintFailure
import org.jmailen.gradle.kotlinter.support.defaultRuleSetProviders
import org.jmailen.gradle.kotlinter.support.reporterFor
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

abstract class LintWorkerAction : WorkAction<LintWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(LintTask::class.java))
    private val reporters = parameters.reporters.get().map { (reporterName, outputPath) ->
        reporterFor(reporterName, outputPath)
    }
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val ktLintParams = parameters.ktLintParams.get()

    override fun execute() {
        var hasError = false

        try {
            reporters.onEach { it.beforeAll() }
            files.forEach { file ->
                val ruleSets = resolveRuleSets(defaultRuleSetProviders, ktLintParams.experimentalRules)
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
                lintFunc?.invoke(file, ruleSets) { error ->
                    hasError = true
                    reporters.onEach { it.onLintError(relativePath, error, false) }
                    logger.quiet("${file.path}:${error.line}:${error.col}: Lint error > [${error.ruleId}] ${error.detail}")
                }
                reporters.onEach { it.after(relativePath) }
            }
            reporters.onEach { it.afterAll() }
        } catch (t: Throwable) {
            throw KotlinterError("lint worker execution error", t)
        }

        if (hasError) {
            throw LintFailure("kotlin source failed lint check")
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
