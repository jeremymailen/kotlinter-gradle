package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.reporter.checkstyle.CheckStyleReporter
import com.pinterest.ktlint.reporter.json.JsonReporter
import com.pinterest.ktlint.reporter.plain.PlainReporter
import java.io.File
import java.io.PrintStream

enum class ReporterType {
    checkstyle,
    html,
    json,
    plain
}

fun reporterFor(reporterName: String, output: File): Reporter {
    val out = PrintStream(output)
    return SortedThreadSafeReporterWrapper(
        when (ReporterType.valueOf(reporterName)) {
            ReporterType.checkstyle -> CheckStyleReporter(out)
            ReporterType.html -> throw NotImplementedError("html reporter not yet compatible with version") // HtmlReporter(out)
            ReporterType.json -> JsonReporter(out)
            ReporterType.plain -> PlainReporter(out)
        }
    )
}

fun reporterFileExtension(reporterName: String) = when (ReporterType.valueOf(reporterName)) {
    ReporterType.checkstyle -> "xml"
    ReporterType.html -> "html"
    ReporterType.json -> "json"
    ReporterType.plain -> "txt"
}
