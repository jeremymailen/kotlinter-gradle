plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

dependencies {
    ktlint(project(":rules"))
}

kotlinter {
    ignoreLintFailures = true
}
