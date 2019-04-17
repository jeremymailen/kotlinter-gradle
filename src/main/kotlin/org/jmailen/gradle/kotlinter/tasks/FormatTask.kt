package org.jmailen.gradle.kotlinter.tasks

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.RuleSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData
import java.io.File

open class FormatTask : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    var indentSize = KotlinterExtension.DEFAULT_INDENT_SIZE

    @Input
    var continuationIndentSize = KotlinterExtension.DEFAULT_CONTINUATION_INDENT_SIZE

    @Input
    var experimentalRules = KotlinterExtension.DEFAULT_EXPERIMENTAL_RULES

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        var fixes = ""

        getSource().forEach { file ->
            val sourceText = file.readText()
            val relativePath = file.toRelativeString(project.projectDir)

            logger.log(LogLevel.DEBUG, "checking format: $relativePath")

            when (file.extension) {
                "kt" -> this::formatKt
                "kts" -> this::formatKts
                else -> {
                    logger.log(LogLevel.DEBUG, "ignoring non Kotlin file: $relativePath")
                    null
                }
            }?.let { formatFunc ->
                val formattedText = formatFunc.invoke(file, resolveRuleSets(experimentalRules)) { line, col, detail, corrected ->
                    val errorStr = "$relativePath:$line:$col: $detail"
                    val msg = when (corrected) {
                        true -> "Format fixed > $errorStr"
                        false -> "Format could not fix > $errorStr"
                    }
                    logger.log(LogLevel.QUIET, msg)
                    fixes += "$msg\n"
                }
                if (!formattedText.contentEquals(sourceText)) {
                    logger.log(LogLevel.QUIET, "Format fixed > $relativePath")
                    file.writeText(formattedText)
                }
            }
        }

        if (fixes.isNotEmpty()) {
            report.writeText(fixes)
        } else {
            report.writeText("ok")
        }
    }

    private fun formatKt(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String, corrected: Boolean) -> Unit): String {
        return KtLint.format(
                file.readText(),
                ruleSets,
                userData(
                    indentSize = indentSize,
                    continuationIndentSize = continuationIndentSize,
                    filePath = file.path
                )) { error, corrected ->
        onError(error.line, error.col, error.detail, corrected)
        }
    }

    private fun formatKts(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String, corrected: Boolean) -> Unit): String {
        return KtLint.formatScript(
                file.readText(),
                ruleSets,
                userData(
                    indentSize = indentSize,
                    continuationIndentSize = continuationIndentSize,
                    filePath = file.path
                )) { error, corrected ->
        onError(error.line, error.col, error.detail, corrected)
        }
    }
}
