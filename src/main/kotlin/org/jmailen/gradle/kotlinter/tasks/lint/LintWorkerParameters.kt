package org.jmailen.gradle.kotlinter.tasks.lint

import org.jmailen.gradle.kotlinter.support.KtLintParams
import java.io.File
import java.io.Serializable
import java.util.UUID

/**
 * Serializable stateless parameters that are needed by the LintWorkerRunnable.
 */
data class LintWorkerParameters(
    val files: List<File>,
    val projectDirectory: File,
    val name: String,
    val executionContextRepositoryId: UUID,
    val ktLintParams: KtLintParams
) : Serializable
