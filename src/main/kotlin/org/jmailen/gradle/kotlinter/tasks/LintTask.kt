package org.jmailen.gradle.kotlinter.tasks

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import java.io.File

@ParallelizableTask
open class LintTask : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    var ignoreFailures = false

    @Input
    var indentSize = 4

    @TaskAction
    fun run() {
        var errors = ""
        val ruleSets = resolveRuleSets()

        getSource().forEach { file ->
            val relativePath = file.toRelativeString(project.projectDir)

            logger.log(LogLevel.DEBUG, "$name linting: $relativePath")

            val lintFunc = when (file.extension) {
                "kt" -> this::lintKt
                "kts" -> this::lintKts
                else -> {
                    logger.log(LogLevel.DEBUG, "$name ignoring non Kotlin file: $relativePath")
                    null
                }
            }

            lintFunc?.invoke(file, ruleSets) { line, col, detail ->
                val errorStr = "$relativePath:$line:$col: $detail"
                logger.log(LogLevel.QUIET, "Lint error > $errorStr")
                errors += "$errorStr\n"
            }
        }

        if (errors.isNotEmpty()) {
            report.writeText(errors)
            if (!ignoreFailures) {
                throw GradleException("Kotlin source failed lint check.")
            }
        } else {
            report.writeText("ok")
        }
    }

    private fun lintKt(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String) -> Unit) {
        KtLint.lint(file.readText(), ruleSets, mapOf("indent_size" to indentSize.toString())) { error ->
            onError(error.line, error.col, error.detail)
        }
    }

    private fun lintKts(file: File, ruleSets: List<RuleSet>, onError: (line: Int, col: Int, detail: String) -> Unit) {
        KtLint.lintScript(file.readText(), ruleSets) { error ->
            onError(error.line, error.col, error.detail)
        }
    }
}
