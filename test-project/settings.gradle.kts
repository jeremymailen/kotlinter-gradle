pluginManagement {
    includeBuild("..")
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":service")
include(":rules")
include(":legacy-rules")
