package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
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

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    open val baselineFile = objectFactory.fileProperty().apply {
        set(projectLayout.projectDirectory.dir("config").file("ktlint-baseline.xml").takeIf { it.asFile.exists() })
    }

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run(inputChanges: InputChanges) {
        val workQueue = workerExecutor.processIsolation { spec ->
            spec.classpath.setFrom(ktlintClasspath, ruleSetsClasspath)
            spec.forkOptions { options ->
                options.maxHeapSize = workerMaxHeapSize.get()
            }
        }

        workQueue.submit(FormatWorkerAction::class.java) { p ->
            p.name.set(name)
            p.files.from(source)
            p.projectDirectory.set(projectLayout.projectDirectory.asFile)
            p.ktLintParams.set(getKtLintParams())
            p.output.set(report)
            p.changedEditorConfigFiles.from(getChangedEditorconfigFiles(inputChanges))
            p.baselineFile.set(baselineFile)
        }

        workQueue.await()
    }
}
