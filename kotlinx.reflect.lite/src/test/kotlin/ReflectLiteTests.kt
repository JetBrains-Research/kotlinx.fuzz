package org.plan.research

import kotlinx.reflect.lite.*
import kotlinx.reflect.lite.jvm.*
import kotlinx.reflect.lite.impl.*
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.reflect.lite.full.isAccessible
import java.io.File
import java.net.URLClassLoader
import kotlin.test.assertTrue


object ReflectLiteTests {
    private fun compileAndLoad(className: String, sourceCode: String): Class<*> {
        val sourceFile = File(FILENAME)

        sourceFile.writeText(sourceCode)

        File(OUTPUT_DIR).mkdirs()

        val process = ProcessBuilder(
            PATH_TO_KOTLIN, sourceFile.absolutePath, "-d", OUTPUT_DIR
        )
        val process2 = process.inheritIO().start()
        val exitCode = process2.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Failed to compile")
        }

        val classLoader = URLClassLoader(arrayOf(File(OUTPUT_DIR).toURI().toURL()))
        for ((declaredClassName, declaredClassFeature) in (qualifiedNameToFeature[className] as ClassFeature).getDeclaredClasses()) {
            val clazz = classLoader.loadClass(declaredClassName.replace('.', '$'))
            if (!declaredClassFeature.isInterface && !declaredClassFeature.isFunctionalInterface)
                instances[declaredClassName] = clazz.getConstructor().newInstance()

        }
        val clazz = classLoader.loadClass(className)

