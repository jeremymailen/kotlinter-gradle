package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType

open class KotlinterExtension {
    companion object {
        const val DEFAULT_FAIL_BUILD_WHEN_CANNOT_AUTO_FORMAT = false
        const val DEFAULT_IGNORE_FAILURES = false
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
    }

    var failBuildWhenCannotAutoFormat = DEFAULT_FAIL_BUILD_WHEN_CANNOT_AUTO_FORMAT
    var ignoreFailures = DEFAULT_IGNORE_FAILURES
    var reporters = arrayOf(DEFAULT_REPORTER)
}
