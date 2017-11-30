package org.jmailen.gradle.kotlinter

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        const val DEFAULT_INDENT_SIZE = 4
        const val DEFAULT_CONTINUATION_INDENT_SIZE = 8
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
    }

    /** Don't fail build on lint issues */
    var ignoreFailures = DEFAULT_IGNORE_FAILURES

    var indentSize = DEFAULT_INDENT_SIZE

    var continuationIndentSize = DEFAULT_CONTINUATION_INDENT_SIZE

    var reporter: String? = null

    var reporters = arrayOf(DEFAULT_REPORTER)

    // for backwards compatibility
    fun reporters() = reporter?.let { arrayOf(it) } ?: reporters
}

enum class ReporterType {
    checkstyle,
    json,
    plain
}
