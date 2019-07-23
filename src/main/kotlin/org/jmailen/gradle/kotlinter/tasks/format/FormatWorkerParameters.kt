package org.jmailen.gradle.kotlinter.tasks.format

import java.io.File
import java.io.Serializable
import java.util.UUID
import org.jmailen.gradle.kotlinter.support.KtLintParams

/**
 * Serializable stateless parameters that are needed by the FormatWorkerRunnable.
 */
data class FormatWorkerParameters(
    val files: List<File>,
    val projectDirectory: File,
    val executionContextRepositoryId: UUID,
    val ktLintParams: KtLintParams
) : Serializable
