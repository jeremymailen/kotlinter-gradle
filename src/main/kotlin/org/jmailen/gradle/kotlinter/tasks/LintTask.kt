package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
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

    @Input
    val ignoreFailures: Property<Boolean> = objectFactory.property(default = DEFAULT_IGNORE_FAILURES)

    @Classpath
    val ktlintClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @Classpath
    val ruleSetsClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @TaskAction
    fun run() {
        val workQueue = workerExecutor.processIsolation { spec ->
            spec.classpath.setFrom(ktlintClasspath, ruleSetsClasspath)
            spec.forkOptions { options ->
            }
        }

        workQueue.submit(LintWorkerAction::class.java) { p ->
            p.name.set(name)
            p.files.from(source)
            p.projectDirectory.set(projectLayout.projectDirectory.asFile)
            p.reporters.putAll(reports)
            p.ktLintParams.set(getKtLintParams())
        }

        try {
            workQueue.await()
        } catch (e: Throwable) {
            e.workErrorCauses<KotlinterError>().ifNotEmpty {
                forEach { logger.error(it.message, it.cause) }
                throw GradleException("error linting sources for $name")
            }
            val lintFailures = e.workErrorCauses<LintFailure>()
            if (lintFailures.isNotEmpty() && !ignoreFailures.get()) {
                throw GradleException("$name sources failed lint check")
            }
        }
    }
}
