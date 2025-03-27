package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.api.MethodHook
import com.code_intelligence.jazzer.api.MethodHooks
import java.lang.reflect.Method
import java.nio.file.FileSystems
import kotlin.io.path.Path
import kotlinx.fuzz.config.KFuzzConfig
import org.reflections.Reflections
import org.reflections.scanners.Scanners.MethodsAnnotated
import org.reflections.util.ConfigurationBuilder

object CustomHooks {
    fun findCustomHookClasses(config: KFuzzConfig): Set<Class<*>> {
        val scanner = MethodsAnnotated
        val reflections = Reflections(ConfigurationBuilder().apply {
            forPackage("")
            scanners += scanner
        })
        val excludedPackagesMatchers = config.global.customHookExcludes.map {
            FileSystems.getDefault().getPathMatcher("glob:$it")
        }

        fun isExcluded(packageName: String) = excludedPackagesMatchers.any {
            it.matches(Path("", packageName))
        }

        val hookMethods = reflections.get(
            MethodsAnnotated
                .with(MethodHook::class.java, MethodHooks::class.java)
                .`as`(Method::class.java)
                .filter {
                    val classPackage = it.declaringClass.`package`?.name ?: return@filter true
                    when {
                        classPackage.startsWith("com.code_intelligence.jazzer") -> false
                        isExcluded(classPackage) -> false
                        else -> true
                    }
                })

        return buildSet {
            for (hook in hookMethods) {
                add(hook.declaringClass)
            }
        }
    }
}
