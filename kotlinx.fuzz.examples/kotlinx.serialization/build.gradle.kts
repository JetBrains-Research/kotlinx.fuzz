plugins {
    id("kotlinx.fuzz.example-module")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation(libs.jazzer.api)
    implementation(libs.jazzer.junit)
    implementation(libs.kotlinx.serialization.json.examples)
    implementation(libs.kotlinx.serialization.cbor.examples)
    implementation(libs.kotlinx.serialization.properties.examples)
    implementation(libs.kotlinx.serialization.protobuf.examples)
}
