package kotlinx.fuzz.jazzer

import java.io.DataOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.net.ServerSocket
import java.net.Socket
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
class JazzerEngine(override val config: KFuzzConfig) : KFuzzEngine {
    private val log = LoggerFacade.getLogger<JazzerEngine>()
    private val jazzerConfig = config.engine as JazzerConfig

    override fun initialise() {
        config.corpusDir.createDirectories()
        config.logsDir.createDirectories()
        config.exceptionsDir.createDirectories()
        config.global.reproducerDir.createDirectories()
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

    // spawn subprocess, redirect output to log and err files
    override fun runTarget(instance: Any, method: Method): Throwable? {
        // TODO: pass the config explicitly rather than through system properties
        val config = KFuzzConfig.fromSystemProperties()
        val methodConfig = method.getAnnotation(KFuzzTest::class.java)?.let { annotation ->
            config.addAnnotationParams(annotation)
        } ?: config
        val propertiesList =
            methodConfig.toPropertiesMap().map { (property, value) -> "-D$property=$value" }

        val debugOptions = try {
            getDebugSetup(
                System.getProperty(INTELLIJ_DEBUGGER_DISPATCH_PORT_VAR_NAME).toInt(),
                method,
            )
        } catch (e: Exception) {
            emptyList()
        }

        val command = buildCommand(debugOptions, propertiesList, config, method)
        val exitCode = ProcessBuilder(*command.toTypedArray()).executeAndSaveLogs(
            stdout = "${method.fullName}.log",
            stderr = "${method.fullName}.err",
        )

        return when (exitCode) {
            0 -> null
            else -> getException(config, method)
        }
    }

    private fun buildCommand(
        debugOptions: List<String>,
        propertiesList: List<String>,
        config: KFuzzConfig,
        method: Method,
    ): List<String> {
        val classpath = System.getProperty("java.class.path")
        val javaCommand = System.getProperty("java.home") + "/bin/java"

        val command = mutableListOf<String>(
            javaCommand,
            "-XX:-OmitStackTraceInFastThrow",
            "-classpath", classpath,
            "-Xmx${jazzerConfig.subprocessMaxHeapSizeMb}m",
            *debugOptions.toTypedArray(),
            *propertiesList.toTypedArray(),
        )
        if (config.target.dumpCoverage) {
            val coverageFile = config.coverageFile(method)
            val opt =
                "-javaagent:$jacocoAgentJar=destfile=$coverageFile,dumponexit=true,output=file,jmx=false,includes=kotlinx.datetime.**"

            command += opt
        }
        command += JazzerLauncher::class.qualifiedName!!
        command += method.declaringClass.name
        command += method.name
        return command
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

internal fun KFuzzConfig.coverageFile(method: Method): Path = global.workDir
    .resolve("coverage")
    .createDirectories()
    .resolve("${method.fullName}.exec")
    .absolute()

internal fun KFuzzConfig.exceptionPath(method: Method): Path =
    exceptionsDir.resolve("${method.fullName}.exception")

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
