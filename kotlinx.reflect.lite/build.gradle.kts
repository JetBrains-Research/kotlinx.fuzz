plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.reflect.lite:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

kotlin {
    explicitApi()
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("ContextReceivers")
        }
    }
}