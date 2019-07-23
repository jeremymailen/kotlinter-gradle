package org.jmailen.gradle.kotlinter.tasks.format

import org.gradle.api.Action
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration

/**
 * Configures the worker with IsolationMode.NONE and the FormatWorkerParameters.
 */
class FormatWorkerConfigurationAction(
    private val formatWorkerParameters: FormatWorkerParameters
) : Action<WorkerConfiguration> {

    override fun execute(workerConfiguration: WorkerConfiguration) {
        workerConfiguration.isolationMode = IsolationMode.NONE
        workerConfiguration.setParams(formatWorkerParameters)
    }
}
