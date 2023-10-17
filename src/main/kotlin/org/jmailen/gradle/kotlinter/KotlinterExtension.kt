package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        const val DEFAULT_FAIL_BUILD_WHEN_CANNOT_AUTO_FORMAT = false
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
    }

    var ignoreFailures = DEFAULT_IGNORE_FAILURES
    var failBuildWhenCannotAutoFormat = DEFAULT_FAIL_BUILD_WHEN_CANNOT_AUTO_FORMAT
    var reporters = arrayOf(DEFAULT_REPORTER)
}
