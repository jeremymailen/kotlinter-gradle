package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jmailen.gradle.kotlinter.tasks.lint.toCliError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

class SortedThreadSafeReporterWrapperTest {

    private lateinit var wrapped: ReporterV2

    private lateinit var reporter: SortedThreadSafeReporterWrapper

    @BeforeEach
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
        val lintError = LintError(0, 0, RuleId("custom:x"), "", false)
        val cliError = lintError.toCliError()

        reporter.onLintError(fileName, cliError)

        verify(wrapped, never()).onLintError(fileName, cliError)
    }

    @Test
    fun onLintErrorIsDelegatedInAfterAll() {
        val fileName = "fileName"
        val lintError = LintError(0, 0, RuleId("custom:x"), "", false)
        val cliError = lintError.toCliError()

        reporter.onLintError(fileName, cliError)

        reporter.afterAll()

        verify(wrapped).onLintError(fileName, cliError)
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
        val firstLintError = LintError(0, 0, RuleId("custom:x"), "", true)
            .toCliError()
        val secondFileName = "a"
        val secondLintError = LintError(1, 0, RuleId("custom:x"), "", false)
            .toCliError()
        val secondLintError2 = LintError(3, 2, RuleId("custom:x"), "", false)
            .toCliError()
        val secondLintError3 = LintError(2, 6, RuleId("custom:x"), "", false)
            .toCliError()
        reporter.before(firstFileName)
        reporter.onLintError(firstFileName, firstLintError)
        reporter.after(firstFileName)
        reporter.before(secondFileName)
        reporter.onLintError(secondFileName, secondLintError)
        reporter.onLintError(secondFileName, secondLintError2)
        reporter.onLintError(secondFileName, secondLintError3)
        reporter.after(secondFileName)

        reporter.afterAll()

        val inOrder = inOrder(wrapped)
        inOrder.verify(wrapped).before(secondFileName)
        inOrder.verify(wrapped).onLintError(secondFileName, secondLintError)
        inOrder.verify(wrapped).onLintError(secondFileName, secondLintError3)
        inOrder.verify(wrapped).onLintError(secondFileName, secondLintError2)
        inOrder.verify(wrapped).after(secondFileName)
        inOrder.verify(wrapped).before(firstFileName)
        inOrder.verify(wrapped).onLintError(firstFileName, firstLintError)
        inOrder.verify(wrapped).after(firstFileName)
        inOrder.verify(wrapped).afterAll()
        inOrder.verifyNoMoreInteractions()
    }
}
