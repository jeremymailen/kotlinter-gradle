package org.jmailen.gradle.kotlinter.tasks

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import java.io.File

open class FormatTask : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    var indentSize = 4

    @TaskAction
    fun run() {
        var fixes = ""
        val ruleSets = resolveRuleSets()

        getSource().forEach { file ->
            val relativePath = file.toRelativeString(project.projectDir)

            logger.log(LogLevel.DEBUG, "checking format: $relativePath")

            val formatFunc = when (file.extension) {
                "kt" -> this::formatKt
                "kts" -> this::formatKts
                else -> {
                    logger.log(LogLevel.DEBUG, "ignoring non Kotlin file: $relativePath")
                    null
                }
            }

            var wasFormatted = false
            val formattedText = formatFunc?.invoke(file, ruleSets) { line, col, detail, corrected ->
                val errorStr = "$relativePath:$line:$col: $detail"
                val msg = when (corrected) {
                    true -> "Format fixed > $errorStr"
                    false -> "Format could not fix > $errorStr"
                }
                logger.log(LogLevel.QUIET, msg)
                fixes += "$msg\n"
                if (corrected) {
                    wasFormatted = true
                }
            }
            if (wasFormatted && formattedText != null) {
                file.writeText(formattedText)
            }
        }

        if (fixes.isNotEmpty()) {
            report.writeText(fixes)
        } else {
            report.writeText("ok")
        }
    }

    private fun formatKt(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String, corrected: Boolean) -> Unit): String {
        return KtLint.format(file.readText(), ruleSets, mapOf("indent_size" to indentSize.toString())) { error, corrected ->
            onError(error.line, error.col, error.detail, corrected)
        }
    }

    private fun formatKts(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String, corrected: Boolean) -> Unit): String {
        return KtLint.formatScript(file.readText(), ruleSets, mapOf("indent_size" to indentSize.toString())) { error, corrected ->
            onError(error.line, error.col, error.detail, corrected)
        }
    }
}
