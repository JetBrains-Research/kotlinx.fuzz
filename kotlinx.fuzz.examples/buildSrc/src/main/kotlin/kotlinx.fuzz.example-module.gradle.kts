plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven(url = "https://plan-maven.apal-research.com")
}

dependencies {
    testImplementation(kotlin("test"))
}


kotlin {
    jvmToolchain(11)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}
