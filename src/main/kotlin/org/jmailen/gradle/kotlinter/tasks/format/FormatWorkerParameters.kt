package org.jmailen.gradle.kotlinter.tasks.format

import java.io.File
import java.util.UUID
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import org.jmailen.gradle.kotlinter.support.KtLintParams

/**
 * Serializable stateless parameters that are needed by the FormatWorkerRunnable.
 */
interface FormatWorkerParameters : WorkParameters {
    val files: Property<List<File>>
    val projectDirectory: Property<File>
    val executionContextId: Property<UUID>
    val ktLintParams: Property<KtLintParams>
}
