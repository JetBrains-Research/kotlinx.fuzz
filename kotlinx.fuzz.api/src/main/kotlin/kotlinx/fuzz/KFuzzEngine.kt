package kotlinx.fuzz

import java.lang.reflect.Method

interface KFuzzEngine {
    /**
     * Initialises engine. Should be called only once for every KFuzzEngine instance
     */
    fun initialise()

    /**
     * Runs engine on the specified target
     *
     * @param instance - instance of a class that contains method under fuzzing (harness)
     * @param method - harness itself
     *
     * @return nullable throwable. Null iff harness ran without failures, cause (look at throwable field in org.junit.platform.engine.TestExecutionResult) otherwise
     */
    fun runTarget(instance: Any, method: Method): Throwable?
}