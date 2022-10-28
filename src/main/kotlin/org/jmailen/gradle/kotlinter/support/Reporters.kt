package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.File
import java.io.PrintStream
import java.util.ServiceLoader

internal enum class ReporterType(val id: String, val fileExtension: String) {
    Checkstyle(id = "checkstyle", fileExtension = "xml"),
    Html(id = "html", fileExtension = "html"),
    Json(id = "json", fileExtension = "json"),
    Plain(id = "plain", fileExtension = "txt"),
    Sarif(id = "sarif", fileExtension = "sarif.json"),
    ;

    companion object {

        fun getById(id: String) =
            values().firstOrNull { it.id == id } ?: error("Unknown reporter type=$id")
    }
}

internal fun reporterPathFor(reporterType: ReporterType, output: File, relativeRoot: File) = when (reporterType) {
    ReporterType.Checkstyle,
    ReporterType.Html,
    ReporterType.Json,
    ReporterType.Plain,
    -> output.toRelativeString(base = relativeRoot)

    ReporterType.Sarif -> output.absolutePath
}

internal fun resolveReporters(
    enabled: Map<ReporterType, File>,
): Map<ReporterType, Reporter> {
    val allReporterProviders = defaultReporters().associateBy { it.id }

    return enabled
        .filter { (type, _) -> allReporterProviders.containsKey(type.id) }
        .mapValues { (type, output) ->
            allReporterProviders.getValue(type.id).get(
                out = PrintStream(output),
                opt = type.generateOpt(),
            )
        }
}

private fun ReporterType.generateOpt() = when (this) {
    ReporterType.Checkstyle,
    ReporterType.Html,
    ReporterType.Json,
    ReporterType.Sarif,
    -> emptyMap()

    ReporterType.Plain -> mapOf("color_name" to "DARK_GRAY")
}

internal fun loadBaselineReporter(output: File) = defaultReporters()
    .first { it.id == "baseline" }
    .get(PrintStream(output), emptyMap())

private fun defaultReporters(): List<ReporterProvider<*>> =
    ServiceLoader.load(ReporterProvider::class.java).toList()
