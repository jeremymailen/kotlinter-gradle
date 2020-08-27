package org.jmailen.gradle.kotlinter.support

import java.util.Properties

class VersionProperties : Properties() {
    init {
        load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun version() = getProperty("version")
}
