plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.reflect.lite:1.1.0")
}

kotlin {
    explicitApi()
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("ContextReceivers")
        }
    }
}