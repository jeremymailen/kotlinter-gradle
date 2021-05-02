buildscript {
    repositories {
        gradlePluginPortal {
            content {
                excludeGroup("org.jmailen.gradle")
            }
        }
        if (project.hasProperty("useMavenLocal")) {
            mavenLocal()
        }
    }
    dependencies {
        if (project.hasProperty("useMavenLocal")) {
            classpath("org.jmailen.gradle.local:kotlinter-gradle:+")
        } else {
            classpath("org.jmailen.gradle:kotlinter-gradle")
        }
    }
}

plugins {
    kotlin("jvm") version "1.5.0"
}

apply(plugin = "org.jmailen.kotlinter")
repositories.mavenCentral()
