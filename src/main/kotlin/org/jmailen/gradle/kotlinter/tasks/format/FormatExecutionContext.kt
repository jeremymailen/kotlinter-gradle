package org.jmailen.gradle.kotlinter.tasks.format

import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContext
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Execution context for the formatting task.
 */
data class FormatExecutionContext(
    override val logger: Logger,
    val fixes: Queue<String> = ConcurrentLinkedQueue<String>()
) : ExecutionContext