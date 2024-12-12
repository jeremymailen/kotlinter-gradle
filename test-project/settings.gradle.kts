pluginManagement {
    includeBuild("..")
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

include(":service")
include(":rules")
