package org.jmailen.gradle.kotlinter.support

import org.gradle.api.GradleException

class KotlinterError(
    message: String,
    error: Throwable
) : GradleException(message, error)
