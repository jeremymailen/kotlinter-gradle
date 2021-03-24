package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

class SortedThreadSafeReporterWrapperTest {

    private lateinit var wrapped: Reporter

    private lateinit var reporter: SortedThreadSafeReporterWrapper

    @Before
    fun setUp() {
        wrapped = mock()

        reporter = SortedThreadSafeReporterWrapper(wrapped)
    }

    @Test
    fun beforeAllIsDelegatedToWrapper() {
        reporter.beforeAll()

        verify(wrapped).beforeAll()
    }

    @Test
    fun beforeIsNotDirectlyDelegated() {
        val fileName = "fileName"

        reporter.before(fileName)

        verify(wrapped, never()).before(fileName)
    }

    @Test
    fun beforeIsDelegatedInAfterAll() {
        val fileName = "fileName"
        reporter.before(fileName)

        reporter.afterAll()

        verify(wrapped).before(fileName)
    }

    @Test
    fun onLintErrorIsNotDirectlyDelegated() {
        val fileName = "fileName"
        val lintError = LintError(0, 0, "", "")
        val corrected = false

        reporter.onLintError(fileName, lintError, corrected)

        verify(wrapped, never()).onLintError(fileName, lintError, corrected)
    }

    @Test
    fun onLintErrorIsDelegatedInAfterAll() {
        val fileName = "fileName"
        val lintError = LintError(0, 0, "", "")
        val corrected = false
        reporter.onLintError(fileName, lintError, corrected)

        reporter.afterAll()

        verify(wrapped).onLintError(fileName, lintError, corrected)
    }

    @Test
    fun afterIsNotDirectlyDelegated() {
        val fileName = "fileName"

        reporter.after(fileName)

        verify(wrapped, never()).after(fileName)
    }

    @Test
    fun afterIsDelegatedInAfterAll() {
        val fileName = "fileName"
        reporter.after(fileName)

        reporter.afterAll()

        verify(wrapped).after(fileName)
    }

    @Test
    fun afterAllDelegatesInSortedOrder() {
        val firstFileName = "b"
        val firstLintError = LintError(0, 0, "", "")
        val firstCorrected = true
        val secondFileName = "a"
        val secondLintError = LintError(1, 0, "", "")
        val secondLintError2 = LintError(3, 2, "", "")
        val secondLintError3 = LintError(2, 6, "", "")
        val secondCorrected = false
        reporter.before(firstFileName)
        reporter.onLintError(firstFileName, firstLintError, firstCorrected)
        reporter.after(firstFileName)
        reporter.before(secondFileName)
        reporter.onLintError(secondFileName, secondLintError, secondCorrected)
        reporter.onLintError(secondFileName, secondLintError2, secondCorrected)
        reporter.onLintError(secondFileName, secondLintError3, secondCorrected)
        reporter.after(secondFileName)

        reporter.afterAll()

        val inOrder = inOrder(wrapped)
        inOrder.verify(wrapped).before(secondFileName)
        inOrder.verify(wrapped).onLintError(secondFileName, secondLintError, secondCorrected)
        inOrder.verify(wrapped).onLintError(secondFileName, secondLintError3, secondCorrected)
        inOrder.verify(wrapped).onLintError(secondFileName, secondLintError2, secondCorrected)
        inOrder.verify(wrapped).after(secondFileName)
        inOrder.verify(wrapped).before(firstFileName)
        inOrder.verify(wrapped).onLintError(firstFileName, firstLintError, firstCorrected)
        inOrder.verify(wrapped).after(firstFileName)
        inOrder.verify(wrapped).afterAll()
        inOrder.verifyNoMoreInteractions()
    }
}
