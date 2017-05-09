package org.jmailen.gradle.kotlinter.tasks

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

open class LintTask : SourceTask() {

    @Input
    lateinit var ruleSets: List<RuleSet>

    @TaskAction
    fun run() {
        this.getSource().forEach { file ->
            val relativePath = file.toRelativeString(project.projectDir)

            logger.log(LogLevel.DEBUG, "linting: $relativePath")

            val lintFunc = when (file.name.endsWith(".kt")) {
                true -> KtLint::lint
                false -> KtLint::lintScript
            }

            var hasLintErrors = false
            lintFunc(file.readText(), ruleSets) { ruleError ->
                logger.log(LogLevel.ERROR,
                        "Lint error > $relativePath:${ruleError.line}:${ruleError.col}: ${ruleError.detail}")
                hasLintErrors = true
            }
            if (hasLintErrors) {
                throw GradleException("Kotlin source failed lint check.")
            }
        }
    }
}
