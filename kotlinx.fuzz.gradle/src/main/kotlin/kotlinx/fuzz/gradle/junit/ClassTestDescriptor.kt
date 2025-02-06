package kotlinx.fuzz.gradle.junit

import kotlinx.fuzz.KFuzzConfig
import kotlinx.fuzz.KFuzzTest
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource

internal class ClassTestDescriptor(
    private val testClass: Class<*>, parent: TestDescriptor, private val config: KFuzzConfig
) : AbstractTestDescriptor(
    parent.uniqueId.append("class", testClass.getName()),
    testClass.getSimpleName(),
    ClassSource.from(testClass),
) {
    init {
        setParent(parent)
        addAllChildren()
    }

    private fun addAllChildren() {
        ReflectionUtils.findMethods(
            testClass,
            { method -> AnnotationUtils.isAnnotated(method, KFuzzTest::class.java) },
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN,
        )
            .map { method -> MethodTestDescriptor(method, this, config) }
            .forEach { child: MethodTestDescriptor? -> this.addChild(child) }
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}
