package org.jmailen.gradle.kotlinter.functional.utils

import org.intellij.lang.annotations.Language

@Language("kotlin")
internal fun kotlinClass(className: String) = """
    object $className
    
""".trimIndent()

@Language("groovy")
internal val settingsFile = """
    rootProject.name = 'kotlinter'
    
""".trimIndent()

@Language("xml")
internal val androidManifest = """
     <manifest package="com.example" />

""".trimIndent()
