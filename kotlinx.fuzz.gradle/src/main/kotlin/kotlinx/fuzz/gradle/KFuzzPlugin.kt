package kotlinx.fuzz.gradle

import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.config.KFuzzConfigBuilder
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.warn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories

private const val INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME = "idea.debugger.dispatch.port"
private const val REGRESSION_ENABLED_NAME = "kotlinx.fuzz.regressionEnabled"

private val Project.fuzzConfig: KFuzzConfig
    get() {
        val dsl = this.extensions.getByType<FuzzConfigDSL>()
        return dsl.build()
    }

@Suppress("unused")
abstract class KFuzzPlugin : Plugin<Project> {
    private val log = Logging.getLogger(KFuzzPlugin::class.java)!!

    override fun apply(project: Project) {
        val pluginVersion = "0.2.2"
        project.dependencies {
            add("testImplementation", "org.jetbrains:kotlinx.fuzz.api:$pluginVersion")
            add("testRuntimeOnly", "org.jetbrains:kotlinx.fuzz.junit:$pluginVersion")
        }

        val projectPropertiesMap: Map<String, String> = project.properties.mapValues { (_, v) -> v.toString() }
        val fuzzConfigDSL = project.extensions.create<FuzzConfigDSL>("fuzzConfig", projectPropertiesMap)
        project.preconfigureFuzzConfigDSL(fuzzConfigDSL)

        project.tasks.withType<Test>().configureEach {
            configureLogging()

            if (this is FuzzTask || this is RegressionTask) {
                return@configureEach
            }

            useJUnitPlatform {
                excludeEngines("kotlinx.fuzz")
            }
        }

        val (defaultCP, defaultTCD) = project.defaultTestParameters()
        project.registerFuzzTask(defaultCP, defaultTCD)
        project.registerRegressionTask(defaultCP, defaultTCD)
    }

    private fun Project.preconfigureFuzzConfigDSL(dsl: FuzzConfigDSL) {
        val buildDir = layout.buildDirectory.get()
        val defaultWorkDir = buildDir.dir("fuzz").asFile.toPath()
        dsl.workDir = defaultWorkDir
        dsl.reproducerDir = defaultWorkDir.resolve("reproducers")
    }

    private fun Project.registerFuzzTask(defaultCP: FileCollection, defaultTCD: FileCollection) {
        project.tasks.register<FuzzTask>("fuzz") {
            classpath = defaultCP
            testClassesDirs = defaultTCD
            outputs.upToDateWhen { false }  // so the task will run on every invocation

            fuzzConfig = this@registerFuzzTask.fuzzConfig
            mainSourceSet = this@registerFuzzTask.extensions.getByType<SourceSetContainer>()["main"]
            runtimeClasspathConfiguration = this@registerFuzzTask.configurations["runtimeClasspath"]

            doFirst {
                systemProperties(fuzzConfig.toPropertiesMap())
                systemProperties[INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME] = System.getProperty(INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME)
            }
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz")
            }
        }
    }

    private fun Project.registerRegressionTask(defaultCP: FileCollection, defaultTCD: FileCollection) {
        project.tasks.register<RegressionTask>("regression") {
            classpath = defaultCP
            testClassesDirs = defaultTCD
            outputs.upToDateWhen { false }
            doFirst {
                val regressionConfig = KFuzzConfigBuilder.fromAnotherConfig(fuzzConfig).build()
                systemProperties(regressionConfig.toPropertiesMap())
                systemProperties[INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME] = System.getProperty(INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME)
                systemProperties[REGRESSION_ENABLED_NAME] = System.getProperty(REGRESSION_ENABLED_NAME)
            }
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz")
            }
        }
    }

    private fun Test.configureLogging() {
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }
    }

    /**
     * Finds the default values of classpath and testClassesDir properties of test tasks.
     * Required to fix https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_task_default_classpath for fuzz tests.
     *
     * @return the default values of classpath and testClassesDir properties of test tasks
     */
    private fun Project.defaultTestParameters(): Pair<FileCollection, FileCollection> =
        tasks.withType<Test>()
            .filterNot { it is FuzzTask }
            .map {
                log.debug("Reusing the classpath of the task '${it.name}' for fuzz tests")
                it.classpath to it.testClassesDirs
            }
            .singleOrNull()
            ?: run {
                log.warn("'fuzz' and 'regression' task was not able to inherit the 'classpath' and 'testClassesDirs' properties, as it found conflicting configurations")
                log.warn("Please, specify them manually in your gradle config using the following syntax:")
                log.warn(
                    """
                    tasks.withType<FuzzTask>().configureEach {
                        classpath = TODO()
                        testClassesDirs = TODO()
                    }
                    """.trimIndent(),
                )
                log.warn(
                    """
                    tasks.withType<RegressionTask>().configureEach {
                        classpath = TODO()
                        testClassesDirs = TODO()
                    }
                    """.trimIndent(),
                )
                project.files() to project.files()
            }
}

abstract class FuzzTask : Test() {
    private val log = LoggerFacade.getLogger<FuzzTask>()

    @Option(
        option = "fullClasspathReport",
        description = "Report on the whole classpath (not just the project classes).",
    )
    @get:Input
    var reportWithAllClasspath: Boolean = false

    @get:Internal
    var runtimeClasspathConfiguration: Configuration? = null

    @get:Internal
    lateinit var fuzzConfig: KFuzzConfig

    @get:Internal
    lateinit var mainSourceSet: SourceSet

    init {
        description = "Runs fuzzing"
        group = "verification"
    }

    @TaskAction
    fun action() {
        overallStats()
        if (fuzzConfig.target.dumpCoverage) {
            val workDir = fuzzConfig.global.workDir

            val coverageMerged = workDir.resolve("merged-coverage.exec")
            jacocoMerge(workDir.resolve("coverage"), coverageMerged)

            jacocoReport(coverageMerged, workDir)
        }
    }

    private fun overallStats() {
        val workDir = fuzzConfig.global.workDir
        overallStats(workDir.resolve("stats"), workDir.resolve("overall-stats.csv"))
    }

    private fun jacocoReport(execFile: Path, workDir: Path) {
        val extraDeps = getDependencies(fuzzConfig.coverage.includeDependencies)
        val runtimeClasspath = runtimeClasspathConfiguration?.files ?: emptySet()

        val projectClasspath = mainSourceSet.output.files
        val sourceDirectories = mainSourceSet.allSource.sourceDirectories.files

        val jacocoClassPath =
            projectClasspath + extraDeps + if (reportWithAllClasspath) runtimeClasspath else emptySet()

        jacocoReport(
            execFile = execFile,
            classPath = jacocoClassPath,
            sourceDirectories = sourceDirectories,
            reportDir = workDir.resolve("jacoco-report").createDirectories(),
            reports = fuzzConfig.coverage.reportTypes,
        )
    }

    private fun getDependencies(dependencies: Set<String>): Set<File> {
        val configuration = runtimeClasspathConfiguration ?: run {
            log.warn { "No 'runtimeClasspath' configuration found, skipping jacoco report generation" }
            return emptySet()
        }

        val deps = configuration.resolvedConfiguration.resolvedArtifacts.associate {
            "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" to it.file.absoluteFile
        }
        return dependencies.mapNotNull { dependency ->
            deps[dependency] ?: run {
                log.warn { "Dependency '$dependency' not found in the classpath while generating jacoco report" }
                null
            }
        }.toSet()
    }
}

abstract class RegressionTask : Test() {
    init {
        description = "Runs regression tests"
        group = "verification"
    }
}
