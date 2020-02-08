package org.jmailen.gradle.kotlinter.tasks

import javax.inject.Inject
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.defaultRuleSetProviders
import org.jmailen.gradle.kotlinter.tasks.format.FormatExecutionContext
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerConfigurationAction
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerParameters
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerRunnable

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
        val executionContextRepository = ExecutionContextRepository.formatInstance
        val executionContext = FormatExecutionContext(defaultRuleSetProviders, logger)
        val executionContextRepositoryId = executionContextRepository.register(executionContext)

        getChunkedSource()
            .map { files ->
                FormatWorkerParameters(
                    files = files,
                    projectDirectory = project.projectDir,
                    executionContextRepositoryId = executionContextRepositoryId,
                    ktLintParams = getKtLintParams()
                )
            }
            .forEach { parameters ->
                workerExecutor.submit(FormatWorkerRunnable::class.java, FormatWorkerConfigurationAction(parameters))
            }

        workerExecutor.await()
        executionContextRepository.unregister(executionContextRepositoryId)

        val reportFile = report.asFile.orNull
        if (executionContext.fixes.isNotEmpty()) {
            reportFile?.writeText(executionContext.fixes.joinToString("\n"))
        } else {
            reportFile?.writeText("ok")
        }
    }
}
