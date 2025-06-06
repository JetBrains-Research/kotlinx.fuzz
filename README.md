# kotlinx.fuzz

`kotlinx.fuzz` is a general purpose fuzzing library for Kotlin. The library provides basic functionality:

* Simple API for writing fuzz tests
* Gradle plugin that provides an easy way of configuring the fuzzer, running it, and generating reports
* Custom JUnit engine that handles interactions with the fuzzing engine and allows for easy integration with IDE
* Integration with Jazzer as the main fuzzing engine for now

## Why fuzzing?

Fuzz testing is a powerful technique capable of exposing undetected errors and vulnerabilities. Research has shown it to be highly effective in detecting critical software flaws. For example, Google’s [OSS-Fuzz](https://github.com/google/oss-fuzz) has discovered over 40,000 bugs in open-source projects.

Despite its success in other ecosystems, fuzzing has not seen wide adoption in Kotlin development. Our goal with **kotlinx.fuzz** is to bridge this gap by introducing an efficient and scalable fuzzing framework specifically designed for Kotlin projects.

## Requirements

Currently, `kotlinx.fuzz` works only for JVM and requires JDK 8. Also, the library is built using Kotlin version 2.0.21, which adds additional requirements. This is a subject to change in the future.

## Simple setup

Here we are going to give you a simple instruction on how to configure and run fuzzer in your project. If you want more detailed instructions, check out our [How to get started](docs/How%20to%20get%20started.md) guide.

1. Add PLAN lab maven repository and kotlin repository to your gradle config:

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        maven(url = "https://plan-maven.apal-research.com")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    }
}
```

`build.gradle.kts`:
```kotlin
repositories {
    maven(url = "https://plan-maven.apal-research.com")
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}
```

2. Add `kotlinx.fuzz` as a dependency:
```kotlin
dependencies {
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer:1.0.0")
}
```

3. Apply `kotlinx.fuzz` plugin to your project:
```kotlin
plugins {
    id("org.jetbrains.kotlinx.fuzz") version "1.0.0"
}
```

4. Configure plugin:
```kotlin
fuzzConfig {
    instrument = listOf("org.example.**")
    maxFuzzTimePerTarget = 10.minutes
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
    }
}
```

5. Write your fuzz tests:
```kotlin
package org.example

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

object ExampleTest {
    @KFuzzTest
    fun foo(data: KFuzzer) {
        if (data.int() % 2 == 0) {
            if (data.int() % 3 == 2) {
                if (data.int() % 31 == 11) {
                    throw IllegalArgumentException()
                }
            }
        }
    }
}
```

6. Run fuzzer:
```bash
~/example » ./gradlew fuzz                                                                                                                                                  1 ↵

> Task fuzz

SampleTarget > public final void org.example.ExampleTest.foo(kotlinx.fuzz.KFuzzer) FAILED
    java.lang.IllegalArgumentException
        at org.example.ExampleTest.foo(ExampleTest.kt:12)
```

7. Check the fuzzing report in `build/fuzz`

You can see more examples of `kotlinz.fuzz` usage in [`kotlinx.fuzz.test`](kotlinx.fuzz.test)

## Modes

Currently `kotlinx.fuzz` can run fuzzer in two different modes:

* **Fuzzing** mode &mdash; in this mode `kotlinx.fuzz` will use the fuzzing engine to generate new seeds that explore the program under test, achieve the highest possible coverage and find new crashes. You can run this mode using `./gradlew fuzz` command.
* **Regression** mode &mdash; in this mode `kotlinx.fuzz` does not generate new crashes, it runs your fuzz tests on the seeds that previously triggered crashes. You can run this mode using `./gradlew regression` command. If there are no previously found crashes, running this task will cause the following error:
```
No matching tests found in any candidate test task.
    Requested tests:
        Test pattern *your test name* which fails in task :regression
