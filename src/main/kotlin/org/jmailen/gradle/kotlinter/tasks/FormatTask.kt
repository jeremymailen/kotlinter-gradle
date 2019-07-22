package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.defaultRuleSetProviders
import org.jmailen.gradle.kotlinter.tasks.format.FormatExecutionContext
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerConfigurationAction
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerParameters
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerRunnable
import java.io.File
import javax.inject.Inject

open class FormatTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    var fileBatchSize = KotlinterExtension.DEFAULT_FILE_BATCH_SIZE

    @Input
    var ktLintParams = KtLintParams()

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val executionContextRepository = ExecutionContextRepository.formatInstance
        val executionContext = FormatExecutionContext(defaultRuleSetProviders, logger)
        val executionContextRepositoryId = executionContextRepository.register(executionContext)

        source
            .toList()
            .chunked(fileBatchSize)
            .map { files ->
                FormatWorkerParameters(
                    files = files,
                    projectDirectory = project.projectDir,
                    executionContextRepositoryId = executionContextRepositoryId,
                    ktLintParams = ktLintParams
                )
            }
            .forEach { parameters ->
                workerExecutor.submit(FormatWorkerRunnable::class.java, FormatWorkerConfigurationAction(parameters))
            }

        workerExecutor.await()
        executionContextRepository.unregister(executionContextRepositoryId)

        if (executionContext.fixes.isNotEmpty()) {
            report.writeText(executionContext.fixes.joinToString("\n"))
        } else {
            report.writeText("ok")
        }
    }
}
