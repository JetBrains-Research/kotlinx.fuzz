# Fuzzing Configuration

There are three ways of specifying configurations for fuzzing: DSL in `build.gradle.kts`, annotations or project properties.

## DSL in `build.gradle.kts`
You need to add `fuzzConfig` block to you build file. E.g.
```kotlin
fuzzConfig {
     workDir = Path("fuzz-workdir")
     maxFuzzTimePerTarget = 1.hour
     coverage {
         reportTypes = setOf(CoverageReportType.HTML)
     }
    engine {
        libFuzzerRssLimit = 2048
    }
}
```

## Annotations
Each test has `KFuzzTest` annotation that can be used to pass some configurations (or all of them). E.g.: `@KFuzzTest(maxFuzzTime = "10s")`

## Project Properties
You need to specify properties that you want by using project properties, e.g.: `gradle fuzz -Pkotlinx.fuzz.maxFuzzTimePerTarget=10s`

## Priorities
You can use all of these approaches at the same time even overriding the same configurations. Priorities (from the highest to the lowest) are: annotations, `fuzzConfig`, project properties, default values

## Description of properties
* `workDir` - Working directory for internal fuzzing files.
* `reproducerDir` - Directory for crash reproducers.
* `instrument` - Which packages to instrument. Instrumentation is the process of modifying a program’s bytecode at runtime to monitor its execution, guide the fuzzer and enable code coverage tracking
* `customHookExcludes` - In which packages NOT to apply custom hooks.
* `hooks` - Whether to apply custom hooks (currently unsupported).
* `logLevel` - Sets the logging level for `kotlinx.fuzz` library. Supported levels (ordered by fatality): `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`.
* `detailedLogging` - Forwards logs to standard output from fuzzing engine.
* `threads` - How many threads to use for parallel fuzzing.
* `supportJazzerTargets` - Whether to fuzz jazzer targets together with `kotlinx.fuzz` ones.
* `reportTypes` - Which reports to generate. Supported types: `HTML`, `XML`, `CSV`.
* `includeDependencies` - Which dependencies to calculate coverage for.
* `maxFuzzTime` - Max time to fuzz each `@KFuzzTest`.
* `keepGoing` - How many crashes to find before stopping fuzzing, or `0` for unlimited.
* `dumpCoverage` - Whether you want to have coverage collected after your run or not
* `libFuzzerRssLimitMb` - Memory usage limit in Mb for libfuzzer. `0` stays for no limit. Find more info [here](https://llvm.org/docs/LibFuzzer.html)
* `reproducerType` - Type of reproducer to generate. Supported types: `LIST_BASED_NO_INLINE`, `LIST_BASED_INLINE`. Find more info [here](Crash%20reproduction.md). 

## Summary
|      Config Name       | Can be set in annotations | DSL section  |                Project Property                |                      Default Value                       |
|:----------------------:|:-------------------------:|:------------:|:----------------------------------------------:|:--------------------------------------------------------:|
|       `workDir`        |             ❌             | `fuzzConfig` |             `kotlinx.fuzz.workDir`             |                 `<build directory>/fuzz`                 |             
|    `reproducerDir`     |             ❌             | `fuzzConfig` |          `kotlinx.fuzz.reproducerDir`          |           `<build directory>/fuzz/reproducers`           |             
|      `instrument`      |            ✔️             | `fuzzConfig` |           `kotlinx.fuzz.instrument`            |                     Doesn't have one                     |             
|  `customHookExcludes`  |            ✔️             | `fuzzConfig` |       `kotlinx.fuzz.customHookExcludes`        |                     Doesn't have one                     |             
|        `hooks`         |             ❌             | `fuzzConfig` |              `kotlinx.fuzz.hooks`              |                          `true`                          |             
|       `logLevel`       |             ❌             | `fuzzConfig` |            `kotlinx.fuzz.logLevel`             |                          `WARN`                          |             
|   `detailedLogging`    |             ❌             | `fuzzConfig` |         `kotlinx.fuzz.detailedLogging`         |                         `false`                          |             
|       `threads`        |             ❌             | `fuzzConfig` |             `kotlinx.fuzz.threads`             | `max(1, Runtime.getRuntime().availableProcessors() / 2)` |             
| `supportJazzerTargets` |             ❌             | `fuzzConfig` |      `kotlinx.fuzz.supportJazzerTargets`       |                         `false`                          |             
|     `reportTypes`      |             ❌             |  `coverage`  |      `kotlinx.fuzz.coverage.reportTypes`       |             `setOf(CoverageReportType.HTML)`             |             
| `includeDependencies`  |             ❌             |  `coverage`  |  `kotlinx.fuzz.coverage.includeDependencies`   |                       `emptySet()`                       |             
|     `maxFuzzTime`      |            ✔️             | `fuzzConfig` |           `kotlinx.fuzz.maxFuzzTime`           |                           `1m`                           |             
|      `keepGoing`       |            ✔️             | `fuzzConfig` |            `kotlinx.fuzz.keepGoing`            |                           `0`                            |             
|     `dumpCoverage`     |            ✔️             | `fuzzConfig` |          `kotlinx.fuzz.dumpCoverage`           |                          `true`                          |             
| `libFuzzerRssLimitMb`  |             ❌             |   `engine`   | `kotlinx.fuzz.jazzer.libFuzzerArgs.rssLimitMb` |                           `0`                            |            
|    `reproducerType`    |             ❌             | `fuzzConfig` |         `kotlinx.fuzz.reproducerType`          |            `ReproducerType.LIST_BASED_INLINE`            |            
