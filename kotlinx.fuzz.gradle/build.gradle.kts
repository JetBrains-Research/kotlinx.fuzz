plugins {
    id("org.plan.research.kotlinx-fuzz-module")
    `kotlin-dsl`
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

gradlePlugin.plugins.create("kotlinx.fuzz") {
    id = "kotlinx.fuzz-gradle"
    implementationClass = "org.planx.fuzzing.plugin.KFuzzPlugin"
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}