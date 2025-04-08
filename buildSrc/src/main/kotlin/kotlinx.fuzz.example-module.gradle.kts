import kotlinx.fuzz.booleanProperty
import kotlinx.fuzz.task.target.CheckTargetsExist
import kotlinx.fuzz.task.target.PrintTargetNames
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

plugins {
    kotlin("jvm")
}

group = GROUP_ID
version = VERSION

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

    // set up Jazzer options
    environment(mapOf("JAZZER_FUZZ" to "1"))
    maxHeapSize = "${1024 * 4}m"
    val jacocoAgent = Path(System.getenv("KFUZZ_JACOCO")).absolutePathString()
    val testName = System.getenv("KFUZZ_TEST_NAME")
    val execsDir = System.getenv("KFUZZ_D")
    val includePaths = System.getenv("KFUZZ_I")
    jvmArgs(
        "-Xss1g",
        "-XX:+UseParallelGC",
        "-javaagent:${jacocoAgent}=destfile=$execsDir/$testName.exec,dumponexit=true,output=file,jmx=false,includes=$includePaths"
    )
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
