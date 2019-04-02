package org.jmailen.gradle.kotlinter.support

import com.github.shyiko.ktlint.core.Reporter
import org.gradle.api.logging.Logger

/**
 * Context that is needed in a Worker Runnable but cannot be passed directly.
 */
data class ExecutionContext(
    val reporters: List<Reporter>,
    val logger: Logger
)