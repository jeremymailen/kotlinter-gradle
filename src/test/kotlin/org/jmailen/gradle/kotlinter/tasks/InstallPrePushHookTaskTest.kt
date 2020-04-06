package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.GradleException
import org.jmailen.gradle.kotlinter.functional.WithGradleTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files

internal class InstallPrePushHookTaskTest : WithGradleTest.Kotlin() {
    @Test
    fun `findGitDir finds directory successfully`() {
        val dotGitDir = testProjectDir.newFolder(".git")

        val found = InstallPrePushHookTask.foo(testProjectDir.root)

        assertEquals(dotGitDir, found)
    }

    @Test
    fun `findGitDir throws when not dot-git directory present`() {
        try {
            InstallPrePushHookTask.foo(testProjectDir.root)
            fail("Expected exception")
        } catch (e: GradleException) {
            assertEquals("Could not find .git directory; searched $testProjectDir and parents", e.message)
        }
    }
}