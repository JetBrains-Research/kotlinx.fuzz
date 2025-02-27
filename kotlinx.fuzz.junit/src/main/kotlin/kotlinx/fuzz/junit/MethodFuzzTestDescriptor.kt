package kotlinx.fuzz.junit

import java.lang.reflect.Method
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource

internal class MethodFuzzTestDescriptor(
    val testMethod: Method, parent: TestDescriptor,
) : AbstractTestDescriptor(
    parent.uniqueId.append("method", testMethod.name),
    displayName(testMethod),
    MethodSource.from(testMethod),
) {
    init {
        setParent(parent)
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    companion object {
        private fun displayName(testField: Method): String = try {
            testField.toString()
        } catch (_: IllegalAccessException) {
            "no name"
        }
    }
}