        return clazz
    }

    private var count1 = 0
    private var count2 = 0
    private val counters = mutableMapOf<String, Int>()
    private val covered = mutableMapOf<String, Boolean>()
    private val instances = mutableMapOf<String, Any>()

    private val primitiveJavaTypesToKotlinTypes = mapOf(
        "int" to INT,
        "long" to LONG,
        "double" to DOUBLE,
        "float" to FLOAT,
        "String" to STRING,
        "boolean" to BOOLEAN,
        "kotlin.jvm.functions.Function1<? super java.lang.Integer, java.lang.Integer>" to LAMBDA,
        "kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super kotlin.coroutines.Continuation<? super java.lang.Integer>, ?>" to SUSPEND_LAMBDA,
        "T" to NULLABLE_GENERIC // TODO: or generic?
    )

    private fun getType(parameter: KParameter): Type {
        val name = parameter.type.javaType.typeName

        if (name in primitiveJavaTypesToKotlinTypes) return primitiveJavaTypesToKotlinTypes[name]!!

        val classFeatures = qualifiedNameToFeature.filterKeys { it == name || it.endsWith(".$name") }
        assertTrue { classFeatures.size == 1 }
        val qualifiedName = classFeatures.keys.first()

        return Type(qualifiedName, "$qualifiedName(${
            (classFeatures[qualifiedName] as ClassFeature).getPrimaryConstructor().getParameterTypes()
                .joinToString(", ") { t -> t.defaultValue }
        })")
    }

    private fun <T : Any> handleClass(clazz: Class<T>, qualifiedName: String) {
        val kClass = clazz.kotlin
        val instance = instances[qualifiedName]

        assertTrue { covered.contains(qualifiedName) }
        covered[qualifiedName] = true

        assertTrue { kClass.isAbstract == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.ABSTRACT } || (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isInterface || (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isFunctionalInterface }
        assertTrue { kClass.isCompanion == qualifiedNameToFeature[qualifiedName]!! is CompanionObjectFeature }
        assertTrue { kClass.isData == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.DATA } }
        assertTrue { kClass.isFinal == (qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.FINAL } || !qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.OPEN } && !kClass.isAbstract) }
        assertTrue { kClass.isFun == (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isFunctionalInterface }
        assertTrue { kClass.isInner == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.INNER } }
        assertTrue { kClass.isOpen == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.OPEN || kClass.isAbstract } }
        assertTrue { kClass.isSealed == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.SEALED } }
        assertTrue { kClass.isValue == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.VALUE } }

        kClass.constructors.forEach { constructor ->
            handleConstructor(
                constructor,
                "$qualifiedName.(${constructor.parameters.joinToString(", ") { getType(it).typeName }})"
            )
        }

        kClass.members.forEach { member ->
            if (member.name != "equals" && member.name != "hashCode" && member.name != "toString")
                handleMember(member, instance!!, qualifiedName)
        }

        kClass.nestedClasses.forEach { nestedClass ->
            val nestedClassJava = nestedClass.java
            nestedClassJava as Class<Any>
            assertTrue { nestedClass.qualifiedName == "$qualifiedName.${nestedClass.simpleName}" }
            handleClass(nestedClassJava, nestedClass.qualifiedName!!)
        }

        kClass.supertypes.forEach { superType -> handleType(superType) }

        kClass.sealedSubclasses.forEach { subClass ->
            val subClassJava = subClass.java
            subClassJava as Class<Any>
            handleClass(subClassJava, subClass.qualifiedName!!)
        }

        kClass.typeParameters.forEach { typeParameter -> handleTypeParameter(typeParameter) }
    }

    private fun <T : Any> handleMember(member: KCallable<*>, instance: T, qualifiedName: String) {
        assertTrue { covered.contains(qualifiedName) }
        covered[qualifiedName] = true
        member.isAccessible = true
        val extends = member.parameters.filter { it.kind == KParameter.Kind.EXTENSION_RECEIVER }
            .joinToString(".") { getType(it).typeName }
        val name = "$extends${if (extends != "") "." else ""}${member.name}"
        when (member) {
            is KProperty<*> -> handleProperty(member, instance, "$qualifiedName.$name", extends)
            is KFunction -> handleFunction(
                member,
                instance,
                "$qualifiedName.${name}(${member.parameters.joinToString(", ") { getType(it).typeName }})",
                extends
            )

            else -> throw IllegalStateException("Unknown type: ${member::class}")
        }
    }

    private fun <T : Any> handleProperty(property: KProperty<*>, instance: T, qualifiedName: String, extends: String) {
        assertTrue { property.getter.property.name == property.name }
        assertTrue { qualifiedNameToFeature.containsKey(qualifiedName) }
        when (property) {
            is KProperty0 -> {
                property as KProperty0<Any>
                val value = property.get().toString()
                val value1 = (qualifiedNameToFeature[qualifiedName] as FieldFeature).type.defaultValue
                assertTrue { value1 == LAMBDA.defaultValue && value == "Function1<java.lang.Integer, java.lang.Integer>" || value == value1 }
                handleCallable(property.getter)
                if (property is KMutableProperty<*>) {
                    property as KMutableProperty0<Any>
                    property.setter.call(instance, property.get())
                    assertTrue { property.get().toString() == value }
                    handleCallable(property.setter)
                }
            }

            is KProperty1<*, *> -> {
                property as KProperty1<Any, Any>
                val value = property.get(instance).toString()
                val value1 = (qualifiedNameToFeature[qualifiedName] as FieldFeature).type.defaultValue
                assertTrue { value1 == LAMBDA.defaultValue && value == "Function1<java.lang.Integer, java.lang.Integer>" || value == value1 }
                handleCallable(property.getter)
                if (property is KMutableProperty<*>) {
                    property as KMutableProperty1<Any, Any>
                    property.setter.call(instance, property.get(instance))
                    assertTrue { property.get(instance).toString() == value }
                    handleCallable(property.setter)
                }
            }

            is KProperty2<*, *, *> -> {
                property as KProperty2<Any, Any, Any>
                assertTrue { extends != "" }
                assertTrue { instances.contains(extends) }
                val value = property.get(instance, instances[extends]!!)
                val value1 = (qualifiedNameToFeature[qualifiedName] as FieldFeature).type.defaultValue
                assertTrue { value1 == LAMBDA.defaultValue && value == "Function1<java.lang.Integer, java.lang.Integer>" || value == value1 }
                handleCallable(property.getter)
                if (property is KMutableProperty<*>) {
                    property as KMutableProperty2<Any, Any, Any>
                    property.setter.call(instance, property.get(instance, instances[extends]!!))
                    assertTrue { property.get(instance, instances[extends]!!).toString() == value }
                    handleCallable(property.setter)
                }
            }
        }
    }

    private fun <T : Any> handleFunction(function: KFunction<*>, instance: T, qualifiedName: String, extends: String) {
        assertTrue { qualifiedNameToFeature.containsKey(qualifiedName) }
    }

    private fun handleConstructor(constructor: KFunction<*>, qualifiedName: String) {
        assertTrue { covered.contains(qualifiedName) }
        covered[qualifiedName] = true
    }

    private fun handleType(type: KType) {}

    private fun handleTypeParameter(typeParameter: KTypeParameter) {}

    private fun handleCallable(callable: KCallable<*>) {}

    @FuzzTest(maxDuration = MAX_DURATION)
    fun test(data: FuzzedDataProvider) {
        try {
            count1 += 1
            if (count1 % 10000 == 0) {
                System.err.println("$count2 / $count1")
                System.err.println("$counters")
            }
            val (className, sourceCode) = data.generateSourceCode()
            val clazz = compileAndLoad(className, sourceCode)

            covered.clear()
            qualifiedNameToFeature.forEach { (key, _) -> covered[key] = false }

            // handleClass(clazz, className)

            // assertTrue { covered.all { (_, value) -> value } }
        } catch (e: FuzzingStateException) {
            count2 += 1
            counters[e.message!!] = counters.getOrPut(e.message!!) { 0 } + 1
        } catch (e: Exception) {
            throw e
        }
    }
}
