package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.LintError
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.LintFailure
import org.jmailen.gradle.kotlinter.support.ktlintEngine
import org.jmailen.gradle.kotlinter.support.reporterFor
import org.jmailen.gradle.kotlinter.support.reporterPathFor
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

abstract class LintWorkerAction : WorkAction<LintWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(LintTask::class.java))
    private val reporters: List<ReporterV2> = parameters.reporters.get().map { (reporterName, outputPath) ->
        reporterFor(reporterName, outputPath)
    }
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()

    override fun execute() {
        resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorConfigFiles,
            logger = logger,
        )
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

                ktlintEngine.lint(Code.fromFile(file)) { error: LintError ->
                    hasError = true
                    reporters.onEach { reporter ->
                        // some reporters want relative paths, some want absolute
                        val filePath = reporterPathFor(reporter, file, projectDirectory)
                        reporter.onLintError(filePath, error.toCliError())
                    }
                    logger.error("${file.path}:${error.line}:${error.col}: Lint error > [${error.ruleId.value}] ${error.detail}")
                }
                reporters.onEach { it.after(relativePath) }
            }
            reporters.onEach { it.afterAll() }
        } catch (t: Throwable) {
            throw KotlinterError("lint worker execution error", t)
        }

        if (hasError) {
            throw LintFailure("kotlin source $name failed lint check")
        }
    }
}

private val supportedExtensions = setOf("kt", "kts")

internal fun LintError.toCliError() = KtlintCliError(
    line,
    col,
    ruleId.value,
    detail,
    if (canBeAutoCorrected) {
        KtlintCliError.Status.LINT_CAN_BE_AUTOCORRECTED
    } else {
        KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED
    },
)
