package org.plan.research

import kotlinx.reflect.lite.*
import kotlinx.reflect.lite.jvm.*
import kotlinx.reflect.lite.impl.*
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.reflect.lite.full.isAccessible
import java.io.File
import java.net.URLClassLoader
import java.util.WeakHashMap
import kotlin.test.assertTrue

object ReflectLiteTests {
    private fun compileAndLoad(className: String, sourceCode: String): KClass<*> {
        val sourceFile = File(FILENAME)

        sourceFile.writeText(sourceCode)

        File(OUTPUT_DIR).mkdirs()

        val process = ProcessBuilder(
            PATH_TO_KOTLIN, sourceFile.absolutePath, "-d", OUTPUT_DIR
        )
        val process2 = process.inheritIO().start()
        val exitCode = process2.waitFor()
        if (exitCode != 0) {
            System.err.println(sourceCode)
            throw RuntimeException("Failed to compile")
        }

        val classLoader = URLClassLoader(arrayOf(File(OUTPUT_DIR).toURI().toURL()))
        val clazz = classLoader.loadClass(className)

        return clazz.kotlin
    }

    var count1 = 0
    var count2 = 0
    val counters = WeakHashMap<String, Int>()

    fun <T> handle(member: KProperty<*>, kClass: KClass<T>) {
        if (member is KProperty1<*, *>) {
            println("hello")
        }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun test(data: FuzzedDataProvider) {
        try {
            count1 += 1
            if (count1 % 10000 == 0) {
                System.err.println("$count2 / $count1")
                System.err.println("$counters")
            }
            val (nameToFeature, className_sourceCode) = data.generateSourceCode()
            val (className, sourceCode) = className_sourceCode
            val kClass = compileAndLoad(className, sourceCode)
            assertTrue { KClass::class.isInstance(kClass) }
            // val kClass = AllFeaturesClass::class.java.kotlin
//            kClass.members.forEach { member ->
//                if (member is KProperty) {
//                    handle(member, kClass)
//                    // assertTrue { nameToFeature.contains("${className}.${member.name}") } // solve problem with extensions
//                    member.isAccessible = true
//                    assertTrue { member.getter.property.name == member.name }
//                }
//            }
        } catch (e: FuzzingStateException) {
            count2 += 1
            counters[e.message] = counters.getOrPut(e.message) { 0 } + 1
        } catch (e: Exception) {
            throw e
        }
    }
}
