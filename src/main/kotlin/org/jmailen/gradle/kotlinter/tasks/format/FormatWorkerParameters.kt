package org.jmailen.gradle.kotlinter.tasks.format

import java.io.File
import java.io.Serializable
import java.util.UUID

/**
 * Serializable stateless parameters that are needed by the FormatWorkerRunnable.
 */
data class FormatWorkerParameters(
    val files: List<File>,
    val projectDirectory: File,
    val executionContextRepositoryId: UUID,
    val experimentalRules: Boolean,
    val allowWildcardImports: Boolean,
    val indentSize: Int,
    val continuationIndentSize: Int
) : Serializable
