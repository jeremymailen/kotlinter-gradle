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

// Required to put the Kotlin plugin on the classpath for the functional test suite
tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

version = "2.3.0"
group = "org.jmailen.gradle"
val pluginId = "org.jmailen.kotlinter"

gradlePlugin {
    plugins {
        create("kotlinterPlugin") {
            id = pluginId
            implementationClass = "org.jmailen.gradle.kotlinter.KotlinterPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/jeremymailen/kotlinter-gradle"
    vcsUrl = "https://github.com/jeremymailen/kotlinter-gradle"
    tags = listOf("kotlin", "ktlint", "lint", "format", "style", "android")

    plugins {
        named("kotlinterPlugin") {
            id = pluginId
            displayName = "Kotlin Lint plugin"
            description = "Lint and formatting for Kotlin using ktlint with configuration-free setup on JVM and Android projects"
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    from(sourceSets.main.get().allSource)
}

publishing {
    publications.withType<MavenPublication> {
        artifact(sourcesJar.get())
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
