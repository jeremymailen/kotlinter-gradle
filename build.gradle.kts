import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
    id("com.gradle.plugin-publish") version "0.10.1"
    `java-gradle-plugin`
    `maven-publish`
    id("org.jmailen.kotlinter") version "2.1.3"
    `idea`
}

repositories {
    jcenter()
    google()
}

val ktlintVers = "0.36.0"

dependencies {
    implementation("com.pinterest.ktlint:ktlint-core:$ktlintVers")
    implementation("com.pinterest.ktlint:ktlint-reporter-checkstyle:$ktlintVers")
    implementation("com.pinterest.ktlint:ktlint-reporter-json:$ktlintVers")
    implementation("com.pinterest.ktlint:ktlint-reporter-html:$ktlintVers")
    implementation("com.pinterest.ktlint:ktlint-reporter-plain:$ktlintVers")
    implementation("com.pinterest.ktlint:ktlint-ruleset-experimental:$ktlintVers")
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:$ktlintVers")

    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:3.5.3")

    testImplementation("junit:junit:4.13")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains:annotations:18.0.0")
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
