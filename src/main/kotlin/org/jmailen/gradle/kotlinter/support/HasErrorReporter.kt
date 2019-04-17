package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Reporter collecting whether or not an error has occurred.
 */
class HasErrorReporter : Reporter {

    val hasError: Boolean
        get() = hasErrorAtomic.get()

    private val hasErrorAtomic = AtomicBoolean(false)

    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        hasErrorAtomic.set(true)
    }
}
