package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.Reporter
import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContext

/**
 * Execution context for the linting task.
 */
data class LintExecutionContext(
    val reporters: List<Reporter>,
    override val logger: Logger
) : ExecutionContext
