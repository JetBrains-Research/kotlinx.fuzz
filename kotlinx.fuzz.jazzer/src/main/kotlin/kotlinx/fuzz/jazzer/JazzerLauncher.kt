package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.fuzz.*
import kotlinx.fuzz.config.JazzerConfig
import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.deduplication.clusterCrashes
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.debug
import kotlinx.fuzz.log.error
import kotlinx.fuzz.log.info

object JazzerLauncher {
    private val log = LoggerFacade.getLogger<JazzerLauncher>()
    private val config = KFuzzConfig.fromSystemProperties()
    private val jazzerConfig = config.engine as JazzerConfig
    private var oldRepresentatives: Int? =
        null  // Number of clusters initially, so that keepGoing will depend inly on new findings
        set(value) {
            require(field == null && value != null) { "Number of old representatives should be set only once to a non-null value" }
            field = value
        }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            log.error { "Usage: <full.class.Name> <methodName>" }
            exitProcess(1)
        }
        // arg[0] - fully qualified name of the class containing fuzz target
        // arg[1] - method name of the fuzz target
        val className = args[0]
        val methodName = args[1]
        log.debug { "Running $className::$methodName" }

        val targetClass = Class.forName(className).kotlin
        val targetMethod = targetClass.memberFunctions.single { it.name == methodName }.javaMethod!!
        val instance = targetClass.objectInstance ?: targetClass.primaryConstructor!!.call()

        initJazzer()

        val result = runTarget(instance, targetMethod)
        serializeFuzzingResult(result, config.fuzzingResultPath(targetMethod))

        if (result.findingsNumber != 0) {
            log.error { "Crashes were found while fuzzing" }
            log.error(result)

            exitProcess(1)
        }
        exitProcess(0)
    }

    private fun Throwable.removeInnerFrames() {
        cause?.removeInnerFrames()

        stackTrace = stackTrace.takeWhile {
            it.className != JazzerTarget::class.qualifiedName && it.methodName != JazzerTarget::fuzzTargetOne.name
        }.toTypedArray()
    }

    private fun configure(reproducerPath: Path, method: Method): List<String> {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val currentCorpus = config.corpusDir.resolve(method.fullName)
        currentCorpus.createDirectories()

        if (config.target.dumpCoverage) {
            val coverageFile = config.global.workDir
                .resolve("coverage")
                .createDirectories()
                .resolve("${method.fullName}.exec")
                .absolutePathString()
            Opt.coverageDump.setIfDefault(coverageFile)
        }

        libFuzzerArgs += currentCorpus.absolutePathString()
        libFuzzerArgs += reproducerPath.absolutePathString()
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimitMb}"
        libFuzzerArgs += "-artifact_prefix=${reproducerPath.absolute()}/"
        libFuzzerArgs += "-max_total_time=${config.target.maxFuzzTime.inWholeSeconds}"

        return libFuzzerArgs
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun runTarget(instance: Any, method: Method): FuzzingResult {
        val elapsed = measureTimeMillis {
            configureFuzzTargetHolder(method, instance)

            val reproducerPath = config.reproducerPathOf(method)
            if (!reproducerPath.exists()) {
                reproducerPath.createDirectories()
            }

            oldRepresentatives = reproducerPath.listStackTraces().size

            val libFuzzerArgs = configure(reproducerPath, method)

            FuzzTargetRunner.registerFatalFindingDeterminatorForJUnit { bytes, finding ->
                val hash = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()
                isTerminalFinding(hash, finding, reproducerPath)
            }

            FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)
        }

        return FuzzingResult(countCrashes(config.reproducerPathOf(method)), elapsed.milliseconds)
    }

    private fun countCrashes(reproducerPath: Path) = reproducerPath.listClusters().size - oldRepresentatives!!

    private fun configureFuzzTargetHolder(method: Method, instance: Any) {
        if (method.isAnnotationPresent(KFuzzTest::class.java)) {
            FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
                JazzerTarget::fuzzTargetOne.javaMethod,
                LifecycleMethodsInvoker.noop(JazzerTarget),
            )
            JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        } else {
            log.info { "Using legacy target" }
            FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
                method, LifecycleMethodsInvoker.noop(instance),
            )
        }
    }

    private fun isTerminalFinding(hash: String, finding: Throwable, reproducerPath: Path): Boolean {
        val file = reproducerPath.absolute().resolve("stacktrace-$hash")

        if (!file.exists()) {
            file.createFile()
            finding.removeInnerFrames()
            file.writeText(finding.stackTraceToString())
        }

        val currentRepresentatives = clusterCrashes(reproducerPath)
        return config.target.keepGoing != 0L && currentRepresentatives - oldRepresentatives!! >= config.target.keepGoing
    }

    private fun initJazzer() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.global.hooks)
        Opt.instrumentationIncludes.setIfDefault(config.global.instrument)
        Opt.customHookIncludes.setIfDefault(config.global.instrument)
        Opt.customHookExcludes.setIfDefault(config.global.customHookExcludes)
        Opt.keepGoing.setIfDefault(config.target.keepGoing)
        Opt.reproducerPath.setIfDefault(config.global.reproducerDir.absolutePathString())

        AgentInstaller.install(Opt.hooks.get())
    }
}
