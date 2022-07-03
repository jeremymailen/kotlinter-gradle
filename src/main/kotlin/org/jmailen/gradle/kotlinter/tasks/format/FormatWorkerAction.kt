package org.jmailen.gradle.kotlinter.tasks.format

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.defaultRuleSetProviders
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import java.io.File

abstract class FormatWorkerAction : WorkAction<FormatWorkerParameters> {
    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(FormatTask::class.java))
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val ktLintParams: KtLintParams = parameters.ktLintParams.get()
    private val output: File? = parameters.output.asFile.orNull

    internal open fun formatAngGetResult(): FormatWorkerActionResult {
        val fixes = mutableListOf<String>()
        val formatted = mutableListOf<File>()

        try {
            files.forEach { file ->
                val ruleSets = resolveRuleSets(defaultRuleSetProviders, ktLintParams.experimentalRules)
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
                        logger.error("${file.absolutePath} result =$corrected")
                        logger.log(LogLevel.QUIET, msg)
                        fixes.add(msg)
                    }
                    if (!formattedText.contentEquals(sourceText)) {
                        logger.log(LogLevel.QUIET, "${file.path}: Format fixed")
                        file.writeText(formattedText)
                        formatted.add(file)
                    }
                }
            }

            return FormatWorkerActionResult(fixes, formatted)
        } catch (t: Throwable) {
            throw KotlinterError("format worker execution error", t)
        }
    }

    override fun execute() {
        val result = formatAngGetResult()

        output?.writeText(
            when (result.msg.isEmpty()) {
                true -> "ok"
                false -> result.msg.joinToString("\n")
            }
        )
    }

    private fun formatKt(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler) =
        format(file, ruleSets, onError, false)

    private fun formatKts(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler) =
        format(file, ruleSets, onError, true)

    private fun format(file: File, ruleSets: List<RuleSet>, onError: ErrorHandler, script: Boolean): String {
        return KtLint.format(
            KtLint.Params(
                fileName = file.path,
                text = file.readText(),
                ruleSets = ruleSets,
                script = script,
                userData = userData(ktLintParams),
                cb = { error, corrected ->
                    onError(error, corrected)
                }
            )
        )
    }
}

typealias ErrorHandler = (error: LintError, corrected: Boolean) -> Unit
