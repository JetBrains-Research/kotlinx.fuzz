plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.4.0"
    kotlin("plugin.serialization") version "2.0.21"
}

val kotlinxRpcVersion = "0.4.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:$kotlinxRpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:$kotlinxRpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:$kotlinxRpcVersion")

    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")

    implementation(kotlin("reflect"))
}
