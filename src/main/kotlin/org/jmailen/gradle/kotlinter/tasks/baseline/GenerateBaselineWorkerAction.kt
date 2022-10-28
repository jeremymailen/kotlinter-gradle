package org.jmailen.gradle.kotlinter.tasks.baseline

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.editorConfigOverride
import org.jmailen.gradle.kotlinter.support.loadBaselineReporter
import org.jmailen.gradle.kotlinter.support.resetEditorconfigCacheIfNeeded
import org.jmailen.gradle.kotlinter.support.resolveRuleProviders
import org.jmailen.gradle.kotlinter.tasks.GenerateBaselineTask
import org.jmailen.gradle.kotlinter.tasks.lint.ErrorHandler
import java.io.File

internal abstract class GenerateBaselineWorkerAction : WorkAction<GenerateBaselineWorkerParameters> {

    private val logger: Logger = Logging.getLogger(GenerateBaselineTask::class.java)
    private val files: List<File> = parameters.files.toList()
    private val projectDirectory: File = parameters.projectDirectory.asFile.get()
    private val name: String = parameters.name.get()
    private val ktLintParams: KtLintParams = parameters.ktLintParams.get()

    override fun execute() {
        resetEditorconfigCacheIfNeeded(
            changedEditorconfigFiles = parameters.changedEditorconfigFiles,
            logger = logger,
        )
        val ruleSets = resolveRuleProviders(includeExperimentalRules = ktLintParams.experimentalRules)
        val baselineReporter = loadBaselineReporter(output = parameters.baselineFile.get().asFile)

        baselineReporter.beforeAll()

        files.forEach { file ->
            val relativePath = file.toRelativeString(projectDirectory)

            baselineReporter.before(relativePath)
            val lintFunc = when (file.extension) {
                "kt" -> ::lintKt
                "kts" -> ::lintKts
                else -> {
                    logger.debug("$name ignoring non Kotlin file: $relativePath")
                    null
                }
            }
            lintFunc?.invoke(file, ruleSets) { error ->
                baselineReporter.onLintError(relativePath, error, false)
            }
            baselineReporter.after(relativePath)
        }
        baselineReporter.afterAll()
    }

    private fun lintKt(file: File, ruleSets: Set<RuleProvider>, onError: (error: LintError) -> Unit) =
        lint(file, ruleSets, onError, false)

    private fun lintKts(file: File, ruleSets: Set<RuleProvider>, onError: (error: LintError) -> Unit) =
        lint(file, ruleSets, onError, true)

    private fun lint(file: File, ruleProviders: Set<RuleProvider>, onError: ErrorHandler, script: Boolean) =
        KtLint.lint(
            KtLint.ExperimentalParams(
                fileName = file.path,
                text = file.readText(),
                ruleProviders = ruleProviders,
                script = script,
                editorConfigOverride = editorConfigOverride(ktLintParams),
                cb = { error, _ -> onError(error) },
            ),
        )
}
