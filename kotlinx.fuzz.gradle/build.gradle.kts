plugins {
    id("kotlinx.fuzz.src-module")
    `kotlin-dsl`
    libs.plugins.gradle.publish
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))

    gradleApi()
    implementation(kotlin("reflect"))
    implementation(libs.junit.platform.engine)

    testImplementation(libs.junit.platform.testkit)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(project(":kotlinx.fuzz.jazzer"))
}

gradlePlugin {
    // TODO
    website = "https://jetbrains.com/404"
    vcsUrl = "https://github.com/JetBrains-Research/kotlinx.fuzz.git"
    plugins {
        create("kotlinx.fuzz") {
            id = "kotlinx.fuzz"
            displayName = "kotlinx.fuzz Gradle plugin"
            description = "Gradle plugin for using kotlinx.fuzz"
            tags = listOf("testing", "fuzzing")
            implementationClass = "kotlinx.fuzz.gradle.KFuzzPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}
