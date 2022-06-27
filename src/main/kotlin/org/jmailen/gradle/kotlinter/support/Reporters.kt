package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.reporter.checkstyle.CheckStyleReporter
import com.pinterest.ktlint.reporter.html.HtmlReporter
import com.pinterest.ktlint.reporter.json.JsonReporter
import com.pinterest.ktlint.reporter.plain.PlainReporter
import com.pinterest.ktlint.reporter.sarif.SarifReporter
import java.io.File
import java.io.PrintStream

/* ktlint-disable enum-entry-name-case */
internal enum class ReporterType(val fileExtension: String) {
    checkstyle("xml"),
    html("html"),
    json("json"),
    plain("txt"),
    sarif("sarif.json"),
}
/* ktlint-enable enum-entry-name-case */

fun reporterFor(reporterName: String, output: File): Reporter {
    val out = PrintStream(output)
    return SortedThreadSafeReporterWrapper(
        when (ReporterType.valueOf(reporterName)) {
            ReporterType.checkstyle -> CheckStyleReporter(out)
            ReporterType.html -> HtmlReporter(out)
            ReporterType.json -> JsonReporter(out)
            ReporterType.plain -> PlainReporter(out)
            ReporterType.sarif -> SarifReporter(out)
        }
    )
}

fun reporterPathFor(reporter: Reporter, output: File, projectDir: File): String {
    val unwrappedReporter = (reporter as? SortedThreadSafeReporterWrapper)?.unwrap() ?: reporter
    return when (unwrappedReporter) {
        is SarifReporter -> output.absolutePath
        else -> output.toRelativeString(projectDir)
    }
}

fun reporterFileExtension(reporterName: String) = ReporterType.valueOf(reporterName).fileExtension
