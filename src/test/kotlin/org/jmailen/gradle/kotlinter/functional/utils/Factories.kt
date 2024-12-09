package org.jmailen.gradle.kotlinter.functional.utils

// language=kotlin
internal fun kotlinClass(className: String) = """
    object $className
    
""".trimIndent()

// language=groovy
internal val settingsFile =
    """
    rootProject.name = 'kotlinter'
    
    """.trimIndent()

// language=xml
internal val androidManifest =
    """
     <manifest package="com.example" />

    """.trimIndent()

internal val editorConfig =
    """
    [*.kt]
    
    """

internal val repositories =
    """
    repositories {
        mavenCentral()
    }

    """.trimIndent()
