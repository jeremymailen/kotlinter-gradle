package org.jmailen.gradle.kotlinter.tasks

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class GenerateHookTest {
    private val gradlew = "gradlewLocation"

    private val hookContent = """
        Foobar
        Baz
    """.trimIndent()

    @Test
    fun testGenerateHook_HasAllContent() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent, true)
        assertTrue(hook.contains(InstallHookTask.shebang))
        assertTrue(hook.contains(InstallHookTask.startHook))
        assertTrue(hook.contains("GRADLEW=$gradlew"))
        assertTrue(hook.contains(hookContent))
        assertTrue(hook.contains(InstallHookTask.endHook))
    }

    @Test
    fun testGenerateHook_IncludesStartHook() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent)
        assertTrue(hook.contains(InstallHookTask.startHook))
    }

    @Test
    fun testGenerateHook_IncludesHookContent() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent)
        assertTrue(hook.contains(hookContent))
    }

    @Test
    fun testGenerateHook_IncludesEndHook() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent)
        assertTrue(hook.contains(InstallHookTask.endHook))
    }

    @Test
    fun testGenerateHook_DoesNotIncludeEndHook() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent, includeEndHook = false)
        assertFalse(hook.contains(InstallHookTask.endHook))
    }

    @Test
    fun testGenerateHook_AddsShebang() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent, addShebang = true)
        assertTrue(hook.contains(InstallHookTask.shebang))
    }

    @Test
    fun testGenerateHook_DoesNotAddShebang() {
        val hook = InstallHookTask.generateHook(gradlew, hookContent, addShebang = false)
        assertFalse(hook.contains(InstallHookTask.shebang))
    }
}
