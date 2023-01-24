package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType
import org.jmailen.gradle.kotlinter.support.versionProperties

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        const val DEFAULT_EXPERIMENTAL_RULES = false
        val DEFAULT_REPORTER = ReporterType.Checkstyle.id
        val DEFAULT_DISABLED_RULES = emptyArray<String>()
    }

    var ignoreFailures = DEFAULT_IGNORE_FAILURES

    var reporters = arrayOf(DEFAULT_REPORTER)

    var experimentalRules = DEFAULT_EXPERIMENTAL_RULES

    var disabledRules = DEFAULT_DISABLED_RULES

    var ktlintVersion = versionProperties.ktlintVersion()
}
