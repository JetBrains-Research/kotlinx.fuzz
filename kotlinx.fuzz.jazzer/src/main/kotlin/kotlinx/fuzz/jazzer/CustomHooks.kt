package kotlinx.fuzz.jazzer

import com.code_intelligence.jazzer.api.MethodHook
import com.code_intelligence.jazzer.api.MethodHooks
import org.reflections.Reflections
import org.reflections.scanners.Scanners.MethodsAnnotated
import org.reflections.util.ConfigurationBuilder
import java.lang.reflect.Method

object CustomHooks {

    fun findCustomHookClasses(): Set<Class<*>> {
        val scanner = MethodsAnnotated
        val reflections = Reflections(ConfigurationBuilder().apply {
            forPackage("")
            scanners += scanner
        })

        val hookMethods = reflections.get(
            MethodsAnnotated
                .with(MethodHook::class.java, MethodHooks::class.java)
                .`as`(Method::class.java)
                .filter {
                    val classPackage = it.declaringClass.`package`?.name ?: return@filter true
                    !classPackage.startsWith("com.code_intelligence.jazzer")
                })

        return buildSet {
            for (hook in hookMethods) {
                add(hook.declaringClass)
            }
        }
    }

}
