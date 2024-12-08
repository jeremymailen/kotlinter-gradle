package org.jmailen.gradle.kotlinter.support

import java.util.Properties

val versionProperties by lazy { VersionProperties() }

class VersionProperties : Properties() {
    init {
        load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun version(): String = getProperty("version")

    fun ktlintVersion(): String = getProperty("ktlintVersion")
}
