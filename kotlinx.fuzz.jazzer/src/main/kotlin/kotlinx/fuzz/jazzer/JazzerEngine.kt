package kotlinx.fuzz.jazzer

import java.io.DataOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlinx.fuzz.KFuzzEngine
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.addAnnotationParams
import kotlinx.fuzz.config.JazzerConfig
import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.log.LoggerFacade
import kotlinx.fuzz.log.error

private const val INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME = "idea.debugger.dispatch.port"

internal val Method.fullName: String
    get() = "${this.declaringClass.name}.${this.name}"

internal val KFuzzConfig.corpusDir: Path
    get() = global.workDir.resolve("corpus")

internal val KFuzzConfig.logsDir: Path
    get() = global.workDir.resolve("logs")

internal val KFuzzConfig.exceptionsDir: Path
    get() = global.workDir.resolve("exceptions")

@Suppress("unused")
class JazzerEngine(private val config: KFuzzConfig) : KFuzzEngine {
    private val log = LoggerFacade.getLogger<JazzerEngine>()
    private val jazzerConfig = config.engine as JazzerConfig

    override fun initialise() {
        config.corpusDir.createDirectories()
        config.logsDir.createDirectories()
        config.exceptionsDir.createDirectories()
        config.global.reproducerDir.createDirectories()
        initialCrashDeduplication()
    }

    private fun initialCrashDeduplication() {
        config.global.reproducerDir.listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach { classDir ->
                classDir.listDirectoryEntries()
                    .filter { it.isDirectory() }
                    .forEach { methodDir ->
                        flatten(methodDir)
                        JazzerLauncher.clusterCrashes(methodDir)
                    }
            }
        clusterCrashes()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun flatten(dir: Path) {
        Files.walk(dir).filter { it.isRegularFile() }.forEach {
            val targetFile = dir.resolve(it.name)
            if (targetFile.exists()) {
                return@forEach
            }
            it.copyTo(targetFile)
        }
        dir.listDirectoryEntries().filter { it.isDirectory() }.forEach { it.deleteRecursively() }
    }

    private fun getDebugSetup(intellijDebuggerDispatchPort: Int, method: Method): List<String> {
        val port = ServerSocket(0).use { it.localPort }
        Socket("127.0.0.1", intellijDebuggerDispatchPort).use { socket ->
            DataOutputStream(socket.getOutputStream()).use { output ->
                output.writeUTF("Gradle JVM")
                output.writeUTF("Jazzer Run Target: ${method.name}")
                output.writeUTF("DEBUG_SERVER_PORT=$port")
                output.flush()

                socket.inputStream.read()
            }
        }

        return listOf("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=$port")
    }

    override fun runTarget(instance: Any, method: Method): Throwable? {
        // spawn subprocess, redirect output to log and err files
        val classpath = System.getProperty("java.class.path")
        val javaCommand = System.getProperty("java.home") + "/bin/java"

        // TODO: pass the config explicitly rather than through system properties
        val config = KFuzzConfig.fromSystemProperties()
        val methodConfig = method.getAnnotation(KFuzzTest::class.java)?.let { annotation ->
            config.addAnnotationParams(annotation)
        } ?: config
        val propertiesList =
            methodConfig.toPropertiesMap().map { (property, value) -> "-D$property=$value" }

        val debugOptions = try {
            getDebugSetup(System.getProperty(INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME).toInt(), method)
        } catch (e: Exception) {
            emptyList()
        }
        val exitCode = ProcessBuilder(
            javaCommand,
            "-XX:-OmitStackTraceInFastThrow",
            "-classpath", classpath,
            "-Xmx${jazzerConfig.maxHeapSizeMb}m",
            *debugOptions.toTypedArray(),
            *propertiesList.toTypedArray(),
            JazzerLauncher::class.qualifiedName!!,
            method.declaringClass.name, method.name,
        ).executeAndSaveLogs(
            stdout = "${method.fullName}.log",
            stderr = "${method.fullName}.err",
        )

        return when (exitCode) {
            0 -> null
            else -> getException(config, method)
        }
    }

    private fun getException(config: KFuzzConfig, method: Method): Throwable {
        val path = config.exceptionPath(method)
        return when {
            path.notExists() -> Error("'path' = $path not exists. Can't read exception from test '${method.fullName}'")
            else -> deserializeException(path) ?: run {
                log.error { "Failed to deserialize exception for target '${method.fullName}'" }
                Error("Failed to deserialize exception for target '${method.fullName}'")
            }
        }
    }

    override fun finishExecution() {
        collectStatistics()
        clusterCrashes()
    }

    private fun clusterCrashes() {
        val crashesForDeletion = mutableListOf<Path>()
        Files.walk(config.global.reproducerDir)
            .filter { it.isDirectory() && it.name.startsWith("cluster-") }
            .map { it to it.listStacktraces() }
            .flatMap { (dir, files) -> files.stream().map { dir to it } }
            .forEach { (clusterDir, stacktraceFile) ->
                val crashFileName = "crash-${stacktraceFile.name.removePrefix("stacktrace-")}"
                val crashFile = clusterDir.parent.resolve(crashFileName)
                val targetFile = clusterDir.resolve(crashFileName)

                if (targetFile.exists() || !crashFile.exists()) {
                    return@forEach
                }

                crashFile.copyTo(targetFile, overwrite = true)
                if (!clusterDir.name.endsWith(crashFileName.removePrefix("crash-"))) {
                    crashesForDeletion.add(crashFile)
                }
            }
        crashesForDeletion.forEach { it.deleteIfExists() }
    }

    private fun collectStatistics() {
        val statsDir = config.global.workDir.resolve("stats")
            .createDirectories()
        config.logsDir.listDirectoryEntries("*.err").forEach { file ->
            val csvText = jazzerLogToCsv(file, config.target.maxFuzzTime)
            statsDir.resolve("${file.nameWithoutExtension}.csv").writeText(csvText)
        }
    }

    private fun ProcessBuilder.executeAndSaveLogs(stdout: String, stderr: String): Int {
        val process = start()
        val stdoutStream = config.logsDir.resolve(stdout).outputStream()
        val stderrStream = config.logsDir.resolve(stderr).outputStream()
        val stdoutThread = logProcessStream(process.inputStream, stdoutStream) {
            if (config.global.detailedLogging) {
                log.info(it)
            }
        }
        val stderrThread = logProcessStream(process.errorStream, stderrStream) {
            if (config.global.detailedLogging) {
                log.info(it)
            }
        }
        val exitCode = process.waitFor()
        stdoutThread.join()
        stderrThread.join()
        return exitCode
    }

    private fun logProcessStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        log: (String?) -> Unit,
    ): Thread = thread(start = true) {
        inputStream.bufferedReader().use { reader ->
            outputStream.bufferedWriter().use { writer ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    writer.appendLine(line)
                    log(line)
                }
            }
        }
    }
}

internal fun KFuzzConfig.exceptionPath(method: Method): Path =
    exceptionsDir.resolve("${method.fullName}.exception")

internal fun Path.listStacktraces(): List<Path> = listDirectoryEntries("stacktrace-*")

internal fun Path.listClusters(): List<Path> = listDirectoryEntries("cluster-*")

/**
 * Reads a Throwable from the specified [path].
 */
private fun deserializeException(path: Path): Throwable? {
    path.inputStream().buffered().use { inputStream ->
        ObjectInputStream(inputStream).use { objectInputStream ->
            return objectInputStream.readObject() as? Throwable
        }
    }
}
