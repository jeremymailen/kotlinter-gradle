import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.gradle.plugin-publish") version "1.1.0"
    `java-gradle-plugin`
    `maven-publish`
    id("org.jmailen.kotlinter") version "3.13.0"
    idea
}

repositories {
    mavenCentral()
    google()
}

val pluginId = "org.jmailen.kotlinter"
val githubUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val webUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val projectDescription = "Lint and formatting for Kotlin using ktlint with configuration-free setup on JVM and Android projects"

version = "3.13.0"
group = "org.jmailen.gradle"
description = projectDescription

object Versions {
    const val androidTools = "7.3.1"
    const val junit = "5.9.1"
    const val ktlint = "0.48.1"
}

configurations {
    register("testRuntimeDependencies") {
        extendsFrom(compileOnly.get())
        attributes {
            // KGP publishes multiple variants https://kotlinlang.org/docs/whatsnew17.html#support-for-gradle-plugin-variants
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        }
    }
    configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
                useVersion(getKotlinPluginVersion())
            }
        }
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:${Versions.androidTools}")
    compileOnly("com.pinterest.ktlint:ktlint-core:${Versions.ktlint}")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testImplementation("commons-io:commons-io:2.11.0")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    val generateVersionProperties = register("generateVersionProperties") {
        val projectVersion = version
        val propertiesFile = File(sourceSets.main.get().output.resourcesDir, "version.properties")
        inputs.property("projectVersion", projectVersion)
        outputs.file(propertiesFile)

        doLast {
            propertiesFile.writeText(
                """
                version = $projectVersion
                ktlint_version = ${Versions.ktlint}
                
                """.trimIndent()
            )
        }
    }

    processResources {
        dependsOn(generateVersionProperties)
    }

    val targetJavaVersion = JavaVersion.VERSION_1_8
    withType<JavaCompile>().configureEach {
        options.release.set(targetJavaVersion.majorVersion.toInt())
    }
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            apiVersion = "1.4"
            languageVersion = "1.4"
            jvmTarget = targetJavaVersion.toString()
        }
    }
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // Required to put the Kotlin plugin on the classpath for the functional test suite
    withType<PluginUnderTestMetadata>().configureEach {
        pluginClasspath.from(configurations.getByName("testRuntimeDependencies"))
    }

    wrapper {
        gradleVersion = "7.6"
    }
}

gradlePlugin {
    website.set(webUrl)
    vcsUrl.set(githubUrl)
    plugins {
        create("kotlinterPlugin") {
            id = pluginId
            displayName = "Kotlin Lint plugin"
            description = project.description
            tags.addAll(listOf("kotlin", "ktlint", "lint", "format", "style", "android"))
            implementationClass = "org.jmailen.gradle.kotlinter.KotlinterPlugin"
        }
    }
}

publishing {
    publications.withType<MavenPublication> {

        pom {
            name.set(project.name)
            description.set(project.description)
            url.set(webUrl)

            scm {
                url.set(githubUrl)
            }

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("jeremymailen")
                    name.set("Jeremy Mailen")
                }
            }
        }
    }
}
