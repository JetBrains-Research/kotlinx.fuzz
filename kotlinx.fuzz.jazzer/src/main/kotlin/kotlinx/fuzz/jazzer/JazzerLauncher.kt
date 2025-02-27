package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import java.io.ObjectOutputStream
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.*
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.system.exitProcess
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.debug
import kotlinx.fuzz.log.error
import kotlinx.fuzz.reproducerPathOf
import org.jetbrains.casr.adapter.CasrAdapter

object JazzerLauncher {
    private val log = LoggerFacade.getLogger<JazzerLauncher>()
    private val config = KFuzzConfig.fromSystemProperties()
    private val jazzerConfig = JazzerConfig.fromSystemProperties()
    private var oldRepresentatives: Int? = null  // Number of clusters initially, so that keepGoing will depend inly on new findings
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

        val error = runTarget(instance, targetMethod)
        error?.let {
            serializeException(error, config.exceptionPath(targetMethod))
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

        if (config.dumpCoverage) {
            val coverageFile = config.workDir
                .resolve("coverage")
                .createDirectories()
                .resolve("${method.fullName}.exec")
                .absolutePathString()
            Opt.coverageDump.setIfDefault(coverageFile)
        }

        libFuzzerArgs += currentCorpus.absolutePathString()
        libFuzzerArgs += reproducerPath.absolutePathString()
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimit}"
        libFuzzerArgs += "-artifact_prefix=${reproducerPath.absolute()}/"
        libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"

        return libFuzzerArgs
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun runTarget(instance: Any, method: Method): Throwable? {
        val reproducerPath = config.reproducerPathOf(method)
        if (!reproducerPath.exists()) {
            reproducerPath.createDirectories()
        }

        oldRepresentatives = reproducerPath.listStacktraces().size

        val libFuzzerArgs = configure(reproducerPath, method)

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingDeterminatorForJUnit { bytes, finding ->
            val hash = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()
            val stopFuzzing = isTerminalFinding(hash, finding, reproducerPath)
            if (stopFuzzing) {
                atomicFinding.set(finding)
            }
            stopFuzzing
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        return atomicFinding.get()
    }

    private fun isTerminalFinding(hash: String, finding: Throwable, reproducerPath: Path): Boolean {
        val file = reproducerPath.absolute().resolve("stacktrace-$hash")

        if (!file.exists()) {
            file.createFile()
            finding.removeInnerFrames()
            file.writeText(finding.stackTraceToString())
        }

        val currentRepresentatives = clusterCrashes(reproducerPath)
        return config.keepGoing != 0L && currentRepresentatives - oldRepresentatives!! >= config.keepGoing
    }

    private fun initJazzer() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.hooks)
        Opt.instrumentationIncludes.setIfDefault(config.instrument)
        Opt.customHookIncludes.setIfDefault(config.instrument)
        Opt.customHookExcludes.setIfDefault(config.customHookExcludes)
        Opt.keepGoing.setIfDefault(config.keepGoing)
        Opt.reproducerPath.setIfDefault(config.reproducerPath.absolutePathString())

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod,
            LifecycleMethodsInvoker.noop(JazzerTarget),
        )
    }

    private fun convertToJavaStyleStackTrace(kotlinStackTrace: String): String {
        val lines = kotlinStackTrace.lines()
        if (lines.isEmpty()) {
            return kotlinStackTrace
        }

        val firstLine = lines.first()
        val updatedFirstLine = if (firstLine.startsWith("Exception in thread \"main\"")) {
            firstLine
        } else {
            "Exception in thread \"main\" $firstLine"
        }

        return listOf(updatedFirstLine).plus(lines.drop(1)).joinToString("\n")
    }

    private fun initClustersMapping(
        directoryPath: Path,
        stacktraceFiles: List<Path>,
        clusters: List<Int>,
    ): MutableMap<Int, Path> {
        val mapping = mutableMapOf<Int, Path>()
        directoryPath.listClusters().map { it.name.removePrefix("cluster-") }.forEach { hash ->
            val clusterId = clusters[stacktraceFiles.indexOfFirst { it.name.endsWith(hash) }]
            mapping[clusterId] = directoryPath.resolve("cluster-$hash")
        }
        return mapping
    }

    private fun clusterCrashes(directoryPath: Path): Int {
        val stacktraceFiles = directoryPath.listStacktraces()

        val rawStackTraces = mutableListOf<String>()

        stacktraceFiles.forEach { file ->
            val lines = convertToJavaStyleStackTrace(file.readText())
            rawStackTraces.add(lines)
        }

        val clusters = CasrAdapter.parseAndClusterStackTraces(rawStackTraces)
        val mapping = initClustersMapping(directoryPath, stacktraceFiles, clusters)

        clusters.forEachIndexed { index, cluster ->
            val stacktraceSrc = stacktraceFiles[index]

            if (!mapping.containsKey(cluster)) {
                mapping[cluster] = directoryPath.resolve("cluster-${stacktraceSrc.name.removePrefix("stacktrace-")}")
            }

            val clusterDir = directoryPath.resolve(mapping[cluster]!!)
            if (!clusterDir.exists()) {
                clusterDir.createDirectory()
            }

            stacktraceSrc.copyTo(clusterDir.resolve(stacktraceSrc.name), overwrite = true)
            if (mapping[cluster]!!.name.removePrefix("cluster-") != stacktraceSrc.name.removePrefix("stacktrace-")) {
                stacktraceSrc.deleteExisting()
            }
        }

        return clusters.maxOrNull() ?: 0
    }
}

/**
 * Serializes [throwable] to the specified [path].
 */
private fun serializeException(throwable: Throwable, path: Path) {
    path.outputStream().buffered().use { outputStream ->
        ObjectOutputStream(outputStream).use { objectOutputStream ->
            objectOutputStream.writeObject(throwable)
        }
    }
}
