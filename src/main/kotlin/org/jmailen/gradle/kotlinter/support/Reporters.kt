package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.cli.reporter.checkstyle.CheckStyleReporter
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import com.pinterest.ktlint.cli.reporter.html.HtmlReporter
import com.pinterest.ktlint.cli.reporter.json.JsonReporter
import com.pinterest.ktlint.cli.reporter.plain.PlainReporter
import com.pinterest.ktlint.cli.reporter.sarif.SarifReporter
import java.io.File
import java.io.PrintStream

/* ktlint-disable enum-entry-name-case */
enum class ReporterType(val fileExtension: String) {
    checkstyle("xml"),
    html("html"),
    json("json"),
    plain("txt"),
    sarif("sarif.json"),
}
/* ktlint-enable enum-entry-name-case */

fun reporterFor(reporterName: String, output: File): ReporterV2 {
    val out = PrintStream(output)
    return SortedThreadSafeReporterWrapper(
        when (ReporterType.valueOf(reporterName)) {
            ReporterType.checkstyle -> CheckStyleReporter(out)
            ReporterType.html -> HtmlReporter(out)
            ReporterType.json -> JsonReporter(out)
            ReporterType.plain -> PlainReporter(out)
            ReporterType.sarif -> SarifReporter(out)
        },
    )
}

fun reporterPathFor(reporter: ReporterV2, output: File, projectDir: File): String {
    val unwrappedReporter = (reporter as? SortedThreadSafeReporterWrapper)?.unwrap() ?: reporter
    return when (unwrappedReporter) {
        is SarifReporter -> output.absolutePath
        else -> output.toRelativeString(projectDir)
    }
}

fun reporterFileExtension(reporterName: String) = ReporterType.valueOf(reporterName).fileExtension
