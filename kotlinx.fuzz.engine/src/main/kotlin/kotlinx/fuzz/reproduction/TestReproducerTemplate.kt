package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock

/**
 * Interface that describes how templates of test are written in a specific test engine (e.g. JUnit).
 */
interface TestReproducerTemplate {
    /**
     * Method for building whole file for test reproducer.
     *
     * @param identifier - unique identifier of a reproducer
     * @param testCode - code for a reproducer itself
     * @param imports - any additional imports if needed. Each of them should be correct line of code.
     * Strings are used because kotlinpoet doesn't allow star imports
     * @param additionalCode - any additional code if needed.
     */
    fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<String> = emptyList(),
        additionalCode: String = "",
    ): String
}