```

Both of these tasks extend built-in Gradle `test` task, so you can provide additional parameters or running individual fuzz tests using the same Gradle arguments.

`kotlinx.fuzz` test engine also provides integration with IDEs and allows you to run and debug your fuzz tests both in fuzz and regression mode.

![image](docs/img/ide-tests.png)

## Report structure

`kotlinx.fuzz` will generate `build/fuzz` folder with the fuzzing campaign report. The general structure of the report is following:

* `corpus` folder contains seeds generated by fuzz engine during the fuzzing campaign.
* `coverage` folder contains binary JaCoCo `.exec` files with coverage for each individual fuzz test.
* `jacoco-report` provides a JaCoCo coverage report in human-readable formats: HTML, CSV and XML.
* `logs` contains fuzz engine logs for each of the fuzz tests.
* `reproducers` folder contains crash seeds found by the fuzz engine and reproducers of them. This folder is used by `kotlinx.fuzz` during `regression` mode. Each `crash-$hash` file is a unique crash found by the fuzz engine. Additionally, for each crash seed you can find `stacktrace-$hash` file that contains a stack trace of the found crash and `cluster-$hash` folder that contains all the other seeds that reproduce the same failure (you can read about in more in [Crash deduplication](docs/Crash%20deduplication.md)). You can find `reproducer-$hash.kt` as well in this directory. This files contain readable version of tests ran on specific inputs that lead to a crash. To run them you can just put them in the `test` directory and run as an ordinary test for your building system (you can read about in more in [Crash reproduction](docs/Crash%20reproduction.md)).  
* `stats` contains fuzz engine's CSV reports for each individual fuzz test. CSV files contain information about number of executions, number of found crashes, coverage, etc.
* `merged-coverage.exec` &mdash; JaCoCo binary file with the merged coverage of all the fuzz tests (this binary file is used to generate `jacoco-report` folder).
* `overall-stats.csv` &mdash; final CSV report of Jazzer for each of the fuzz tests.

## Configuration options

The plugin currently allows you to configure its parameters in the `fuzzConfig` section in `build.gradle.kts`. Here are the main configuration options:
* `fuzzEngine` &mdash; fuzz engine to use, currently only `"Jazzer"` is supported
* `instrument` &mdash; glob patterns matching names of classes that should be instrumented for fuzzing, it determines what classes fuzzer will target during fuzzing; ideally it should be a class or a package that you want to fuzz
* `workDir` &mdash; directory where the all fuzzing results will be stored; default `"build/fuzz"`
* `dumpCoverage` &mdash; flag to enable/disable JaCoCo `.exec` file generation, enabled by default; you can read a little bit more about logging in `kotlinx.fuzz` [here](docs/Logging.md)
* `logLevel` &mdash; logging level enabled for `kotlinx.fuzz`; `warn` by default
* `maxSingleTargetFuzzTime` &mdash; max time to fuzz a single target; default 1 minute
* `reproducerPath` &mdash; path to store reproducers; default `"$workDir/reproducers"`

These and some other options can also be set through the system properties. You can check system property names [here](docs/Configuration.md).

Design, implementation and default values of configuration properties are subject to change in the future releases.

### Using custom hooks

The plugin supports Jazzer's custom hooks. They can be used, for example, to mock certain methods (e.g. make `Random.nextInt()` deterministic), or to add assertions before or after sensitive methods (e.g. verify that a new process does not execute `rm -rf /`).

To apply a custom hook:

1. Add a dependency:

```kotlin
testImplementation("org.jetbrains:kotlinx.fuzz.jazzer")
```

2. Write your hook. See the [Jazzer hooks](https://github.com/CodeIntelligenceTesting/jazzer/blob/main/docs/advanced.md#custom-hooks) reference, or [example hook](./kotlinx.fuzz.test/src/test/kotlin/kotlinx/fuzz/test/hooks/Hooks.kt) in this project tests. Note that a hook written in Kotlin must have the `@JvmStatic` annotation.

3. And that's it! Hooks will be applied to the packages specified in `instrument` config parameter. If you want to exclude some packages from instrumentation, use the `customHooksExcludes` parameter.

## Differences from Jazzer

`kotlinx.fuzz` uses Jazzer as the main fuzzing engine, but also introduces several new key features:

* Improved and simplified API
* Gradle plugin that integrates all the fuzzing-related tasks into your build system
  * Easy configuration
  * Verification of configuration options
* Improved crash deduplication algorithm
* New reproducer generation approach
* Improved regression mode

## Trophy list

Trophy list can be found [here](docs/Trophy%20list.md)
