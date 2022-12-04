plugins {
    kotlin("jvm") version "1.7.22"
    id("org.jmailen.kotlinter")
}

kotlinter {
    baseline = "config/baseline.xml"
}
