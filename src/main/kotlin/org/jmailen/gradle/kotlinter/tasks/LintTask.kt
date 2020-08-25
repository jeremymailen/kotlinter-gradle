package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_IGNORE_FAILURES
import org.jmailen.gradle.kotlinter.KotlinterPlugin
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.LintFailure
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerAction
import java.io.File

@CacheableTask
open class LintTask : ConfigurableKtLintTask() {

    @OutputFiles
    val reports = mapProperty(default = emptyMap<String, File>())

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource() = super.getSource()

    @Input
    val ignoreFailures = property(default = DEFAULT_IGNORE_FAILURES)

    @TaskAction
    fun run() {
        KotlinterPlugin.workQueue.submit(LintWorkerAction::class.java) { p ->
            p.name.set(name)
            p.files.from(source)
            p.projectDirectory.set(project.projectDir)
            p.reporters.putAll(reports)
            p.ktLintParams.set(getKtLintParams())
        }
        val result = KotlinterPlugin.workQueue.runCatching { await() }

        result.exceptionOrNull()?.workErrorCauses<KotlinterError>()?.ifNotEmpty {
            forEach { logger.error(it.message, it.cause) }
            throw GradleException("error linting sources for $name")
        }

        val lintFailures = result.exceptionOrNull()?.workErrorCauses<LintFailure>() ?: emptyList()
        if (lintFailures.isNotEmpty() && !ignoreFailures.get()) {
            throw GradleException("$name sources failed lint check")
        }
    }
}
