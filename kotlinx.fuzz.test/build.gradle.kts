plugins {
    kotlin("jvm") version "2.0.20"
    id("kotlinx.fuzz")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}