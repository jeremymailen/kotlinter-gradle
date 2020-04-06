package org.jmailen.gradle.kotlinter.support

import org.gradle.api.GradleException
import org.gradle.internal.impldep.com.google.common.io.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

internal class GitTest {
    @Test
    fun `findGitDir finds directory successfully`() {
        val testDir = Files.createTempDir()
        val dotGitDir = File(testDir, ".git").apply { mkdir() }

        val found = findGitDir(testDir)

        assertEquals(dotGitDir, found)
    }

    @Test
    fun `findGitDir throws when not dot-git directory present`() {
        val testDir = Files.createTempDir()

        try {
            findGitDir(testDir)
            fail("Expected exception")
        } catch (e: GradleException) {
            assertEquals("Could not find .git directory; searched $testDir and parents", e.message)
        }
    }
}