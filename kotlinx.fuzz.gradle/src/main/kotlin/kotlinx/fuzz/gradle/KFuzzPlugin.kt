package kotlinx.fuzz.gradle

import kotlinx.fuzz.FuzzConfig
import kotlinx.fuzz.FuzzConfig.Companion.toPropertiesMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import kotlin.properties.Delegates

abstract class KFuzzPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<FuzzTask>("fuzz") {
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz-test")
            }
        }
    }

    fun Project.fuzzConfig(block: FuzzConfigBuilder.() -> Unit) {
        val properties = FuzzConfigBuilder.build(block).toPropertiesMap()
        tasks.named<FuzzTask>("fuzz") {
            systemProperties(properties)
        }
    }
}

abstract class FuzzTask : Test() {
    @TaskAction
    fun action() {
        println("Invoking FuzzTask")
    }
}

class FuzzConfigBuilder private constructor() {
    var fuzzEngine: String = FuzzConfig.FUZZ_ENGINE_DEFAULT
    var hooks: Boolean = FuzzConfig.HOOKS_DEFAULT
    var keepGoing: Int = FuzzConfig.KEEP_GOING_DEFAULT
    lateinit var instrument: List<String>
    var customHookExcludes: List<String> = FuzzConfig.CUSTOM_HOOK_EXCLUDES_DEFAULT
    var maxSingleTargetFuzzTime: Int by Delegates.notNull<Int>()

    fun build(): FuzzConfig = FuzzConfig(
        fuzzEngine = fuzzEngine,
        hooks = hooks,
        keepGoing = keepGoing,
        instrument = instrument,
        customHookExcludes = customHookExcludes,
        maxSingleTargetFuzzTime = maxSingleTargetFuzzTime
    )

    companion object {
        internal fun build(block: FuzzConfigBuilder.() -> Unit): FuzzConfig =
            FuzzConfigBuilder().apply(block).build()

        internal fun writeToSystemProperties(block: FuzzConfigBuilder.() -> Unit) {
            build(block).toPropertiesMap().forEach { (key, value) ->
                System.setProperty(key, value)
            }
        }
    }
}
