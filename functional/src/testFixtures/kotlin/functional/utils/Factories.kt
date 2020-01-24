package functional.utils

import org.intellij.lang.annotations.Language

@Language("kotlin")
fun kotlinClass(className: String) = """
    object $className
    
""".trimIndent()

@Language("groovy")
val settingsFile = """
    rootProject.name = 'kotlinter'
    
""".trimIndent()

@Language("xml")
val androidManifest = """
     <manifest package="com.example" />

""".trimIndent()
