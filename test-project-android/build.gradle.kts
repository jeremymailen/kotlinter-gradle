plugins {
    kotlin("android") version "1.7.10"
    id("com.android.library")
    id("org.jmailen.kotlinter")
}

android {
    compileSdkVersion(31)

    if (properties["agpVersion"].toString().startsWith("4")) {
        sourceSets {
            named("main") { java.srcDirs("src/main/kotlin") }
            named("test") { java.srcDirs("src/test/kotlin") }
        }
    }
}
