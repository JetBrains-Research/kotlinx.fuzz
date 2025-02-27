package kotlinx.fuzz.junit

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.junit.KotlinxFuzzJunitEngine.Companion.isFuzzTarget
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource

internal class ClassTestDescriptor(
    private val testClass: Class<*>,
    parent: TestDescriptor,
    private val config: KFuzzConfig,
    private val isRegression: Boolean,
    supportJazzerTargets: Boolean,
) : AbstractTestDescriptor(
    parent.uniqueId.append("class", testClass.getName()),
    testClass.getSimpleName(),
    ClassSource.from(testClass),
) {
    init {
        setParent(parent)
        addAllChildren(supportJazzerTargets)
    }

    private fun addAllChildren(supportJazzerTargets: Boolean) {
        ReflectionUtils.findMethods(
            testClass,
            { method -> method.isFuzzTarget(supportJazzerTargets) },
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN,
        )
            .map { method ->
                when {
                    isRegression -> MethodRegressionTestDescriptor(method, this, config)
                    else -> MethodFuzzTestDescriptor(method, this)
                }
            }
            .forEach { child -> this.addChild(child) }
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}
