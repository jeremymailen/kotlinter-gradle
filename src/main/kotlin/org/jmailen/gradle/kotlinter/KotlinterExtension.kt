package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
    }

    var ignoreFailures = DEFAULT_IGNORE_FAILURES

    var reporters = arrayOf(DEFAULT_REPORTER)
}
