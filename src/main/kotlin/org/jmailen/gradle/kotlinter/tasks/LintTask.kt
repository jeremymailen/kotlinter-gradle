package org.jmailen.gradle.kotlinter.tasks

import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_IGNORE_FAILURES
import org.jmailen.gradle.kotlinter.support.ExecutionContextRepository
import org.jmailen.gradle.kotlinter.support.HasErrorReporter
import org.jmailen.gradle.kotlinter.support.defaultRuleSetProviders
import org.jmailen.gradle.kotlinter.support.reporterFor
import org.jmailen.gradle.kotlinter.tasks.lint.LintExecutionContext
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerConfigurationAction
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerParameters
import org.jmailen.gradle.kotlinter.tasks.lint.LintWorkerRunnable

@CacheableTask
open class LintTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : ConfigurableKtLintTask() {

    @Optional
    @OutputFiles
    val reports = mapProperty(default = emptyMap<String, File>())

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource() = super.getSource()

    @Input
    val ignoreFailures = property(default = DEFAULT_IGNORE_FAILURES)

    @TaskAction
    fun run() {
        val hasErrorReporter = HasErrorReporter()
        val reporters = reports.get().map { (reporter, report) ->
            reporterFor(reporter, report)
        } + hasErrorReporter
        val executionContextRepository = ExecutionContextRepository.lintInstance
        val executionContextRepositoryId =
            executionContextRepository.register(LintExecutionContext(defaultRuleSetProviders, reporters, logger))

        reporters.onEach { it.beforeAll() }
        getChunkedSource()
            .map { files ->
                LintWorkerParameters(
                    files = files,
                    projectDirectory = project.projectDir,
                    executionContextRepositoryId = executionContextRepositoryId,
                    name = name,
                    ktLintParams = getKtLintParams()
                )
            }
            .forEach { parameters ->
                workerExecutor.submit(LintWorkerRunnable::class.java, LintWorkerConfigurationAction(parameters))
            }
        workerExecutor.await()
        executionContextRepository.unregister(executionContextRepositoryId)

        reporters.onEach { it.afterAll() }
        if (hasErrorReporter.hasError && !ignoreFailures.get()) {
            throw GradleException("Kotlin source failed lint check.")
        }
    }
}
