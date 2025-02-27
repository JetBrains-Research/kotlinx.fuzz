package kotlinx.fuzz.junit

import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.name
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource

internal class CrashTestDescriptor(
    val testMethod: Method, val crashFile: Path, parent: TestDescriptor,
) : AbstractTestDescriptor(
    parent.uniqueId.append("crash", crashFile.name),
    displayName(testMethod, crashFile),
    MethodSource.from(testMethod),
) {
    init {
        setParent(parent)
    }

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST

    companion object {
        private fun displayName(testField: Method, crash: Path): String = try {
            "${testField.name}: ${crash.name}"
        } catch (_: IllegalAccessException) {
            "no name"
        }
    }
}
