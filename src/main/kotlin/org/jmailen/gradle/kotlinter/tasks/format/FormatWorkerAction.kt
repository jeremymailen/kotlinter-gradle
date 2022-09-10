package org.jmailen.gradle.kotlinter.tasks.format

import com.pinterest.ktlint.core.Code
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.createKtlintEngine
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import java.io.File

abstract class FormatWorkerAction : WorkAction<FormatWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(FormatTask::class.java))
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val ktLintParams: KtLintParams = parameters.ktLintParams.get()
    private val output: File? = parameters.output.asFile.orNull

    override fun execute() {
        val ktLintEngine = createKtlintEngine(ktLintParams = ktLintParams)
        ktLintEngine.resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorConfigFiles,
            logger = logger,
        )

        val fixes = mutableListOf<String>()
        try {
            files.forEach { file ->
                val sourceText = file.readText()
                val relativePath = file.toRelativeString(projectDirectory)

                logger.log(LogLevel.DEBUG, "$name checking format: $relativePath")

                if (file.extension !in supportedExtensions) {
                    logger.log(LogLevel.DEBUG, "$name ignoring non Kotlin file: $relativePath")
                    return@forEach
                }

                val formattedText = ktLintEngine.format(Code.CodeFile(file)) { error, corrected ->
                    val msg = when (corrected) {
                        true -> "${file.path}:${error.line}:${error.col}: Format fixed > [${error.ruleId}] ${error.detail}"
                        false -> "${file.path}:${error.line}:${error.col}: Format could not fix > [${error.ruleId}] ${error.detail}"
                    }
                    logger.log(LogLevel.QUIET, msg)
                    fixes.add(msg)
                }
                if (!formattedText.contentEquals(sourceText)) {
                    logger.log(LogLevel.QUIET, "${file.path}: Format fixed")
                    file.writeText(formattedText)
                }
            }
        } catch (t: Throwable) {
            throw KotlinterError.WorkerError("format worker execution error", t)
        }

        output?.writeText(
            when (fixes.isEmpty()) {
                true -> "ok"
                false -> fixes.joinToString("\n")
            },
        )
    }
}

private val supportedExtensions = setOf("kt", "kts")
