import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
    idea
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publish)
    // Remove temporarily until 2.1.0 compatibility is released
    // alias(libs.plugins.kotlinter)
}

repositories {
    mavenCentral()
    google()
}

val pluginId = "org.jmailen.kotlinter"
val githubUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val webUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val projectDescription = "Lint and formatting for Kotlin using ktlint with configuration-free setup on JVM and Android projects"

version = "5.0.1"
group = "org.jmailen.gradle"
description = projectDescription

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
    compileOnly(libs.android.tools.gradle)

    compileOnly(libs.bundles.ktlint.engine)
    compileOnly(libs.bundles.ktlint.reporters)
    compileOnly(libs.bundles.ktlint.rulesets)

    testImplementation(libs.bundles.ktlint.engine)
    testImplementation(libs.bundles.ktlint.reporters)
    testImplementation(libs.bundles.ktlint.rulesets)
    testImplementation(libs.bundles.junit.jupiter)
    testImplementation(libs.commons.io)
    testImplementation(libs.mockito.kotlin)
}

kotlin {
    jvmToolchain(21)
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
                ktlintVersion = ${libs.versions.ktlint.get()}
                """.trimIndent()
            )
        }
    }

    processResources {
        dependsOn(generateVersionProperties)
    }

    withType<JavaCompile>().configureEach {
        options.release.set(JavaVersion.VERSION_1_8.majorVersion.toInt())
    }
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_1_8)
            languageVersion.set(KotlinVersion.KOTLIN_1_8)
            jvmTarget.set(JvmTarget.JVM_1_8)
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
        gradleVersion = "8.11.1"
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
