package org.jmailen.gradle.kotlinter.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun testUnknownReporterExtension() {
        assertThrows<IllegalArgumentException> { reporterFileExtension("unknown") }
    }

    @Test
    fun testReporterPathFor() {
        val projectDir = File("/tmp")
        val output = File("/tmp/src/My.kt")
        val reportFile = File.createTempFile("report", "out")
        val relativePath = output.toRelativeString(projectDir)
        val absolutePath = output.absolutePath

        assertEquals(
            absolutePath,
            reporterPathFor(reporterFor("sarif", reportFile), output, projectDir),
        )

        assertEquals(
            relativePath,
            reporterPathFor(reporterFor("checkstyle", reportFile), output, projectDir),
        )

        assertEquals(
            relativePath,
            reporterPathFor(reporterFor("json", reportFile), output, projectDir),
        )

        assertEquals(
            relativePath,
            reporterPathFor(reporterFor("html", reportFile), output, projectDir),
        )
    }
}
