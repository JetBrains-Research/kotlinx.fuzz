package kotlinx.fuzz.gradle.junit

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource
import java.lang.reflect.Method

internal class MethodTestDescriptor(val testMethod: Method, parent: TestDescriptor) :
    AbstractTestDescriptor(
        parent.uniqueId.append("method", testMethod.name),
        displayName(testMethod),
        MethodSource.from(testMethod)
    ) {
    init {
        setParent(parent)
    }

    override fun getType(): TestDescriptor.Type {
        return TestDescriptor.Type.TEST
    }

    companion object {
        private fun displayName(testField: Method): String = try {
            testField.toString()
        } catch (_: IllegalAccessException) {
            "no name"
        }
    }
}
