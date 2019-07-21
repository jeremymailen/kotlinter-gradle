package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.support.*
import org.jmailen.gradle.kotlinter.tasks.lint.LintExecutionContext
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerConfigurationAction
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerParameters
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerRunnable
import java.io.File
import javax.inject.Inject

@CacheableTask
open class LintTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : SourceTask() {

    @OutputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var reports: Map<String, File>

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource() = super.getSource()

    @Input
    var ignoreFailures = KotlinterExtension.DEFAULT_IGNORE_FAILURES

    @Input
    var fileBatchSize = KotlinterExtension.DEFAULT_FILE_BATCH_SIZE

    @Input
    var ktLintParams = KtLintParams()

    @Internal
    var sourceSetId = ""

    fun setIndentSize(indent: Int) { ktLintParams.indentSize = indent }

    fun setContinuationIndentSize(indent: Int) { ktLintParams.continuationIndentSize = indent }

    @TaskAction
    fun run() {
        val hasErrorReporter = HasErrorReporter()
        val reporters = reports.map { (reporter, report) ->
            reporterFor(reporter, report)
        } + hasErrorReporter
        val executionContextRepository = ExecutionContextRepository.lintInstance
        val executionContextRepositoryId = executionContextRepository.register(LintExecutionContext(defaultRuleSetProviders, reporters, logger))

        reporters.onEach { it.beforeAll() }

        source
            .toList()
            .chunked(fileBatchSize)
            .map { files ->
                LintWorkerParameters(
                    files = files,
                    projectDirectory = project.projectDir,
                    executionContextRepositoryId = executionContextRepositoryId,
                    name = name,
                    ktLintParams = ktLintParams
                )
            }
            .forEach { parameters ->
                workerExecutor.submit(LintWorkerRunnable::class.java, LintWorkerConfigurationAction(parameters))
            }
        workerExecutor.await()
        executionContextRepository.unregister(executionContextRepositoryId)

        reporters.onEach { it.afterAll() }
        if (hasErrorReporter.hasError && !ignoreFailures) {
            throw GradleException("Kotlin source failed lint check.")
        }
    }
}
