package org.jmailen.gradle.kotlinter.support

import java.util.Properties

internal val versionProperties by lazy(::VersionProperties)

internal class VersionProperties : Properties() {
    init {
        load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun kotlinterVersion(): String = getProperty("version")

    fun ktlintVersion(): String = getProperty("ktlint_version")
}
