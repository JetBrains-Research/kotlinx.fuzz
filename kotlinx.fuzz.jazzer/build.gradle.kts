plugins {
    id("org.plan.research.kotlinx-fuzz-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation("com.code-intelligence:jazzer:$JAZZER_VERSION")
    implementation(kotlin("reflect"))
}
