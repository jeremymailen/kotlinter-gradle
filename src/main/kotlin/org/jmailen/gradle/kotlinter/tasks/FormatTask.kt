package org.jmailen.gradle.kotlinter.tasks

import java.io.File
import javax.inject.Inject
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
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerAction

open class FormatTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : SourceTask() {

    @OutputFile
    lateinit var report: File

    @Input
    var fileBatchSize = KotlinterExtension.DEFAULT_FILE_BATCH_SIZE

    @Input
    var ktLintParams = KtLintParams()

    fun setIndentSize(indentSize: Int) {
        ktLintParams.indentSize = indentSize
    }

    fun setContinuationIndentSize(continuationIndentSize: Int) {
        ktLintParams.continuationIndentSize = continuationIndentSize
    }

    fun setExperimentalRules(experimentalRules: Boolean) {
        ktLintParams.experimentalRules = experimentalRules
    }

    fun setDisabledRules(disabledRules: Array<String>) {
        ktLintParams.disabledRules = disabledRules
    }

    fun setEditorConfigPath(editorConfigPath: String) {
        ktLintParams.editorConfigPath = editorConfigPath
    }

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val executionContextRepository = ExecutionContextRepository.formatInstance
        val executionContext = FormatExecutionContext(defaultRuleSetProviders, logger)
        val executionContextId = executionContextRepository.register(executionContext)

        source
            .toList()
            .chunked(fileBatchSize)
            .forEach { files ->
                workerExecutor.classLoaderIsolation().submit(FormatWorkerAction::class.java) { params ->
                    params.files.set(files)
                    params.projectDirectory.set(project.projectDir)
                    params.executionContextId.set(executionContextId)
                    params.ktLintParams.set(ktLintParams)
                }
            }

        workerExecutor.await()
        executionContextRepository.unregister(executionContextId)

        if (executionContext.fixes.isNotEmpty()) {
            report.writeText(executionContext.fixes.joinToString("\n"))
        } else {
            report.writeText("ok")
        }
    }
}
