package org.jmailen.gradle.kotlinter.support

/**
 * Silences the "terminally deprecated method in sun.misc.Unsafe has been called" warning which the Kotlin
 * compiler embedded in ktlint triggers on JDK 24+. See https://openjdk.org/jeps/498.
 */
internal const val SUN_MISC_UNSAFE_MEMORY_ACCESS_ALLOW = "--sun-misc-unsafe-memory-access=allow"

/**
 * The option was added in JDK 23 but only became necessary in JDK 24, where the default changed from `allow` to `warn`.
 */
private const val UNSAFE_WARNING_JVM_VERSION = 24

/**
 * Not using Gradle's `JavaVersion` enum since its constants only exist in Gradle versions which already knew about
 * the JDK release in question.
 */
internal fun defaultWorkerJvmArgs(jvmMajorVersion: Int = Runtime.version().feature()): List<String> =
    if (jvmMajorVersion >= UNSAFE_WARNING_JVM_VERSION) listOf(SUN_MISC_UNSAFE_MEMORY_ACCESS_ALLOW) else emptyList()
