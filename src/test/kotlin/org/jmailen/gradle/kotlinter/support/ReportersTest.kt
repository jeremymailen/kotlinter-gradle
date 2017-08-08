package org.jmailen.gradle.kotlinter.support

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportersTest {

    @Test
    fun testExtensions() {
        assertEquals(reporterFileExtension("checkstyle"), "xml")
        assertEquals(reporterFileExtension("json"), "json")
        assertEquals(reporterFileExtension("plain"), "txt")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnknownReporterExtension() {
        reporterFileExtension("unknown")
    }
}
