package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.Code
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.createKtlintEngine
import org.jmailen.gradle.kotlinter.support.reporterPathFor
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.support.resolveReporters
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

abstract class LintWorkerAction : WorkAction<LintWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(LintTask::class.java))
    private val files: Iterable<File> = parameters.files
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val ktLintParams: KtLintParams = parameters.ktLintParams.get()

    override fun execute() {
        val ktLintEngine = createKtlintEngine(ktLintParams = ktLintParams)
        ktLintEngine.resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorConfigFiles,
            logger = logger,
        )
        logger.info("Resolved ${ktLintEngine.ruleProviders.size} RuleProviders")
        if (logger.isDebugEnabled) {
            logger.debug("Resolved RuleSetProviders = ${ktLintEngine.ruleProviders.joinToString { it.createNewRuleInstance().id }}")
        }

        val reporters = resolveReporters(enabled = parameters.reporters.get())

        var hasError = false

        try {
            reporters.onEach { (_, reporter) -> reporter.beforeAll() }
            files.sorted().forEach { file ->
                val relativePath = file.toRelativeString(projectDirectory)
                reporters.onEach { (_, reporter) -> reporter.before(relativePath) }
                logger.debug("$name linting: $relativePath")

                if (file.extension !in supportedExtensions) {
                    logger.debug("$name ignoring non Kotlin file: $relativePath")
                    return@forEach
                }

                ktLintEngine.lint(Code.CodeFile(file)) { error ->
                    hasError = true
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
}

private val supportedExtensions = setOf("kt", "kts")
