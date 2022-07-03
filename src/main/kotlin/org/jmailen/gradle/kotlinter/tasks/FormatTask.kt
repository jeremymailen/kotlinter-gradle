package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerAction
import javax.inject.Inject

open class FormatTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : ConfigurableKtLintTask(
    projectLayout = projectLayout,
    objectFactory = objectFactory,
) {

    @OutputFile
    @Optional
    val report: RegularFileProperty = objectFactory.fileProperty()

    init {
        outputs.upToDateWhen { false }
    }

    internal open fun executeFormat(
        filesToFormat: FileCollection,
        action: Class<out FormatWorkerAction>,
    ) {
        val result = with(workerExecutor.noIsolation()) {
            submit(action) { p ->
                p.name.set(name)
                p.files.from(filesToFormat)
                p.projectDirectory.set(projectLayout.projectDirectory.asFile)
                p.gitProjectDirectory.set(project.rootDir)
                p.ktLintParams.set(getKtLintParams())
                p.output.set(report)
            }
            runCatching { await() }
        }

        result.exceptionOrNull()?.workErrorCauses<KotlinterError>()?.ifNotEmpty {
            forEach { logger.error(it.message, it.cause) }
            throw GradleException("error formatting sources for $name")
        }
    }

    @TaskAction
    fun run() {
        executeFormat(
            filesToFormat = source,
            action = FormatWorkerAction::class.java
        )
    }
}
