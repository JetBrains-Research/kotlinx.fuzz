plugins {
    id("kotlinx.fuzz.example-module")
}


dependencies {
    testImplementation(kotlin("reflect"))
    implementation(libs.jazzer.api)
    implementation(libs.jazzer.junit)
    implementation(libs.reflections)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.html.jvm)
}
