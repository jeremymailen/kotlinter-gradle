package org.jmailen.gradle.kotlinter.support

import org.gradle.internal.impldep.com.google.common.io.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

internal class FileFindersTest {
    @Test
    fun `findInParents finds directory in current directory`() {
        val testDir = Files.createTempDir()
        val dotGitDir = File(testDir, ".git").apply { mkdir() }

        val found = findInParents(".git", testDir)

        assertEquals(dotGitDir, found)
    }

    @Test
    fun `findInParents finds directory in parent directory`() {
        val testDir = Files.createTempDir()
        val dotGitDir = File(testDir, ".git").apply { mkdir() }
        val subDir = File(testDir, "foo").apply { mkdir() }

        val found = findInParents(".git", subDir)

        assertEquals(dotGitDir, found)
    }

    @Test
    fun `findInParents returns null when file not found`() {
        val testDir = Files.createTempDir()
        assertNull(findInParents(".git", testDir))
    }
}