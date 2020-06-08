package org.jmailen.gradle.kotlinter.tasks

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class GenerateHookTest {
    private val gradlew = "gradlewLocation"

    @Test
    fun testGenerateHook_HasAllContent() {
        val hook = InstallHookTask.generateHook(gradlew, true)
        assertTrue(hook.contains(InstallHookTask.shebang))
        assertTrue(hook.contains(InstallHookTask.startHook))
        assertTrue(hook.contains("GRADLEW=$gradlew"))
        assertTrue(hook.contains(InstallHookTask.hookContent))
        assertTrue(hook.contains(InstallHookTask.endHook))
    }

    @Test
    fun testGenerateHook_IncludesStartHook() {
        val hook = InstallHookTask.generateHook(gradlew)
        assertTrue(hook.contains(InstallHookTask.startHook))
    }

    @Test
    fun testGenerateHook_IncludesHookContent() {
        val hook = InstallHookTask.generateHook(gradlew)
        assertTrue(hook.contains(InstallHookTask.hookContent))
    }

    @Test
    fun testGenerateHook_IncludesEndHook() {
        val hook = InstallHookTask.generateHook(gradlew)
        assertTrue(hook.contains(InstallHookTask.endHook))
    }

    @Test
    fun testGenerateHook_DoesNotIncludeEndHook() {
        val hook = InstallHookTask.generateHook(gradlew, includeEndHook = false)
        assertFalse(hook.contains(InstallHookTask.endHook))
    }

    @Test
    fun testGenerateHook_AddsShebang() {
        val hook = InstallHookTask.generateHook(gradlew, addShebang = true)
        assertTrue(hook.contains(InstallHookTask.shebang))
    }

    @Test
    fun testGenerateHook_DoesNotAddShebang() {
        val hook = InstallHookTask.generateHook(gradlew, addShebang = false)
        assertFalse(hook.contains(InstallHookTask.shebang))
    }
}
