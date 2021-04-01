package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_IGNORE_FAILURES
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.LintFailure
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerAction
import java.io.File
import javax.inject.Inject

@CacheableTask
open class LintTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : ConfigurableKtLintTask() {

    @OutputFiles
    val reports: MapProperty<String, File> = mapProperty(default = emptyMap<String, File>())

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree = super.getSource()

    @Input
    val ignoreFailures: Property<Boolean> = property(default = DEFAULT_IGNORE_FAILURES)

    @TaskAction
    fun run() {
        val result = with(workerExecutor.noIsolation()) {
            submit(LintWorkerAction::class.java) { p ->
                p.name.set(name)
                p.files.from(source)
                p.projectDirectory.set(project.projectDir)
                p.reporters.putAll(reports)
                p.ktLintParams.set(getKtLintParams())
            }
            runCatching { await() }
        }

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
