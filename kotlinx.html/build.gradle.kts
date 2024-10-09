plugins {
    kotlin("jvm") version "2.0.10"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    testImplementation("com.code-intelligence:jazzer-api:0.0.0-dev")
    testImplementation("com.code-intelligence:jazzer-junit:0.0.0-dev")

    testImplementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")


    val kotlinxHtmlVersion = "0.11.0"
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    maxHeapSize = "${1024 * 4}m"
}

kotlin {
    jvmToolchain(17)
}
