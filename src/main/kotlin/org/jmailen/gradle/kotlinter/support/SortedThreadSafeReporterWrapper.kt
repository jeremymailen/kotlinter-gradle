package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet

/**
 * A wrapper for a Reporter that guarantees thread safety and consistent ordering of all the calls to the reporter.
 * As a downside, the calls to the wrapped reporter are delayed until the end of the execution.
 */
class SortedThreadSafeReporterWrapper(
    private val wrapped: Reporter
) : Reporter {

    private val callsToBefore: ConcurrentMap<String, Unit> = ConcurrentHashMap()
    private val lintErrorReports: ConcurrentMap<String, ConcurrentSkipListSet<LintErrorReport>> = ConcurrentHashMap()
    private val callsToAfter: ConcurrentMap<String, Unit> = ConcurrentHashMap()

    override fun beforeAll() {
        wrapped.beforeAll()
    }

    override fun before(file: String) {
        callsToBefore[file] = Unit
    }

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        lintErrorReports.putIfAbsent(file, ConcurrentSkipListSet())
        lintErrorReports[file]!!.add(LintErrorReport(err, corrected))
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
                lintErrorReports[fileName]?.forEach {
                    wrapped.onLintError(fileName, it.lintError, it.corrected)
                }
                if (callsToAfter.contains(fileName)) {
                    wrapped.after(fileName)
                }
            }

        wrapped.afterAll()
    }

    private data class LintErrorReport(
        val lintError: LintError,
        val corrected: Boolean
    ) : Comparable<LintErrorReport> {
        override fun compareTo(other: LintErrorReport) = if (lintError.line == other.lintError.line)
            lintError.col.compareTo(other.lintError.col)
        else
            lintError.line.compareTo(other.lintError.line)
    }
}
