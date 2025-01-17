rootProject.name = "kotlinx.fuzz"
include("kotlinx.fuzz.api")
include("kotlinx.fuzz.jazzer")
include("kotlinx.fuzz.gradle")
includeBuild("kotlinx.fuzz.test")

// examples
include("kotlinx.fuzz.examples")
include("kotlinx.fuzz.examples:kotlinx.serialization")
include("kotlinx.fuzz.examples:kotlinx.html")
include("kotlinx.fuzz.examples:kotlinx.io")
include("kotlinx.fuzz.examples:kotlinx.cli")
include("kotlinx.fuzz.examples:kotlinx.collections.immutable")
include("kotlinx.fuzz.examples:kotlinx.datetime")
