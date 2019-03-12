package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        const val DEFAULT_INDENT_SIZE = 4
        const val DEFAULT_CONTINUATION_INDENT_SIZE = 4
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
        const val DEFAULT_EXPERIMENTAL_RULES = false
    }

    /** Don't fail build on lint issues */
    var ignoreFailures = DEFAULT_IGNORE_FAILURES

    var indentSize = DEFAULT_INDENT_SIZE

    var continuationIndentSize = DEFAULT_CONTINUATION_INDENT_SIZE

    var reporter: String? = null

    var reporters = arrayOf(DEFAULT_REPORTER)

    var experimentalRules = DEFAULT_EXPERIMENTAL_RULES

    // for backwards compatibility
    fun reporters() = reporter?.let { arrayOf(it) } ?: reporters
}
