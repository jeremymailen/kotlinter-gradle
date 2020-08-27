package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerAction
import javax.inject.Inject

open class FormatTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : ConfigurableKtLintTask() {

    @OutputFile
    @Optional
    val report = project.objects.fileProperty()

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val result = with(workerExecutor.noIsolation()) {
            submit(FormatWorkerAction::class.java) { p ->
                p.name.set(name)
                p.files.from(source)
                p.projectDirectory.set(project.projectDir)
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
}
