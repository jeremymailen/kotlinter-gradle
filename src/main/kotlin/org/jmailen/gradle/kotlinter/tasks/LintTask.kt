package org.jmailen.gradle.kotlinter.tasks

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.support.reporterFor
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.support.userData
import java.io.File

@ParallelizableTask
open class LintTask : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    var reporter = KotlinterExtension.DEFAULT_REPORTER

    @Input
    var ignoreFailures = KotlinterExtension.DEFAULT_IGNORE_FAILURES

    @Input
    var indentSize = KotlinterExtension.DEFAULT_INDENT_SIZE

    @Internal
    var sourceSetId = ""

    @TaskAction
    fun run() {
        var hasErrors = false
        var fileReporter = reporterFor(reporter, report)
        val ruleSets = resolveRuleSets()

        fileReporter.beforeAll()

        getSource().forEach { file ->
            val relativePath = file.toRelativeString(project.projectDir)
            fileReporter.before(relativePath)
            logger.log(LogLevel.DEBUG, "$name linting: $relativePath")

            val lintFunc = when (file.extension) {
                "kt" -> this::lintKt
                "kts" -> this::lintKts
                else -> {
                    logger.log(LogLevel.DEBUG, "$name ignoring non Kotlin file: $relativePath")
                    null
                }
            }

            lintFunc?.invoke(file, ruleSets) { error ->
                fileReporter.onLintError(relativePath, error, false)

                val errorStr = "$relativePath:${error.line}:${error.col}: ${error.detail}"
                logger.log(LogLevel.QUIET, "Lint error > $errorStr")

                hasErrors = true
            }

            fileReporter.after(relativePath)
        }

        fileReporter.afterAll()
        if (hasErrors && !ignoreFailures) {
            throw GradleException("Kotlin source failed lint check.")
        }
    }

    private fun lintKt(file: File, ruleSets: List<RuleSet>, onError: (error: LintError) -> Unit) =
            KtLint.lint(file.readText(), ruleSets, userData(indentSize = indentSize), onError)

    private fun lintKts(file: File, ruleSets: List<RuleSet>, onError: (error: LintError) -> Unit) =
            KtLint.lintScript(file.readText(), ruleSets, userData(indentSize = indentSize), onError)
}
