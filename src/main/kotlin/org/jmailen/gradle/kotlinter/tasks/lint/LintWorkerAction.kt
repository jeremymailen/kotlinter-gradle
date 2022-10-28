package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.containsLintError
import com.pinterest.ktlint.core.api.loadBaseline
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.editorConfigOverride
import org.jmailen.gradle.kotlinter.support.reporterPathFor
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.support.resolveReporters
import org.jmailen.gradle.kotlinter.support.resolveRuleProviders
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

abstract class LintWorkerAction : WorkAction<LintWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(LintTask::class.java))
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val ktLintParams: KtLintParams = parameters.ktLintParams.get()

    override fun execute() {
        resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorconfigFiles,
            logger = logger,
        )
        val ruleSets = resolveRuleProviders(includeExperimentalRules = ktLintParams.experimentalRules)
        logger.info("$name - resolved ${ruleSets.size} RuleSetProviders")
        logger.info("$name - executing against ${files.size} file(s)")
        if (logger.isDebugEnabled) {
            logger.debug("Resolved RuleSetProviders = ${ruleSets.joinToString { it.createNewRuleInstance().id }}")
        }

        val reporters = resolveReporters(enabled = parameters.reporters.get())

        val baselineRules = parameters.baselineFile.orNull?.asFile?.absolutePath
            ?.let(::loadBaseline)
            ?.lintErrorsPerFile

        var hasError = false

        try {
            reporters.onEach { (_, reporter) -> reporter.beforeAll() }
            files.forEach { file ->
                val relativePath = file.toRelativeString(projectDirectory)
                val errorsInTheFile = baselineRules?.get(relativePath).orEmpty()
                reporters.onEach { (_, reporter) -> reporter.before(relativePath) }
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

                    if (!errorsInTheFile.containsLintError(error)) {
                        reporters.onEach { (type, reporter) ->
                            // some reporters want relative paths, some want absolute
                            val filePath = reporterPathFor(
                                reporterType = type,
                                output = file,
                                relativeRoot = projectDirectory,
                            )
                            reporter.onLintError(filePath, error, false)
                        }
                        logger.quiet("${file.path}:${error.line}:${error.col}: Lint error > [${error.ruleId}] ${error.detail}")
                    }
                }
                reporters.onEach { (_, reporter) -> reporter.after(relativePath) }
            }
            reporters.onEach { (_, reporter) -> reporter.afterAll() }
        } catch (t: Throwable) {
            throw KotlinterError.WorkerError("lint worker execution error", t)
        }

        if (hasError) {
            throw KotlinterError.LintingError("$name source failed lint check")
        }
    }

    private fun lintKt(file: File, ruleSets: Set<RuleProvider>, onError: (error: LintError) -> Unit) =
        lint(file, ruleSets, onError, false)

    private fun lintKts(file: File, ruleSets: Set<RuleProvider>, onError: (error: LintError) -> Unit) =
        lint(file, ruleSets, onError, true)

    private fun lint(file: File, ruleProviders: Set<RuleProvider>, onError: ErrorHandler, script: Boolean) =
        KtLint.lint(
            KtLint.ExperimentalParams(
                fileName = file.path,
                text = file.readText(),
                ruleProviders = ruleProviders,
                script = script,
                editorConfigOverride = editorConfigOverride(ktLintParams),
                cb = { error, _ -> onError(error) },
            ),
        )
}

typealias ErrorHandler = (error: LintError) -> Unit
