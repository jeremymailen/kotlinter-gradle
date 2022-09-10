package org.jmailen.gradle.kotlinter.support

import org.gradle.api.GradleException

internal sealed class KotlinterError(
    message: String,
    cause: Throwable? = null,
) : GradleException(message, cause) {

    class LintingError(
        message: String,
    ) : KotlinterError(message)

    class WorkerError(
        message: String,
        cause: Throwable,
    ) : KotlinterError(message, cause)
}
