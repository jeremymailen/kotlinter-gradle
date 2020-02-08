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
    runtimeOnly("com.android.tools.build:gradle:3.5.3")
    runtimeOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation(testFixtures(project(":functional")))
}
