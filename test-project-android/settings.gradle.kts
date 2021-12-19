pluginManagement {
    includeBuild("..")
    repositories {
        gradlePluginPortal()
        google()
    }
    val agpVersion: String by settings
    plugins {
        id("com.android.library") version agpVersion
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
