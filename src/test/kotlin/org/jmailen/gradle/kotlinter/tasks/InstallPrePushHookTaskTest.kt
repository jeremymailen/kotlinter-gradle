package org.jmailen.gradle.kotlinter.tasks

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class InstallPrePushHookTaskTest {
    private val gradlew = "gradlewLocation"

    @Test
    fun testGenerateHook_HasAllContent() {
        val hook = InstallPrePushHookTask.generateHook(gradlew, true)
        assertTrue(hook.contains(InstallPrePushHookTask.shebang))
        assertTrue(hook.contains(InstallPrePushHookTask.startHook))
        assertTrue(hook.contains("GRADLEW=$gradlew"))
        assertTrue(hook.contains(InstallPrePushHookTask.hookContent))
        assertTrue(hook.contains(InstallPrePushHookTask.endHook))
    }

    @Test
    fun testGenerateHook_IncludesStartHook() {
        val hook = InstallPrePushHookTask.generateHook(gradlew)
        assertTrue(hook.contains(InstallPrePushHookTask.startHook))
    }

    @Test
    fun testGenerateHook_IncludesHookContent() {
        val hook = InstallPrePushHookTask.generateHook(gradlew)
        assertTrue(hook.contains(InstallPrePushHookTask.hookContent))
    }

    @Test
    fun testGenerateHook_IncludesEndHook() {
        val hook = InstallPrePushHookTask.generateHook(gradlew)
        assertTrue(hook.contains(InstallPrePushHookTask.endHook))
    }

    @Test
    fun testGenerateHook_DoesNotIncludeEndHook() {
        val hook = InstallPrePushHookTask.generateHook(gradlew, includeEndHook = false)
        assertFalse(hook.contains(InstallPrePushHookTask.endHook))
    }

    @Test
    fun testGenerateHook_AddsShebang() {
        val hook = InstallPrePushHookTask.generateHook(gradlew, addShebang = true)
        assertTrue(hook.contains(InstallPrePushHookTask.shebang))
    }

    @Test
    fun testGenerateHook_DoesNotAddShebang() {
        val hook = InstallPrePushHookTask.generateHook(gradlew, addShebang = false)
        assertFalse(hook.contains(InstallPrePushHookTask.shebang))
    }
}
