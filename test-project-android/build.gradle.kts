plugins {
    kotlin("android") version "1.6.20"
    id("com.android.library")
    id("org.jmailen.kotlinter")
}

android {
    compileSdkVersion(31)
}

dependencies {
    ktlintRuleSet(project(":test-custom-rules"))
}
