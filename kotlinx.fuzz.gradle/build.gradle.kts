plugins {
    id("org.plan.research.kotlinx-fuzz-module")
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))

    gradleApi()
    implementation(kotlin("reflect"))
    implementation("org.junit.platform:junit-platform-engine:$JUNIT_PLATFORM_VERSION")

    testImplementation("org.junit.platform:junit-platform-testkit:$JUNIT_PLATFORM_VERSION")
    testImplementation("org.junit.jupiter:junit-jupiter:$JUNIT_JUPITER_VERSION")
    testImplementation("com.code-intelligence:jazzer-api:$JAZZER_VERSION")
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
            implementationClass = "kotlinx.fuzz.gradle.MainPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}