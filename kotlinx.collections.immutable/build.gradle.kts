plugins {
    kotlin("jvm") version "2.0.10"
    jacoco
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    val jazzerVersion = "0.22.1"
    testImplementation("com.code-intelligence:jazzer-api:${jazzerVersion}")
    testImplementation("com.code-intelligence:jazzer-junit:${jazzerVersion}")

    testImplementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    maxHeapSize = "${1024 * 4}m"
    val jacocoDest = properties["jacocoDest"]
    configure<JacocoTaskExtension> {
        includes = listOf("kotlinx.collections.immutable.**")
        destinationFile = layout.buildDirectory.file("jacoco/$jacocoDest.exec").get().asFile
    }
}

kotlin {
    jvmToolchain(17)
}
