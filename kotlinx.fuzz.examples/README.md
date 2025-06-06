# kotlinx.fuzz
Fuzzer for Kotlin libraries

The tests are disabled by default, so that they don't take too much time when building the project. They can be enabled by defining the `enableTests` property:
```
./gradlew :kotlinx.fuzz.examples:kotlinx.serialization:test -PenableTests=true
```

# Setup instructions

1. Create a new submodule for your library. Add it into [`settings.gradle.kts`](../settings.gradle.kts):
```kotlin
include("my-module")
```
2. Use following `build.gradle.kts` template:
```kotlin
plugins {
    id("org.plan.research.kotlinx-fuzz-submodule")
}

dependencies {
    implementation("your.target.library:here:1.0.0")
}
```

You don't need to change anything else.
Our `org.plan.research.kotlinx-fuzz-submodule` plugin already declares everything necessary:
* jazzer dependency (version `0.22.1`)
* jazzer environment variables (mainly `JAZZER_FUZZ`)
* JUnit options
* `copyDependencies` task: before executing tests, Gradle will copy all your dependencies into `my-module/build/dependencies` folder

When running the [`run-experiment`](scripts/run-experiment) script you can provide jars from `my-module/build/dependencies` as `--classfile` options for JaCoCo coverage computation.

3. Create a `junit-platform.properties` file in `my-module/src/test/resources/` and specify additional Jazzer options. Example:
```
jazzer.instrumentation_includes=your.targe.library.**
jazzer.custom_hook_includes=your.targe.library.**
jazzer.jazzer_fuzz=1
jazzer.fuzz=1
jazzer.keep_going=9999
```