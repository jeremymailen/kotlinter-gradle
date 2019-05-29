package org.jmailen.gradle.kotlinter.tasks.lint

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
    val experimentalRules: Boolean,
    val allowWildcardImports: Boolean,
    val indentSize: Int,
    val continuationIndentSize: Int
) : Serializable
