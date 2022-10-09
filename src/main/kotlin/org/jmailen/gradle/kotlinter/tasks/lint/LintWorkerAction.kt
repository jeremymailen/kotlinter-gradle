package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.Code
import com.pinterest.ktlint.core.Reporter
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.createKtlintEngine
import org.jmailen.gradle.kotlinter.support.reporterFor
import org.jmailen.gradle.kotlinter.support.reporterPathFor
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

abstract class LintWorkerAction : WorkAction<LintWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(LintTask::class.java))
    private val reporters: List<Reporter> = parameters.reporters.get().map { (reporterName, outputPath) ->
        reporterFor(reporterName, outputPath)
    }
    private val files: List<File> = parameters.files.toList()
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

        var hasError = false

        try {
            reporters.onEach { it.beforeAll() }
            files.forEach { file ->
                val relativePath = file.toRelativeString(projectDirectory)
                reporters.onEach { it.before(relativePath) }
                logger.debug("$name linting: $relativePath")

                if (file.extension !in supportedExtensions) {
                    logger.debug("$name ignoring non Kotlin file: $relativePath")
                    return@forEach
                }

                ktLintEngine.lint(Code.CodeFile(file)) { error ->
                    hasError = true
                    reporters.onEach { reporter ->
                        // some reporters want relative paths, some want absolute
                        val filePath = reporterPathFor(reporter, file, projectDirectory)
                        reporter.onLintError(filePath, error, false)
                    }
                    logger.quiet("${file.path}:${error.line}:${error.col}: Lint error > [${error.ruleId}] ${error.detail}")
                }
                reporters.onEach { it.after(relativePath) }
            }
            reporters.onEach { it.afterAll() }
        } catch (t: Throwable) {
            throw KotlinterError.WorkerError("lint worker execution error", t)
        }

        if (hasError) {
            throw KotlinterError.LintingError("$name source failed lint check")
        }
    }
}

private val supportedExtensions = setOf("kt", "kts")
