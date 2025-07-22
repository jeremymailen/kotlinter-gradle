plugins {
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

dependencies {
    ktlint(project(":rules"))
}

kotlinter {
    reporters = arrayOf("plain", "checkstyle")
    ignoreLintFailures = true
    ktlintVersion = "1.6.0"
}
