pluginManagement {
    includeBuild("..")
    repositories {
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    }
}
