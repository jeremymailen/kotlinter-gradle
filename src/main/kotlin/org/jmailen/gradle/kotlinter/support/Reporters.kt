package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.reporter.checkstyle.CheckStyleReporter
import com.pinterest.ktlint.reporter.json.JsonReporter
import com.pinterest.ktlint.reporter.plain.PlainReporter
import java.io.File
import java.io.PrintStream
import me.cassiano.ktlint.reporter.html.HtmlReporter

enum class ReporterType(val fileExtension: String) {
    checkstyle("xml"),
    html("html"),
    json("json"),
    plain("txt")
}

fun reporterFor(reporterName: String, output: File): Reporter {
    val out = PrintStream(output)
    return SortedThreadSafeReporterWrapper(
        when (ReporterType.valueOf(reporterName)) {
            ReporterType.checkstyle -> CheckStyleReporter(out)
            ReporterType.html -> HtmlReporter(out)
            ReporterType.json -> JsonReporter(out)
            ReporterType.plain -> PlainReporter(out)
        }
    )
}

fun reporterFileExtension(reporterName: String) = ReporterType.valueOf(reporterName).fileExtension
