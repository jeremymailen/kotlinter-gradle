package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
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
    var indentSize = KotlinterExtension.DEFAULT_INDENT_SIZE

    @Input
    var continuationIndentSize = KotlinterExtension.DEFAULT_CONTINUATION_INDENT_SIZE

    @Input
    var experimentalRules = KotlinterExtension.DEFAULT_EXPERIMENTAL_RULES

    @Input
    var fileBatchSize = KotlinterExtension.DEFAULT_FILE_BATCH_SIZE

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val ruleSets = resolveRuleSets(experimentalRules)
        val executionContextRepository = ExecutionContextRepository.formatInstance
        val executionContext = FormatExecutionContext(ruleSets, logger)
        val executionContextRepositoryId = executionContextRepository.register(executionContext)

        source
            .toList()
            .chunked(fileBatchSize)
            .map { files ->
                FormatWorkerParameters(
                    files = files,
                    projectDirectory = project.projectDir,
                    executionContextRepositoryId = executionContextRepositoryId,
                    experimentalRules = experimentalRules,
                    indentSize = indentSize,
                    continuationIndentSize = continuationIndentSize
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
