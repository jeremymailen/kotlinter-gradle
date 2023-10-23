package org.jmailen.gradle.kotlinter.tasks.format

import com.pinterest.ktlint.rule.engine.api.Code
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.LintFailure
import org.jmailen.gradle.kotlinter.support.ktlintEngine
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import java.io.File

abstract class FormatWorkerAction : WorkAction<FormatWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(FormatTask::class.java))
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val output: File? = parameters.output.asFile.orNull

    override fun execute() {
        resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorConfigFiles,
            logger = logger,
        )
        val fixes = mutableListOf<String>()

        var hasError = false
        try {
            files.forEach { file ->

                val sourceText = file.readText()
                val relativePath = file.toRelativeString(projectDirectory)

                logger.log(LogLevel.DEBUG, "$name checking format: $relativePath")

                if (file.extension !in supportedExtensions) {
                    logger.log(LogLevel.DEBUG, "$name ignoring non Kotlin file: $relativePath")
                    return@forEach
                }

                val formattedText = ktlintEngine.format(Code.fromFile(file)) { error, corrected ->
                    val msg = when (corrected) {
                        true -> "${file.path}:${error.line}:${error.col}: Format fixed > [${error.ruleId.value}] ${error.detail}"
                        false -> "${file.path}:${error.line}:${error.col}: Format could not fix > [${error.ruleId.value}] ${error.detail}"
                    }
                    if (corrected) {
                        logger.warn(msg)
                    } else {
                        hasError = true
                        logger.error(msg) // TODO: is this needed?
                    }
                    fixes.add(msg)
                }
                if (!formattedText.contentEquals(sourceText)) {
                    logger.warn("${file.path}: Format fixed")
                    file.writeText(formattedText)
                }
            }
        } catch (t: Throwable) {
            throw KotlinterError("format worker execution error", t)
        }

        if (hasError) {
            throw LintFailure("kotlin source failed lint check")
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
