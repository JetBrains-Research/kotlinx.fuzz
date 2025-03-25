import kotlinx.fuzz.booleanProperty
import kotlinx.fuzz.task.target.CheckTargetsExist
import kotlinx.fuzz.task.target.PrintTargetNames

plugins {
    kotlin("jvm")
}

group = GROUP_ID
version = VERSION

repositories {
    mavenCentral()
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

    // set up Jazzer options
    environment(mapOf("JAZZER_FUZZ" to "1"))
    maxHeapSize = "${1024 * 4}m"
    jvmArgs("-Xss1g", "-XX:+UseParallelGC")
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath).into("${project.layout.buildDirectory.get()}/dependencies")
}

tasks.register<PrintTargetNames>("printTargetNames") {
    dependsOn("compileTestKotlin")
    classpathDir.set(kotlin.sourceSets.test.get().kotlin.destinationDirectory)
    outputFile.set(layout.buildDirectory.file("targets.txt"))
}

tasks.register<CheckTargetsExist>("checkTargetsExist") {
    dependsOn("compileTestKotlin")
    classpathDir.set(kotlin.sourceSets.test.get().kotlin.destinationDirectory)
}

tasks.getByName("test").let {
    it.enabled = project.booleanProperty("enableTests") == true
    it.dependsOn("copyDependencies")
}
