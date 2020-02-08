plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jmailen.kotlinter")
    id("java-gradle-plugin")
}

repositories {
    jcenter()
    google()
}

dependencies {
    runtimeOnly(project(":"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation(testFixtures(project(":functional")))
}
