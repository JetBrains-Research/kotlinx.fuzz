import com.saveourtool.diktat.plugin.gradle.DiktatExtension
import com.saveourtool.diktat.plugin.gradle.DiktatGradlePlugin

plugins {
    id("org.plan.research.kotlinx-fuzz-module")
    id("com.saveourtool.diktat") version "2.0.0" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        reporters {
            plain()
            sarif()
        }
        inputs {
            exclude {
                val examplesDir = project(":kotlinx.fuzz.examples").projectDir.toPath().normalize()
                it.file.toPath().startsWith(examplesDir)
            }
            include("src/**/*.kt")
        }
        debug = true
    }
}
