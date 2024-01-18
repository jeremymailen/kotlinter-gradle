package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.support.LintFailure
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerAction
import java.io.File
import javax.inject.Inject

@CacheableTask
open class LintTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : ConfigurableKtLintTask(
    projectLayout = projectLayout,
    objectFactory = objectFactory,
) {

    @OutputFiles
    val reports: MapProperty<String, File> = objectFactory.mapProperty(default = emptyMap())

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree = super.getSource()

    @TaskAction
    fun run(inputChanges: InputChanges) {
        val result = with(workerExecutor.noIsolation()) {
            submit(LintWorkerAction::class.java) { p ->
                p.name.set(name)
                p.files.from(source)
                p.projectDirectory.set(projectLayout.projectDirectory.asFile)
                p.reporters.putAll(reports)
                p.changedEditorConfigFiles.from(getChangedEditorconfigFiles(inputChanges))
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
