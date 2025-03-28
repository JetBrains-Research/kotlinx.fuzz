plugins {
    id("kotlinx.fuzz.example-module")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.kotlinx.cli)
    implementation(libs.commons.cli)
}
