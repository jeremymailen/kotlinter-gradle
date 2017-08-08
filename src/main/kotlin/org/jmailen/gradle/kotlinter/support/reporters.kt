package org.jmailen.gradle.kotlinter.support

import com.github.shyiko.ktlint.core.Reporter
import com.github.shyiko.ktlint.reporter.checkstyle.CheckStyleReporter
import com.github.shyiko.ktlint.reporter.json.JsonReporter
import com.github.shyiko.ktlint.reporter.plain.PlainReporter
import org.jmailen.gradle.kotlinter.ReporterType
import java.io.File
import java.io.PrintStream

fun reporterFor(reporterName: String, output: File): Reporter {
    val out = PrintStream(output)
    return when (ReporterType.valueOf(reporterName)) {
        ReporterType.checkstyle -> CheckStyleReporter(out)
        ReporterType.json -> JsonReporter(out)
        ReporterType.plain -> PlainReporter(out)
    }
}

fun reporterFileExtension(reporterName: String) = when (ReporterType.valueOf(reporterName)) {
    ReporterType.checkstyle -> "xml"
    ReporterType.json -> "json"
    ReporterType.plain -> "txt"
}
