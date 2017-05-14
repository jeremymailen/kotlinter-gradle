package org.jmailen.gradle.kotlinter.tasks

import com.github.shyiko.ktlint.core.KtLint
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import java.io.File

@ParallelizableTask
open class LintTask : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    fun ignoreFailures() =
            project.extensions.getByType(KotlinterExtension::class.java).ignoreFailures

    @TaskAction
    fun run() {
        var errors = ""
        val ruleSets = resolveRuleSets()

        getSource().forEach { file ->
            val relativePath = file.toRelativeString(project.projectDir)

            logger.log(LogLevel.DEBUG, "linting: $relativePath")

            val lintFunc = when (file.extension) {
                "kt" -> KtLint::lint
                "kts" -> KtLint::lintScript
                else -> {
                    logger.log(LogLevel.DEBUG, "ignoring non Kotlin file: $relativePath")
                    null
                }
            }

            lintFunc?.invoke(file.readText(), ruleSets) { (line, col, detail) ->
                val errorStr = "$relativePath:$line:$col: $detail"
                logger.log(LogLevel.QUIET, "Lint error > $errorStr")
                errors += "$errorStr\n"
            }
        }

        if (errors.isNotEmpty()) {
            report.writeText(errors)
            if (!ignoreFailures()) {
                throw GradleException("Kotlin source failed lint check.")
            }
        } else {
            report.writeText("ok")
        }
    }
}
