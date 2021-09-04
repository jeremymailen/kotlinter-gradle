package org.jmailen.gradle.kotlinter.support

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ReportersTest {

    @Test
    fun testExtensions() {
        assertEquals("xml", reporterFileExtension("checkstyle"))
        assertEquals("json", reporterFileExtension("json"))
        assertEquals("txt", reporterFileExtension("plain"))
        assertEquals("sarif.json", reporterFileExtension("sarif"))
        assertEquals("html", reporterFileExtension("html"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnknownReporterExtension() {
        reporterFileExtension("unknown")
    }

    @Test
    fun testReporterPathFor() {
        val projectDir = File("/tmp")
        val output = File("/tmp/src/My.kt")
        val reportFile = File.createTempFile("report", "out")

        assertEquals(
            "/tmp/src/My.kt",
            reporterPathFor(reporterFor("sarif", reportFile), output, projectDir)
        )

        assertEquals(
            "src/My.kt",
            reporterPathFor(reporterFor("checkstyle", reportFile), output, projectDir)
        )

        assertEquals(
            "src/My.kt",
            reporterPathFor(reporterFor("json", reportFile), output, projectDir)
        )

        assertEquals(
            "src/My.kt",
            reporterPathFor(reporterFor("html", reportFile), output, projectDir)
        )
    }
}
