package org.jmailen.gradle.kotlinter

import org.jmailen.gradle.kotlinter.support.ReporterType

open class KotlinterExtension {
    companion object {
        const val DEFAULT_IGNORE_FAILURES = false
        val DEFAULT_REPORTER = ReporterType.checkstyle.name
        const val DEFAULT_EXPERIMENTAL_RULES = false
        val DEFAULT_DISABLED_RULES = emptyArray<String>()
        const val DEFAULT_FILE_BATCH_SIZE = 30
    }

    /** Don't fail build on lint issues */
    var ignoreFailures = DEFAULT_IGNORE_FAILURES

    var indentSize: Int? = null

    var reporters = arrayOf(DEFAULT_REPORTER)

    var experimentalRules = DEFAULT_EXPERIMENTAL_RULES

    var disabledRules = DEFAULT_DISABLED_RULES

    /** The file list is split into batches and processed together on a Worker API call */
    var fileBatchSize = DEFAULT_FILE_BATCH_SIZE
}
