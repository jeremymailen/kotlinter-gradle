package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.LintError

internal fun List<LintError>.doesNotContain(error: LintError): Boolean = none { it.isSameAs(error) }

private fun LintError.isSameAs(lintError: LintError) =
    col == lintError.col &&
        line == lintError.line &&
        ruleId == lintError.ruleId
