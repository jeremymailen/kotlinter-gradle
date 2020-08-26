import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files

plugins {
    kotlin("jvm") version "1.4.0"
    id("com.gradle.plugin-publish") version "0.12.0"
    `java-gradle-plugin`
    `maven-publish`
    // id("org.jmailen.kotlinter") version "2.4.1"
    idea
}

repositories {
    jcenter()
    google()
}

val pluginId = "org.jmailen.kotlinter"
val githubUrl ="https://github.com/jeremymailen/kotlinter-gradle"
val webUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val projectDescription = "Lint and formatting for Kotlin using ktlint with configuration-free setup on JVM and Android projects"

version = "3.0.0"
group = "org.jmailen.gradle"
description = projectDescription

object Versions {
    const val androidTools = "4.0.1"
    const val jetbrainsAnnotations = "20.0.0"
    const val junit = "4.13"
    const val ktlint = "0.38.1"
    const val mockitoKotlin = "2.2.0"
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
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}")
    testImplementation("org.jetbrains:annotations:${Versions.jetbrainsAnnotations}")
}

// Required to put the Kotlin plugin on the classpath for the functional test suite
tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles javaDoc JAR"
    archiveClassifier.set("javadoc")
    from(tasks["javadoc"])
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
    add(configurations.archives.name, javadocJar)
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
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            apiVersion = "1.3"
            languageVersion = "1.3"
            jvmTarget = "1.8"
        }
    }

    wrapper {
        gradleVersion = "6.6"
    }

    /**
     * Write the version to a resource file so the GitHookTask can use it in the hook content
     */
    val writeVersion by registering {
        doLast {
            // Build version path from project group & name
            val versionPath = versionDir(project)
                .let { basePath ->
                    project.group.toString()
                        .split(".")
                        .fold(basePath) { path, component -> path.resolve(component) }
                }
                .resolve(project.rootProject.name)
                .resolve("version.properties")
            versionPath.parent.toFile().mkdirs()
            Files.newBufferedWriter(versionPath).use {
                it.write("version = $version\n")
            }
        }
    }

    // Write version before processing resources
    processResources {
        dependsOn(writeVersion)
    }
}

sourceSets {
    main {
        // Add version dir to resources
        resources.srcDir(versionDir(project))
    }
}

/**
 * Dir to put version file in
 */
fun versionDir(project: Project) =
    project.buildDir.toPath()
        .resolve("generated-resources")
        .resolve("version")
