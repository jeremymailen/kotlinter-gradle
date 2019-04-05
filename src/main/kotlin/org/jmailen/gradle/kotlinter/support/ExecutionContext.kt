package org.jmailen.gradle.kotlinter.support

import org.gradle.api.logging.Logger

/**
 * Interface for context that is needed in a Worker Runnable but cannot be passed directly.
 */
interface ExecutionContext {
    val logger: Logger
}