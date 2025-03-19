package kotlinx.fuzz

import java.lang.reflect.Method
import kotlinx.fuzz.reproduction.CrashReproducerWriter

interface KFuzzEngine {
    var reproducerWriter: CrashReproducerWriter

    /**
     * Initialises engine. Should be called only once for every KFuzzEngine instance
     */
    fun initialise()

    /**
     * Runs engine on the specified target. Crashes should be saved in "<reproducerPath>/<class name>/<method name>".
     * Each crash should be represented as file with name that starts with "crash-", "timeout-" or "slow-unit- and contain byte array
     * that can be passed as input to KFuzzerImpl to reproduce the crash
     *
     * @param instance - instance of a class that contains method under fuzzing (harness)
     * @param method - harness itself
     * @return nullable throwable. Null iff harness ran without failures, cause (look at throwable field in
     * org.junit.platform.engine.TestExecutionResult) otherwise
     */
    fun runTarget(instance: Any, method: Method): Throwable?

    /**
     * Runs any needed postwork, like rearranging crash files
     */
    fun finishExecution()
}
