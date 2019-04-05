package org.jmailen.gradle.kotlinter.tasks.format

import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContext

/**
 * Execution context for the formatting task.
 */
data class FormatExecutionContext(
    override val logger: Logger,
    val fixes: MutableList<String> = mutableListOf()
) : ExecutionContext