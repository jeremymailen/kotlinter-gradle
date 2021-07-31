import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.gradle.plugin-publish") version "0.14.0"
    `java-gradle-plugin`
    `maven-publish`
    id("org.jmailen.kotlinter") version "3.4.4"
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

version = "3.4.5"
group = "org.jmailen.gradle"
description = projectDescription

object Versions {
    const val androidTools = "4.1.3"
    const val jetbrainsAnnotations = "20.1.0"
    const val junit = "4.13.2"
    const val ktlint = "0.42.0"
    const val mockitoKotlin = "3.1.0"
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:${Versions.androidTools}")

    listOf(
        "ktlint-core",
        "ktlint-reporter-checkstyle",
        "ktlint-reporter-json",
        "ktlint-reporter-html",
        "ktlint-reporter-plain",
        "ktlint-ruleset-experimental",
        "ktlint-ruleset-standard"
    ).forEach { module ->
        implementation("com.pinterest.ktlint:$module:${Versions.ktlint}")
    }

    testImplementation("junit:junit:${Versions.junit}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")
    testImplementation("org.jetbrains:annotations:${Versions.jetbrainsAnnotations}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks {
    val generateVersionProperties = register("generateVersionProperties") {
        doLast {
            val resourcesDir = sourceSets.main.get().resources.sourceDirectories.asPath
            File(mkdir(resourcesDir), "version.properties").writeText("version = $version")
        }
    }

    processResources {
        dependsOn(generateVersionProperties)
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            apiVersion = "1.4"
            languageVersion = "1.4"
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    // Required to put the Kotlin plugin on the classpath for the functional test suite
    withType<PluginUnderTestMetadata>().configureEach {
        pluginClasspath.from(configurations.compileOnly)
    }

    wrapper {
        gradleVersion = "7.0.2"
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

gradlePlugin {
    plugins {
        create("kotlinterPlugin") {
            id = pluginId
            implementationClass = "org.jmailen.gradle.kotlinter.KotlinterPlugin"
        }
    }
}

pluginBundle {
    website = webUrl
    vcsUrl = githubUrl
    description = project.description
    tags = listOf("kotlin", "ktlint", "lint", "format", "style", "android")

    plugins {
        named("kotlinterPlugin") {
            displayName = "Kotlin Lint plugin"
        }
    }
}

artifacts {
    add(configurations.archives.name, sourcesJar)
}

publishing {
    publications.withType<MavenPublication> {
        artifact(sourcesJar.get())

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
