import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin
    id("com.gradle.plugin-publish") version Versions.pluginPublish
    `java-gradle-plugin`
    `maven-publish`
    id("org.jmailen.kotlinter") version Versions.kotlinter
    `idea`
}

repositories {
    jcenter()
    google()
}

val pluginId = "org.jmailen.kotlinter"
val githubUrl ="https://github.com/jeremymailen/kotlinter-gradle"
val webUrl = "https://github.com/jeremymailen/kotlinter-gradle"
val projectDescription = "Lint and formatting for Kotlin using ktlint with configuration-free setup on JVM and Android projects"

version = "2.3.0"
group = "org.jmailen.gradle"
description = projectDescription

dependencies {
    implementation("com.pinterest.ktlint:ktlint-core:${Versions.ktlint}")
    implementation("com.pinterest.ktlint:ktlint-reporter-checkstyle:${Versions.ktlint}")
    implementation("com.pinterest.ktlint:ktlint-reporter-json:${Versions.ktlint}")
    implementation("com.pinterest.ktlint:ktlint-reporter-html:${Versions.ktlint}")
    implementation("com.pinterest.ktlint:ktlint-reporter-plain:${Versions.ktlint}")
    implementation("com.pinterest.ktlint:ktlint-ruleset-experimental:${Versions.ktlint}")
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:${Versions.ktlint}")

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:${Versions.androidTools}")

    testImplementation("junit:junit:${Versions.junit}")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}")
    testImplementation("org.jetbrains:annotations:${Versions.jetbrainsAnnotations}")
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles javaDoc JAR"
    classifier = "javadoc"
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
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    wrapper {
        gradleVersion = "6.0.1"
    }
}
