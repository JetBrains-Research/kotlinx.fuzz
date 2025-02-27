plugins {
    id("kotlinx.fuzz.src-module")
    `kotlin-dsl`
    `maven-publish`
//    alias(libs.plugins.gradle.publish)
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))

    implementation(kotlin("reflect"))
    implementation(libs.jacoco.core)
    implementation(libs.jacoco.report)

    testImplementation(libs.junit.jupiter)
}

gradlePlugin {
    website = "https://github.com/JetBrains-Research/kotlinx.fuzz"
    vcsUrl = "https://github.com/JetBrains-Research/kotlinx.fuzz.git"
    plugins {
        create("kotlinx.fuzz.gradle") {
            id = project.name
            displayName = "kotlinx.fuzz Gradle plugin"
            description = "Gradle plugin for using kotlinx.fuzz"
            tags = listOf("testing", "fuzzing")
            implementationClass = "kotlinx.fuzz.gradle.KFuzzPlugin"
        }
    }
}

/**
 * We need custom publishing setup here, as `gradlePlugin` automatically configures maven publication to include
 * necessary components
 */
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
        repositories {
            maven {
                url = uri("https://maven.pkg.github.com/plan-research/kotlin-maven")
                credentials {
                    username = project.findProperty("gpr.user")?.toString()
                        ?: System.getenv("MAVEN_REPOSITORY_LOGIN")
                    password = project.findProperty("gpr.token")?.toString()
                        ?: System.getenv("MAVEN_REPOSITORY_PASSWORD")
                }
            }
        }
    }
}
