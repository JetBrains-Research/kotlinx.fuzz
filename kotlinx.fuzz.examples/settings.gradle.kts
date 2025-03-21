pluginManagement {
    includeBuild("..")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}


include("kotlinx.serialization")
include("kotlinx.html")
include("kotlinx.io")
include("kotlinx.cli")
include("kotlinx.collections.immutable")
include("kotlinx.datetime")
