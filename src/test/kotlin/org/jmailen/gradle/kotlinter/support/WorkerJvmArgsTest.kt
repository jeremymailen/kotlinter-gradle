package org.jmailen.gradle.kotlinter.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkerJvmArgsTest {

    @Test
    fun `allows sun misc Unsafe memory access on jvms which warn about it`() {
        assertEquals(listOf(SUN_MISC_UNSAFE_MEMORY_ACCESS_ALLOW), defaultWorkerJvmArgs(jvmMajorVersion = 24))
        assertEquals(listOf(SUN_MISC_UNSAFE_MEMORY_ACCESS_ALLOW), defaultWorkerJvmArgs(jvmMajorVersion = 25))
    }

    @Test
    fun `passes no arguments on jvms which don't know the option`() {
        assertEquals(emptyList<String>(), defaultWorkerJvmArgs(jvmMajorVersion = 11))
        assertEquals(emptyList<String>(), defaultWorkerJvmArgs(jvmMajorVersion = 21))
    }
}
