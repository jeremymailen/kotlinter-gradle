package org.jmailen.gradle.kotlinter.support

class VersionProperties : Properties() {
    init {
        load(this.javaClass.getResourceAsStream("/version.properties"))
    }

    fun version(): String = getProperty("version")
}
