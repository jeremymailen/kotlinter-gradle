package org.jmailen.gradle.kotlinter.tasks

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class FormatTask : SourceTask() {

    @Internal
    lateinit var ruleSets: List<RuleSet>

    @OutputFile
    lateinit var report: File

    @TaskAction
    fun run() {
        var fixes = ""

        getSource().forEach { file ->
            val relativePath = file.toRelativeString(project.projectDir)

            logger.log(LogLevel.DEBUG, "checking format: $relativePath")

            val formatFunc = when (file.extension) {
                "kt" -> KtLint::format
                "kts" -> KtLint::formatScript
                else -> {
                    logger.log(LogLevel.DEBUG, "ignoring non Kotlin file: $relativePath")
                    null
                }
            }

            var wasFormatted = false
            val formattedText = formatFunc?.invoke(file.readText(), ruleSets) { (line, col, detail), corrected ->
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
}
