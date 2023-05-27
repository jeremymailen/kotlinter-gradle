package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet

/**
 * A wrapper for a Reporter that guarantees thread safety and consistent ordering of all the calls to the reporter.
 * As a downside, the calls to the wrapped reporter are delayed until the end of the execution.
 */
class SortedThreadSafeReporterWrapper(
    private val wrapped: ReporterV2,
) : ReporterV2 {

    private val callsToBefore: ConcurrentMap<String, Unit> = ConcurrentHashMap()
    private val lintErrorReports: ConcurrentMap<String, ConcurrentSkipListSet<KtlintCliError>> = ConcurrentHashMap()
    private val callsToAfter: ConcurrentMap<String, Unit> = ConcurrentHashMap()

    override fun beforeAll() {
        wrapped.beforeAll()
    }

    override fun before(file: String) {
        callsToBefore[file] = Unit
    }

    override fun onLintError(file: String, ktlintCliError: KtlintCliError) {
        lintErrorReports.putIfAbsent(
            file,
            ConcurrentSkipListSet() { o1, o2 ->
                when (o1.line == o2.line) {
                    true -> o1.col.compareTo(o2.col)
                    false -> o1.line.compareTo(o2.line)
                }
            },
        )
        lintErrorReports[file]!!.add(ktlintCliError)
    }

    override fun after(file: String) {
        callsToAfter[file] = Unit
    }

    override fun afterAll() {
        (callsToBefore.keys + lintErrorReports.keys + callsToAfter.keys)
            .sorted()
            .forEach { fileName ->
                if (callsToBefore.contains(fileName)) {
                    wrapped.before(fileName)
                }
                lintErrorReports[fileName]?.let { lintErrorReports ->
                    lintErrorReports.forEach {
                        wrapped.onLintError(fileName, it)
                    }
                }
                if (callsToAfter.contains(fileName)) {
                    wrapped.after(fileName)
                }
            }

        wrapped.afterAll()
    }

    fun unwrap() = wrapped
}
