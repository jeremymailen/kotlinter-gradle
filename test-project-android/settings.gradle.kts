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
include("app")
include("custom-ktlint-rules")
