import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    id("com.gradle.plugin-publish") version "1.1.0"
    `java-gradle-plugin`
    `maven-publish`
    id("org.jmailen.kotlinter") version "4.1.0"
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

version = "4.1.1"
group = "org.jmailen.gradle"
description = projectDescription

object Versions {
    const val androidTools = "7.3.1"
    const val junit = "5.9.3"
    const val ktlint = "1.0.1"
    const val mockitoKotlin = "4.1.0"
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

    listOf(
        "ktlint-rule-engine",
        "ktlint-rule-engine-core",
        "ktlint-cli-reporter-core",
        "ktlint-cli-reporter-checkstyle",
        "ktlint-cli-reporter-json",
        "ktlint-cli-reporter-html",
        "ktlint-cli-reporter-plain",
        "ktlint-cli-reporter-sarif",
        "ktlint-ruleset-standard"
    ).forEach { module ->
        implementation("com.pinterest.ktlint:$module:${Versions.ktlint}")
    }

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit}")
    testImplementation("commons-io:commons-io:2.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")
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
            propertiesFile.writeText("version = $projectVersion")
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
            apiVersion = "1.8"
            languageVersion = "1.8"
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
        gradleVersion = "8.4"
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

java {
    withSourcesJar()
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
                developer {
                    id.set("mateuszkwiecinski")
                    name.set("Mateusz Kwieciński")
                }
            }
        }
    }
}
