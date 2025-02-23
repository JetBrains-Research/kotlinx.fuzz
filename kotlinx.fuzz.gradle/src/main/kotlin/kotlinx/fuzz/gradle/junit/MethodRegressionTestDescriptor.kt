package kotlinx.fuzz.gradle.junit

import kotlinx.fuzz.config.KFConfig
import java.lang.reflect.Method
import kotlinx.fuzz.listCrashes
import kotlinx.fuzz.reproducerPathOf
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource

internal class MethodRegressionTestDescriptor(
    private val testMethod: Method, parent: TestDescriptor, config: KFConfig,
) : AbstractTestDescriptor(
    parent.uniqueId.append("method", testMethod.name),
    displayName(testMethod),
    MethodSource.from(testMethod),
) {
    init {
        setParent(parent)
        config.reproducerPathOf(testMethod).listCrashes().forEach { crashFile ->
            addChild(CrashTestDescriptor(testMethod, crashFile, this))
        }
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    companion object {
        private fun displayName(testField: Method): String = try {
            testField.toString()
        } catch (_: IllegalAccessException) {
            "no name"
        }
    }
}
