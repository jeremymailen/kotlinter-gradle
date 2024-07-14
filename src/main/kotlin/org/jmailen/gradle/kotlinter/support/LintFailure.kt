package org.jmailen.gradle.kotlinter.support

import org.gradle.api.GradleException

class LintFailure(message: String) : GradleException(message)
