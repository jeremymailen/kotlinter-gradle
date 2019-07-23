package org.jmailen.gradle.kotlinter.tasks.lint

import org.gradle.api.Action
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration

/**
 * Configures the worker with IsolationMode.NONE and the LintWorkerParameters.
 */
class LintWorkerConfigurationAction(
    private val lintWorkerParameters: LintWorkerParameters
) : Action<WorkerConfiguration> {

    override fun execute(workerConfiguration: WorkerConfiguration) {
        workerConfiguration.isolationMode = IsolationMode.NONE
        workerConfiguration.setParams(lintWorkerParameters)
    }
}
