pluginManagement {
    includeBuild("..")
    repositories {
        gradlePluginPortal()
        google()
    }
    val agpVersion: String by settings
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "com.android.library") {
            useModule("com.android.tools.build:gradle:$agpVersion")
        }
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
