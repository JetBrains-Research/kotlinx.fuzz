package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.agent.AgentInstaller
import com.code_intelligence.jazzer.driver.FuzzTargetHolder
import com.code_intelligence.jazzer.driver.FuzzTargetRunner
import com.code_intelligence.jazzer.driver.LifecycleMethodsInvoker
import com.code_intelligence.jazzer.driver.Opt
import com.code_intelligence.jazzer.utils.Log
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.*
import kotlin.reflect.jvm.javaMethod
import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzEngine

@Suppress("unused")
class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val jazzerConfig = JazzerConfig.fromSystemProperties()

    init {
        val codeLocation = this::class.java.protectionDomain.codeSource.location
        val fileCodeLocation = File(codeLocation.toURI())
        val libsLocation = fileCodeLocation.parentFile
        System.load("$libsLocation/${System.mapLibraryName("casr_adapter")}")
    }

    private external fun parseAndClusterStackTraces(rawStacktraces: List<String>): List<Int>

    override fun initialise() {
        Log.fixOutErr(System.out, System.err)

        Opt.hooks.setIfDefault(config.hooks)
        Opt.instrumentationIncludes.setIfDefault(config.instrument)
        Opt.customHookIncludes.setIfDefault(config.instrument)
        Opt.customHookExcludes.setIfDefault(config.customHookExcludes)

        AgentInstaller.install(Opt.hooks.get())

        FuzzTargetHolder.fuzzTarget = FuzzTargetHolder.FuzzTarget(
            JazzerTarget::fuzzTargetOne.javaMethod,
            LifecycleMethodsInvoker.noop(JazzerTarget),
        )
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalStdlibApi::class)
    override fun runTarget(instance: Any, method: Method): Throwable? {
        val libFuzzerArgs = mutableListOf("fake_argv0")
        val corpusDir = createTempDirectory("jazzer-corpus")

        libFuzzerArgs += corpusDir.toString()
        libFuzzerArgs += "-max_total_time=${config.maxSingleTargetFuzzTime.inWholeSeconds}"
        libFuzzerArgs += "-rss_limit_mb=${jazzerConfig.libFuzzerRssLimit}"

        val atomicFinding = AtomicReference<Throwable>()
        FuzzTargetRunner.registerFatalFindingHandlerForJUnit { bytes, finding ->
            atomicFinding.set(finding)
            val hash = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()
            val file = Paths.get(Opt.reproducerPath.get(), "stacktrace-$hash")
            if (!file.exists()) {
                file.createFile()
                var indexOfFirstInternalFrame = finding.stackTrace.indexOfFirst {
                    it.className == "kotlinx.fuzz.jazzer.JazzerTarget" && it.methodName == "fuzzTargetOne"
                }
                var inCausedBy = false
                val stackTraceString = finding.stackTraceToString()
                file.writeText(stackTraceString.split("\n").filter {
                    if (inCausedBy) {
                        true
                    } else if (it.trim().startsWith("at")) {
                        indexOfFirstInternalFrame -= 1
                        indexOfFirstInternalFrame >= 0
                    } else if (it.trim().startsWith("Caused by")) {
                        inCausedBy = true
                        true
                    } else {
                        true
                    }
                }.joinToString("\n"))
            }
        }

        JazzerTarget.reset(MethodHandles.lookup().unreflect(method), instance)
        FuzzTargetRunner.startLibFuzzer(libFuzzerArgs)

        corpusDir.deleteRecursively()

        return atomicFinding.get()
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

    override fun finishExecution() {
        val directoryPath = Paths.get(Opt.reproducerPath.get()).absolute()
        val stacktraceFiles = directoryPath.listDirectoryEntries("stacktrace-*")

        val rawStackTraces = mutableListOf<String>()
        val fileMapping = mutableListOf<Pair<Path, Path>>()

        stacktraceFiles.forEach { file ->
            val crashFile = directoryPath.resolve("crash-${file.name.removePrefix("stacktrace-")}")
            val lines = convertToJavaStyleStackTrace(Files.readString(file))
            rawStackTraces.add(lines)
            fileMapping.add(file to crashFile)
        }

        val clusters = parseAndClusterStackTraces(rawStackTraces)
        val mapping = mutableMapOf<Int, Path>()

        clusters.forEachIndexed { index, cluster ->
            val (stacktraceSrc, crashSrc) = fileMapping[index]
            val isOld = mapping.containsKey(cluster)

            if (!mapping.containsKey(cluster)) {
                mapping[cluster] = directoryPath.resolve("cluster-${stacktraceSrc.readLines().first().trim()}")
            }

            val clusterDir = directoryPath.resolve(mapping[cluster]!!)
            if (!clusterDir.exists()) {
                clusterDir.createDirectory()
            }

            stacktraceSrc.copyTo(clusterDir.resolve(stacktraceSrc.fileName), overwrite = true)
            if (isOld) {
                stacktraceSrc.deleteExisting()
            }

            crashSrc.copyTo(clusterDir.resolve(crashSrc.fileName), overwrite = true)
            if (isOld) {
                crashSrc.deleteExisting()
            }
        }
    }
}
