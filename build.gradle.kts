import kotlinx.fuzz.configureDiktat

plugins {
    id("kotlinx.fuzz.src-module")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    configureDiktat()
}
