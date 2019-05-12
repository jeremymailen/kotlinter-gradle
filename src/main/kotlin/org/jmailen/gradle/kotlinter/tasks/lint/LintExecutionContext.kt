package org.jmailen.gradle.kotlinter.tasks.lint

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.RuleSetProvider
import org.gradle.api.logging.Logger
import org.jmailen.gradle.kotlinter.support.ExecutionContext

/**
 * Execution context for the linting task.
 */
data class LintExecutionContext(
    val ruleSetProviders: Iterable<RuleSetProvider>,
    val reporters: List<Reporter>,
    override val logger: Logger
) : ExecutionContext
