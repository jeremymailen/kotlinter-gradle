package org.jmailen.gradle.kotlinter

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        const val DEFAULT_INDENT_SIZE = 4
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
    }

    /** Don't fail build on lint issues */
    var ignoreFailures = DEFAULT_IGNORE_FAILURES

    var indentSize = DEFAULT_INDENT_SIZE

    var reporter = DEFAULT_REPORTER
}

enum class ReporterType {
    checkstyle,
    json,
    plain
}
