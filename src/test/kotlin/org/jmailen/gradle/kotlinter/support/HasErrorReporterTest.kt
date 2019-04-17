package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.LintError
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class HasErrorReporterTest {

    private lateinit var reporter: HasErrorReporter

    @Before
    fun setUp() {
        reporter = HasErrorReporter()
    }

    @Test
    fun hasErrorReturnsFalseForOnLintErrorNeverCalled() {
        val result = reporter.hasError

        assertFalse(result)
    }

    @Test
    fun hasErrorReturnsTrueForOnLintErrorCalled() {
        reporter.onLintError("", LintError(0, 0, "", ""), false)

        val result = reporter.hasError

        assertTrue(result)
    }
}
