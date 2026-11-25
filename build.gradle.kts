import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
    idea
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlinter)
}

repositories {
    mavenCentral()
    google()
}

val pluginId = "org.jmailen.kotlinter"
val githubUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val webUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val projectDescription = "Lint and formatting for Kotlin using ktlint with configuration-free setup on JVM and Android projects"
val targetJavaVersion = JavaVersion.VERSION_17

version = "5.7.0"
group = "org.jmailen.gradle"
description = projectDescription

configurations {
    register("testRuntimeDependencies") {
        extendsFrom(compileOnly.get())
        exclude(group = "io.github.ktlint.core")
        attributes {
            // KGP publishes multiple variants https://kotlinlang.org/docs/whatsnew17.html#support-for-gradle-plugin-variants
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        }
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly(libs.android.tools.gradle.api)

    compileOnly(libs.bundles.ktlint.engine)
    compileOnly(libs.bundles.ktlint.reporters)
    compileOnly(libs.bundles.ktlint.rulesets)
    compileOnly(libs.ktlint.compatibility)

    testImplementation(libs.commons.io)
    testImplementation(libs.mockito.kotlin)

    testImplementation(libs.bundles.ktlint.engine)
    testImplementation(libs.bundles.ktlint.reporters)
    testImplementation(libs.bundles.ktlint.rulesets)
    testImplementation(libs.ktlint.compatibility)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.platform)
}

if (GradleVersion.current() >= GradleVersion.version("9.1")) {
    kotlin {
        jvmToolchain(25)
    }
}

tasks {
    val generateKotlinterProperties = register("generateKotlinterProperties") {
        val projectVersion = version
        val ktlintVersion = libs.versions.ktlint.get()
        val propertiesFile = File(sourceSets.main.get().output.resourcesDir, "kotlinter.properties")
        inputs.property("projectVersion", projectVersion)
        inputs.property("ktlintVersion", ktlintVersion)
        outputs.file(propertiesFile)

        doLast {
            propertiesFile.writeText(
                """
                version = $projectVersion
                ktlintVersion = $ktlintVersion
                """.trimIndent(),
            )
        }
    }

    processResources {
        dependsOn(generateKotlinterProperties)
    }

    withType<JavaCompile>().configureEach {
        options.release.set(targetJavaVersion.majorVersion.toInt())
    }
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.majorVersion))
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
        gradleVersion = "9.5.1"
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
    sourceCompatibility = targetJavaVersion
    targetCompatibility = targetJavaVersion
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
