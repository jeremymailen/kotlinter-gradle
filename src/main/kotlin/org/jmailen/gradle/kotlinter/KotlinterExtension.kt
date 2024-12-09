package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType
import org.jmailen.gradle.kotlinter.support.versionProperties

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FORMAT_FAILURES = true
        const val DEFAULT_IGNORE_LINT_FAILURES = false
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
    }

    var ktlintVersion = versionProperties.ktlintVersion()
    var ignoreFormatFailures = DEFAULT_IGNORE_FORMAT_FAILURES
    var ignoreLintFailures = DEFAULT_IGNORE_LINT_FAILURES
    var reporters = arrayOf(DEFAULT_REPORTER)
}
