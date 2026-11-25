package org.jmailen.gradle.kotlinter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KtLintVersionTest {
    @Test
    fun `only accepts supported releases`() {
        listOf("2.0.0-ALPHA-5", "2.0.0-ALPHA-10", "2.0.0-BETA-1", "2.0.0", "2.1.0").forEach {
            assertTrue(isSupportedKtLintVersion(it), it)
        }
        listOf("1.8.0", "2.0.0-ALPHA-4", "2.0.0-SNAPSHOT", "3.0.0", "invalid").forEach {
            assertFalse(isSupportedKtLintVersion(it), it)
        }
    }
}
