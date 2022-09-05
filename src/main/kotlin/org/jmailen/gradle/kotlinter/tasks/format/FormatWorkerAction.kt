package org.jmailen.gradle.kotlinter.tasks.format

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.defaultRuleSetProviders
import org.jmailen.gradle.kotlinter.support.editorConfigOverride
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.support.resolveRuleProviders
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
        resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorConfigFiles,
            logger = logger,
        )

        val fixes = mutableListOf<String>()
        try {
            files.forEach { file ->
                val ruleSets = resolveRuleProviders(defaultRuleSetProviders, ktLintParams.experimentalRules)
                val sourceText = file.readText()
                val relativePath = file.toRelativeString(projectDirectory)

                logger.log(LogLevel.DEBUG, "$name checking format: $relativePath")

                when (file.extension) {
                    "kt" -> this::formatKt
                    "kts" -> this::formatKts
                    else -> {
                        logger.log(LogLevel.DEBUG, "$name ignoring non Kotlin file: $relativePath")
                        null
                    }
                }?.let { formatFunc ->
                    val formattedText = formatFunc.invoke(file, ruleSets) { error, corrected ->
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
            }
        } catch (t: Throwable) {
            throw KotlinterError("format worker execution error", t)
        }

        output?.writeText(
            when (fixes.isEmpty()) {
                true -> "ok"
                false -> fixes.joinToString("\n")
            },
        )
    }

    private fun formatKt(file: File, ruleSets: Set<RuleProvider>, onError: ErrorHandler) =
        format(file, ruleSets, onError, false)

    private fun formatKts(file: File, ruleSets: Set<RuleProvider>, onError: ErrorHandler) =
        format(file, ruleSets, onError, true)

    private fun format(file: File, ruleProviders: Set<RuleProvider>, onError: ErrorHandler, script: Boolean): String {
        return KtLint.format(
            KtLint.ExperimentalParams(
                fileName = file.path,
                text = file.readText(),
                ruleProviders = ruleProviders,
                script = script,
                editorConfigOverride = editorConfigOverride(ktLintParams),
                cb = { error, corrected ->
                    onError(error, corrected)
                },
            ),
        )
    }
}

typealias ErrorHandler = (error: LintError, corrected: Boolean) -> Unit
