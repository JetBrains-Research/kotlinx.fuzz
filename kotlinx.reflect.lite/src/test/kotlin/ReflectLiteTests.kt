package org.plan.research

import kotlinx.reflect.lite.*
import kotlinx.reflect.lite.jvm.*
import kotlinx.reflect.lite.impl.*
import kotlinx.reflect.lite.full.*
import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@Suppress("UNCHECKED_CAST")
object ReflectLiteTests {
    private fun compileAndLoad(className: String, sourceCode: String) {
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
            clazzes[declaredClassName] = clazz
            if (!declaredClassFeature.isInterface && !declaredClassFeature.isFunctionalInterface && !declaredClassFeature.hasModifier { it == ClassModifier.ABSTRACT }) {
                val parameters = mutableListOf<Any?>()
                val types = mutableListOf<Class<*>>()
                for (p in declaredClassFeature.getPrimaryConstructor().parameters) {
                    parameters.add(toValue(p.type))
                    types.add(toClass(p.type))
                }
                try {
                    val constr = clazz.getDeclaredConstructor(*types.toTypedArray())
                    constr.isAccessible = true
                    instances[declaredClassName] = constr.newInstance(*parameters.toTypedArray())
                } catch (e: Exception) {
                    val constr = clazz.getDeclaredConstructor()
                    constr.isAccessible = true
                    instances[declaredClassName] = constr.newInstance()
                }
            }
        }
    }

    private val covered = mutableMapOf<String, Boolean>()
    private val instances = mutableMapOf<String, Any>()
    private val clazzes = mutableMapOf<String, Class<*>>()

    private val primitiveJavaTypesToKotlinTypes = mapOf(
        "int" to INT,
        "int[]" to INT_ARRAY,
        "long" to LONG,
        "long[]" to LONG_ARRAY,
        "double" to DOUBLE,
        "double[]" to DOUBLE_ARRAY,
        "float" to FLOAT,
        "float[]" to FLOAT_ARRAY,
        "String" to STRING,
        "String[]" to STRING_ARRAY,
        "java.lang.String" to STRING,
        "java.lang.String[]" to STRING_ARRAY,
        "boolean" to BOOLEAN,
        "boolean[]" to BOOLEAN_ARRAY,
        "kotlin.jvm.functions.Function1<? super java.lang.Integer, java.lang.Integer>" to LAMBDA,
        "kotlin.jvm.functions.Function1<? super java.lang.Integer, java.lang.Integer>[]" to LAMBDA_ARRAY,
        "kotlin.jvm.functions.Function1<java.lang.Integer, java.lang.Integer>" to LAMBDA,
        "kotlin.jvm.functions.Function1<java.lang.Integer, java.lang.Integer>[]" to LAMBDA_ARRAY,
        "kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super kotlin.coroutines.Continuation<? super java.lang.Integer>, ?>" to SUSPEND_LAMBDA,
        "kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super kotlin.coroutines.Continuation<? super java.lang.Integer>, ?>[]" to SUSPEND_LAMBDA_ARRAY,
        "kotlin.jvm.functions.Function2<java.lang.Integer, kotlin.coroutines.Continuation<? super java.lang.Integer>, java.lang.Object>" to SUSPEND_LAMBDA,
        "kotlin.jvm.functions.Function2<java.lang.Integer, kotlin.coroutines.Continuation<? super java.lang.Integer>, java.lang.Object>[]" to SUSPEND_LAMBDA_ARRAY,
        "java.util.List<java.lang.Integer>" to LIST_INT,
        "java.util.List<java.lang.Integer>[]" to LIST_INT_ARRAY,
        "void" to VOID
    )

    private fun getType(oldName: String, isMarkedNullable: Boolean): Type {
        val nameWithTemplate = oldName.replace("$", ".")
        if (nameWithTemplate in primitiveJavaTypesToKotlinTypes) return primitiveJavaTypesToKotlinTypes[nameWithTemplate]!!
        val name = oldName.replace("$", ".").split("<").first()
        if (name == "T") {
            if (isMarkedNullable) return NULLABLE_GENERIC
            return GENERIC
        }

        if (name == "T[]") {
            if (isMarkedNullable) return NULLABLE_GENERIC_ARRAY
            return GENERIC_ARRAY
        }

        if (name.endsWith("[]")) {
            val simpleType = getType(name.substring(0, name.length - 2), isMarkedNullable)
            return Type(simpleType.typeName, "*listOf(${simpleType.defaultValue}).toTypedArray()")
        }

        val classFeatures = qualifiedNameToFeature.filterKeys { it == name || it.endsWith(".$name") }
        assertTrue { classFeatures.size == 1 }
        val qualifiedName = classFeatures.keys.first()

        return possibleTypes[qualifiedName]!!
    }

    private suspend fun <T : Any> handleClass(clazz: Class<T>, qualifiedName: String, data: FuzzedDataProvider) {
        val kClass = clazz.kotlin
        val instance = instances[qualifiedName]

        assertTrue { qualifiedNameToFeature.contains(qualifiedName) && (qualifiedNameToFeature[qualifiedName] is ClassFeature || qualifiedNameToFeature[qualifiedName] is CompanionObjectFeature) }
        assertTrue { covered.contains(qualifiedName) }
        if (covered[qualifiedName]!!) return
        covered[qualifiedName] = true

        if (qualifiedNameToFeature[qualifiedName] is ClassFeature) {
            assertTrue { kClass.isAbstract == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.ABSTRACT } || (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isInterface || (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isFunctionalInterface }
            assertTrue { kClass.isData == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.DATA } }
            assertTrue { kClass.isFinal == (qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.FINAL } || !qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.OPEN } && !kClass.isAbstract) }
            assertTrue { kClass.isInner == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.INNER } }
            assertTrue { kClass.isOpen == (qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.OPEN }) }
            assertTrue { kClass.isSealed == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.SEALED } }
            assertTrue { kClass.isValue == qualifiedNameToFeature[qualifiedName]!!.hasModifier { m -> m == ClassModifier.VALUE } }
            assertTrue { kClass.isFun == (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isFunctionalInterface }
            assertFalse { kClass.isCompanion }
        } else {
            assertTrue { kClass.isCompanion }
            assertFalse { kClass.isAbstract }
            assertFalse { kClass.isData }
            assertTrue { kClass.isFinal }
            assertFalse { kClass.isInner }
            assertFalse { kClass.isOpen }
            assertFalse { kClass.isSealed }
            assertFalse { kClass.isValue }
        }

        assertTrue { kClass.java == clazz }
        try {
            clazz.kotlinPackage.members.forEach { member ->
                handleMember(member, instance!!, qualifiedName, data)
            }
        } catch (e: KotlinReflectionInternalError) {
            // expected?
        }

        if (!kClass.isCompanion && !(qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isInterface && !(qualifiedNameToFeature[qualifiedName]!! as ClassFeature).isFunctionalInterface) {
            assertTrue { kClass.primaryConstructor != null }
            val primaryConstructor = "$qualifiedName.(${
                kClass.primaryConstructor!!.parameters.joinToString(", ") {
                    getType(
                        it.type.javaType.typeName,
                        it.type.isMarkedNullable
                    ).typeName
                }
            })"
            assertTrue { primaryConstructor in qualifiedNameToFeature && qualifiedNameToFeature[primaryConstructor] is PrimaryConstructorFeature }
        }

        kClass.constructors.forEach { constructor ->
            handleConstructor(
                constructor,
                "$qualifiedName.(${
                    constructor.parameters.joinToString(", ") {
                        getType(
                            it.type.javaType.typeName,
                            it.type.isMarkedNullable
                        ).typeName
                    }
                })",
                data,
                qualifiedNameToFeature[qualifiedName]!! as ClassFeature
            )
        }

        kClass.members.forEach { member ->
            if (member.name != "equals" && member.name != "hashCode" && member.name != "toString" && ((!member.name.startsWith(
                    "component"
                ) && member.name != "copy") || !qualifiedNameToFeature[qualifiedName]!!.hasModifier { it == ClassModifier.DATA })
            ) handleMember(member, instance!!, qualifiedName, data)
        }

        assertTrue { (kClass.companionObject != null) == (qualifiedNameToFeature.containsKey("$qualifiedName.Companion")) }

        kClass.nestedClasses.forEach { nestedClass ->
            val nestedClassJava = nestedClass.java
            nestedClassJava as Class<Any>
            assertTrue { nestedClass.qualifiedName == "$qualifiedName.${nestedClass.simpleName}" }
            handleClass(nestedClassJava, nestedClass.qualifiedName!!, data)
        }

        kClass.supertypes.forEach { superType ->
            if ((superType.classifier as KClass<*>).qualifiedName != "kotlin.Any")
                handleType(
                    superType,
                    possibleTypes[(qualifiedNameToFeature[qualifiedName]!! as ClassFeature).superClasses.first { it.name == (superType.classifier as KClass<*>).qualifiedName }.name]!!
                )
        }

        kClass.sealedSubclasses.forEach { subClass ->
            val subClassJava = subClass.java
            subClassJava as Class<Any>
            handleClass(subClassJava, subClass.qualifiedName!!, data)
        }

        kClass.typeParameters.forEach { typeParameter ->
            handleTypeParameter(
                typeParameter,
                false,
                (qualifiedNameToFeature[qualifiedName]!! as ClassFeature).variance
            )
        }
    }

    private suspend fun <T : Any> handleMember(
        member: KCallable<*>,
        instance: T,
        qualifiedName: String,
        data: FuzzedDataProvider
    ) {
        member.isAccessible = true
        val extends = member.parameters.filter { it.kind == KParameter.Kind.EXTENSION_RECEIVER }
            .joinToString(".") { getType(it.type.javaType.typeName, it.type.isMarkedNullable).typeName }
        val name = "$extends${if (extends != "") "." else ""}${member.name}"
        when (member) {
            is KProperty<*> -> handleProperty(member, instance, name, extends, data, qualifiedName)
            is KFunction -> handleFunction(
                member,
                "$qualifiedName.$name(${
                    member.parameters.joinToString(", ") {
                        getType(
                            it.type.javaType.typeName,
                            it.type.isMarkedNullable
                        ).typeName
                    }
                })",
                data,
                qualifiedName
            )

            else -> throw IllegalStateException("Unknown type: ${member::class}")
        }
    }

    private fun equalValues(value: String, value1: String): Boolean {
        val a = value.replace("Function1<java.lang.Integer, java.lang.Integer>", LAMBDA.defaultValue)
            .replace(
                "Function2<java.lang.Integer, kotlin.coroutines.Continuation<? super java.lang.Integer>, java.lang.Object>",
                SUSPEND_LAMBDA.defaultValue
            )
            .replace("[]", LIST_INT.defaultValue).replace(LONG.defaultValue, "0")
        val b = value1.replace("Function1<java.lang.Integer, java.lang.Integer>", LAMBDA.defaultValue)
            .replace(
                "Function2<java.lang.Integer, kotlin.coroutines.Continuation<? super java.lang.Integer>, java.lang.Object>",
                SUSPEND_LAMBDA.defaultValue
            )
            .replace("[]", LIST_INT.defaultValue).replace(LONG.defaultValue, "0")

        if (a.contains("@")) {
            return if (b.contains("@")) {
                a.split("@").first() == b.split("@").first()
            } else {
                possibleTypes[a.split("@").first()]!!.defaultValue.replace(LONG.defaultValue, "0") == b
            }
        } else {
            return if (b.contains("@")) {
                possibleTypes[b.split("@").first()]!!.defaultValue.replace(LONG.defaultValue, "0") == a
            } else {
                a == b
            }
        }
    }

    private suspend fun <T : Any> handleProperty(
        property: KProperty<*>,
        instance: T,
        name: String,
        extends: String,
        data: FuzzedDataProvider,
        className: String
    ) {
        val qualifiedName = "$className.$name"

        assertTrue { qualifiedNameToFeature.contains(qualifiedName) && qualifiedNameToFeature[qualifiedName] is FieldFeature }
        assertTrue { covered.contains(qualifiedName) }
        covered[qualifiedName] = true

        assertTrue {
            (qualifiedNameToFeature[qualifiedName] as FieldFeature).toString()
                .contains("get()") || extends != "" || clazzes[className]!!.declaredFields.any { it == property.javaField }
        }
        assertTrue {
            (qualifiedNameToFeature[qualifiedName] as FieldFeature).toString()
                .contains("get()") || extends != "" || property.javaField!!.kotlinProperty == property
        }
        if (!qualifiedNameToFeature[qualifiedName]!!.hasModifier { it == ParameterModifier.PRIVATE || it == ParameterModifier.INTERNAL }) {
            if (extends == "") {
                assertTrue { property.javaGetter == clazzes[className]!!.getDeclaredMethod("get$name") }
                if (property is KMutableProperty)
                    assertTrue {
                        property.javaSetter == clazzes[className]!!.getDeclaredMethod(
                            "set$name",
                            toClass((qualifiedNameToFeature[qualifiedName] as FieldFeature).type)
                        )
                    }
            } else {
                assertTrue {
                    property.javaGetter == clazzes[className]!!.getDeclaredMethod(
                        "get${name.split('.').last()}",
                        toClass(possibleTypes[extends]!!)
                    )
                }
                if (property is KMutableProperty) {
                    assertTrue {
                        property.javaSetter == clazzes[className]!!.getDeclaredMethod(
                            "set${name.split('.').last()}",
                            toClass(possibleTypes[extends]!!),
                            toClass((qualifiedNameToFeature[qualifiedName] as FieldFeature).type)
                        )
                    }
                }
            }
        }

        assertTrue { property.getter.property.name == property.name }
        when (property) {
            is KProperty0 -> {
                property as KProperty0<Any>
                property.getter.isAccessible = true
                val value = property.get()?.toString() ?: "null"
                val value1 = (qualifiedNameToFeature[qualifiedName] as FieldFeature).type.defaultValue
                assertTrue { equalValues(value, value1) }
                handleCallable(property.getter, data, qualifiedNameToFeature[qualifiedName]!!)
                if (property is KMutableProperty<*>) {
                    property as KMutableProperty0<Any>
                    property.setter.isAccessible = true
                    property.setter.call(instance, property.get())
                    assertTrue { equalValues((property.get()?.toString() ?: "null"), value) }
                    handleCallable(property.setter, data, qualifiedNameToFeature[qualifiedName]!!)
                }
            }

            is KProperty1<*, *> -> {
                property as KProperty1<Any, Any>
                property.getter.isAccessible = true
                val value = property.get(instance)?.toString() ?: "null"
                val value1 = toValue((qualifiedNameToFeature[qualifiedName] as FieldFeature).type).toString()
                assertTrue { equalValues(value, value1) }
                handleCallable(property.getter, data, qualifiedNameToFeature[qualifiedName]!!)
                if (property is KMutableProperty<*>) {
                    property as KMutableProperty1<Any, Any>
                    property.setter.isAccessible = true
                    property.setter.call(instance, property.get(instance))
                    assertTrue { equalValues((property.get(instance)?.toString() ?: "null"), value) }
                    handleCallable(property.setter, data, qualifiedNameToFeature[qualifiedName]!!)
                }
            }

            is KProperty2<*, *, *> -> {
                property as KProperty2<Any, Any, Any>
                property.getter.isAccessible = true
                assertTrue { extends != "" }
                assertTrue { instances.contains(extends) }
                val value = property.get(instance, instances[extends]!!)?.toString() ?: "null"
                val value1 = (qualifiedNameToFeature[qualifiedName] as FieldFeature).type.defaultValue
                assertTrue { equalValues(value, value1) }
                handleCallable(property.getter, data, qualifiedNameToFeature[qualifiedName]!!)
                if (property is KMutableProperty<*>) {
                    property as KMutableProperty2<Any, Any, Any>
                    property.setter.isAccessible = true
                    property.setter.call(instance, instances[extends]!!, property.get(instance, instances[extends]!!))
                    assertTrue { equalValues((property.get(instance, instances[extends]!!)?.toString() ?: "null"), value) }
                    handleCallable(property.setter, data, qualifiedNameToFeature[qualifiedName]!!)
                }
            }
        }
    }

    private suspend fun handleFunction(
        function: KFunction<*>,
        qualifiedName: String,
        data: FuzzedDataProvider,
        className: String
    ) {
        assertTrue { qualifiedNameToFeature.contains(qualifiedName) && qualifiedNameToFeature[qualifiedName] is MethodFeature || qualifiedNameToFeature[qualifiedName] is OperatorFeature }
        assertTrue { covered.contains(qualifiedName) }
        covered[qualifiedName] = true

        assertTrue { clazzes[className]!!.declaredMethods.any { it == function.javaMethod } }
        assertTrue { function.javaMethod!!.kotlinFunction == function }
        assertTrue { function.javaConstructor == null }

        assertFalse { function.isExternal }
        assertTrue { function.isInfix == qualifiedNameToFeature[qualifiedName]!!.hasModifier { it == FunctionModifier.INFIX } }
        assertTrue { function.isInline == qualifiedNameToFeature[qualifiedName]!!.hasModifier { it == FunctionModifier.INLINE } }
        assertTrue { function.isOperator == qualifiedNameToFeature[qualifiedName]!! is OperatorFeature }
        assertTrue { function.isSuspend == qualifiedNameToFeature[qualifiedName]!!.hasModifier { it == FunctionModifier.SUSPEND } }

        handleCallable(function, data, qualifiedNameToFeature[qualifiedName]!!)
    }

    private suspend fun handleConstructor(
        constructor: KFunction<*>,
        qualifiedName: String,
        data: FuzzedDataProvider,
        classFeature: ClassFeature
    ) {
        assertTrue { qualifiedNameToFeature.contains(qualifiedName) }
        assertTrue { covered.contains(qualifiedName) }
        covered[qualifiedName] = true

        assertTrue { clazzes[classFeature.name]!!.declaredConstructors.any { it == constructor.javaConstructor } }
        assertTrue { (constructor.javaConstructor!! as Constructor<Any>).kotlinFunction == constructor }

        assertFalse { constructor.isExternal }
        assertFalse { constructor.isInfix }
        assertFalse { constructor.isInline }
        assertFalse { constructor.isOperator }
        assertFalse { constructor.isSuspend }

        handleCallable(constructor, data, qualifiedNameToFeature[qualifiedName]!!, classFeature)
    }

    private fun handleType(type: KType, expectedType: Type) {
        assertTrue { type.isMarkedNullable == (expectedType == NULLABLE_GENERIC) }
        type.arguments.forEach { handleTypeProjection(it) }
        assertTrue { type.classifier != null }
        if (type.classifier is KTypeParameter) assertTrue { (type.classifier as KTypeParameter).name == "T" }
        else assertTrue {
            (type.classifier as KClass<*>).simpleName == expectedType.typeName.split('.').last() ||
                    (type.classifier as KClass<*>).simpleName == "Function1" && expectedType == LAMBDA ||
                    (type.classifier as KClass<*>).simpleName == "Function2" && expectedType == SUSPEND_LAMBDA ||
                    (type.classifier as KClass<*>).simpleName == "List" && expectedType == LIST_INT
        }
    }

    private fun handleTypeProjection(type: KTypeProjection) {

    }

    private fun handleTypeParameter(
        typeParameter: KTypeParameter,
        isReified: Boolean,
        variance: ClassFeatureVariance = ClassFeatureVariance.INVARIANT
    ) {
        assertTrue { typeParameter.name == "T" }
        assertTrue { typeParameter.isReified == isReified }
        assertTrue {
            typeParameter.upperBounds.size == 1 &&
                    typeParameter.upperBounds[0].classifier == Any::class.java.kotlin
        }
        when (typeParameter.variance) {
            KVariance.INVARIANT -> assertTrue { variance == ClassFeatureVariance.INVARIANT }
            KVariance.IN -> assertTrue { variance == ClassFeatureVariance.IN }
            KVariance.OUT -> assertTrue { variance == ClassFeatureVariance.OUT }
        }
    }

    private fun handleParameters(
        kParameters: List<KParameter>,
        parameters: List<ParameterFeature>,
        hiddenParameters: List<ParameterFeature>
    ) {
        var index = 0
        assertTrue { kParameters.size == hiddenParameters.size + parameters.size }
        for ((kParameter, parameter) in kParameters.zip(hiddenParameters + parameters)) {
            assertTrue { kParameter.isOptional == parameter.withDefaultValue }
            assertTrue { kParameter.isVararg == (parameter.mutability == MutabilityQualifier.VARARG) }
            assertTrue {
                kParameter.name == parameter.name?.split('.')
                    ?.last() || kParameter.name == null && parameter.name == "value"
            } // TODO: strange behaviour
            assertTrue {
                getType(kParameter.type.javaType.typeName, kParameter.type.isMarkedNullable).typeName ==
                        parameter.type.typeName
            }
            assertTrue { kParameter.index == index }
            when (kParameter.kind) {
                KParameter.Kind.INSTANCE -> assertTrue { index == 0 && hiddenParameters.isNotEmpty() }
                KParameter.Kind.EXTENSION_RECEIVER -> assertTrue {
                    0 < index && index < hiddenParameters.size && parameter.name?.contains(
                        "."
                    ) ?: true
                }

                KParameter.Kind.VALUE -> assertTrue { index >= hiddenParameters.size }
            }
            index += 1
        }
    }

    private suspend fun handleCallable(
        callable: KCallable<*>,
        data: FuzzedDataProvider,
        feature: Feature,
        classFeature: ClassFeature? = null
    ) {
        val returnType: Type?
        lateinit var parameters: MutableList<ParameterFeature>
        when (feature) {
            is PrimaryConstructorFeature, is SecondaryConstructorFeature -> {
                assertFalse { callable.isAbstract }
                if (feature is SecondaryConstructorFeature) { // TODO: strange behaviour
                    assertFalse { callable.isFinal }
                    assertTrue { callable.isOpen }
                } else {
                    assertTrue { callable.isFinal }
                    assertFalse { callable.isOpen }
                }
                assertFalse { callable.isSuspend }
                when (callable.visibility) {
                    KVisibility.PUBLIC -> assertTrue { feature.hasModifier { it == ConstructorModifier.PUBLIC } || !feature.hasModifier { it == ConstructorModifier.PROTECTED || it == ConstructorModifier.PRIVATE || it == ConstructorModifier.INTERNAL } }
                    KVisibility.PROTECTED -> assertTrue { feature.hasModifier { it == ConstructorModifier.PROTECTED } }
                    KVisibility.INTERNAL -> assertTrue { feature.hasModifier { it == ConstructorModifier.INTERNAL } }
                    KVisibility.PRIVATE -> assertTrue { feature.hasModifier { it == ConstructorModifier.PRIVATE } }
                    null -> throw IllegalStateException("visibility should be always representable")
                }
                returnType = getType(callable.returnType.javaType.typeName, callable.returnType.isMarkedNullable)
                assertTrue { callable.name == "<init>" }
                handleType(callable.returnType, returnType)
                callable.typeParameters.forEach { handleTypeParameter(it, false) }
                parameters =
                    if (feature is PrimaryConstructorFeature) feature.parameters else (feature as SecondaryConstructorFeature).parameters
                handleParameters(callable.parameters, parameters, emptyList())
            }

            is MethodFeature, is OperatorFeature -> {
                assertTrue { callable.isAbstract == feature.hasModifier { it == FunctionModifier.ABSTRACT } }
                assertTrue { callable.isFinal == feature.hasModifier { it == FunctionModifier.FINAL } || !feature.hasModifier { it == FunctionModifier.OPEN || it == FunctionModifier.ABSTRACT } }
                assertTrue { callable.isOpen == feature.hasModifier { it == FunctionModifier.ABSTRACT || it == FunctionModifier.OPEN } }
                assertTrue { callable.isSuspend == feature.hasModifier { it == FunctionModifier.SUSPEND } }
                when (callable.visibility) {
                    KVisibility.PUBLIC -> assertTrue { feature.hasModifier { it == FunctionModifier.PUBLIC } || !feature.hasModifier { it == FunctionModifier.PROTECTED || it == FunctionModifier.PRIVATE || it == FunctionModifier.INTERNAL } }
                    KVisibility.PROTECTED -> assertTrue { feature.hasModifier { it == FunctionModifier.PROTECTED } }
                    KVisibility.INTERNAL -> assertTrue { feature.hasModifier { it == FunctionModifier.INTERNAL } }
                    KVisibility.PRIVATE -> assertTrue { feature.hasModifier { it == FunctionModifier.PRIVATE } }
                    null -> throw IllegalStateException("visibility should be always representable")
                }
                assertTrue { callable.name == feature.name!!.split('.').last() }
                returnType =
                    if (feature is OperatorFeature) feature.returnType else (feature as MethodFeature).returnType
                handleType(callable.returnType, returnType)
                callable.typeParameters.forEach {
                    handleTypeParameter(
                        it,
                        if (feature is MethodFeature) feature.isReified() else (feature as OperatorFeature).isReified()
                    )
                }
                val callableParameters =
                    if (feature is MethodFeature) feature.parameters else (feature as OperatorFeature).parameters
                val hiddenParameters =
                    if (feature is MethodFeature) feature.hiddenParameters else (feature as OperatorFeature).hiddenParameters
                handleParameters(callable.parameters, callableParameters, hiddenParameters)
                parameters = (hiddenParameters + callableParameters).toMutableList()
            }

            is FieldFeature -> {
                assertFalse { callable.isAbstract }
                assertTrue { callable.isFinal }
                assertFalse { callable.isOpen }
                assertFalse { callable.isSuspend }
                when (callable.visibility) {
                    KVisibility.PUBLIC -> assertTrue { feature.hasModifier { it == ParameterModifier.PUBLIC } || !feature.hasModifier { it == ParameterModifier.PROTECTED || it == ParameterModifier.PRIVATE || it == ParameterModifier.INTERNAL } }
                    KVisibility.PROTECTED -> assertTrue { feature.hasModifier { it == ParameterModifier.PROTECTED } }
                    KVisibility.INTERNAL -> assertTrue { feature.hasModifier { it == ParameterModifier.INTERNAL } }
                    KVisibility.PRIVATE -> assertTrue { feature.hasModifier { it == ParameterModifier.PRIVATE } }
                    null -> throw IllegalStateException("visibility should be always representable")
                }
                assertTrue {
                    callable.name == "<get-${feature.name.split(".").last()}>" ||
                            callable.name == "<set-${feature.name.split(".").last()}>"
                }
                handleType(callable.returnType, feature.type)
                callable.typeParameters.forEach { handleTypeParameter(it, false) }
                parameters = feature.parameters.toMutableList()
                if (!(feature.type == STRING && feature.isLazy) && !(feature.isDefaultValue && !feature.name.contains(
                        '.'
                    )) && feature.mutability != MutabilityQualifier.VAL && callable.name.contains("set")
                ) {
                    parameters.add(ParameterFeature(feature.type, MutabilityQualifier.VAL, "value", false))
                    handleParameters(
                        callable.parameters,
                        listOf(ParameterFeature(feature.type, MutabilityQualifier.VAL, "value", false)),
                        feature.parameters
                    )
                } else {
                    handleParameters(callable.parameters, emptyList(), feature.parameters)
                }
            }
        }

        val correctParametersMap = mutableMapOf<KParameter, Any?>()
        val correctParametersList = mutableListOf<Any?>()
        for ((parameter, kParameter) in parameters.zip(callable.parameters)) {
            correctParametersList.add(
                toValue(
                    getType(
                        kParameter.type.javaType.typeName,
                        kParameter.type.isMarkedNullable
                    )
                )
            )
            correctParametersMap[kParameter] =
                toValue(getType(kParameter.type.javaType.typeName, kParameter.type.isMarkedNullable))
        }
        try {
            callable.isAccessible = true
            val a = callable.call(*correctParametersList.toTypedArray())
            val b = callable.callBy(correctParametersMap)
            val c = callable.callSuspend(*correctParametersList.toTypedArray())

            if (getType(
                    callable.returnType.javaType.typeName,
                    callable.returnType.isMarkedNullable
                ).typeName in clazzes
            ) {
                assertTrue {
                    clazzes[getType(
                        callable.returnType.javaType.typeName,
                        callable.returnType.isMarkedNullable
                    ).typeName]!!.kotlin.members.filterIsInstance<KProperty1<*, *>>()
                        .all {
                            it as KProperty1<Any?, Any?>
                            it.isAccessible = true
                            val aProperty = it.get(a)
                            val bProperty = it.get(b)
                            val cProperty = it.get(c)
                            equalValues(aProperty.toString(), bProperty.toString()) && equalValues(
                                aProperty.toString(),
                                cProperty.toString()
                            )
                        }
                }
            } else {
                assertTrue { a.toString() == b.toString() && a.toString() == c.toString() }
            }

            for (iteration in 0 until 1000) {
                val parametersMap = mutableMapOf<KParameter, Any?>()
                val parametersList = mutableListOf<Any?>()
                val types = mutableListOf<Type>()
                val length = data.consumeInt(0, MAX_PARAMETERS_NUMBER)
                for (i in 0 until length) {
                    val type = primitiveTypes[data.consumeInt(0, primitiveTypes.size - 1)]
                    val value = toValue(type)
                    val parameter = object : KParameter {
                        override val index: Int
                            get() = data.consumeInt()
                        override val isOptional: Boolean
                            get() = data.consumeBoolean()
                        override val isVararg: Boolean
                            get() = data.consumeBoolean()
                        override val kind: KParameter.Kind
                            get() = KParameter.Kind.values()[data.consumeInt(0, KParameter.Kind.values().lastIndex)]
                        override val name: String?
                            get() = data.consumeString(MAX_STR_LENGTH)
                        override val type: KType
                            get() = object : KType {
                                override val arguments: List<KTypeProjection>
                                    get() = emptyList()
                                override val classifier: KClassifier?
                                    get() = null
                                override val isMarkedNullable: Boolean
                                    get() = data.consumeBoolean()
                            }
                    }
                    parametersList.add(value)
                    types.add(type)
                    parametersMap[parameter] = value
                }
                var throws = parametersList.size != parameters.size
                if (!throws) {
                    for (i in 0 until parametersList.size) {
                        if (parameters[i].type != types[i]) throws = true
                    }
                }
                try {
                    callable.call(*parametersList.toTypedArray())
                } catch (e: IllegalArgumentException) {
                    assertTrue { throws }
                } catch (e: InvocationTargetException) {
                    assertTrue { throws }
                }
                try {
                    callable.callBy(parametersMap)
                } catch (e: IllegalArgumentException) {
                    assertTrue { throws }
                } catch (e: InvocationTargetException) {
                    assertTrue { throws }
                }
                try {
                    callable.callSuspend(*parametersList.toTypedArray())
                } catch (e: IllegalArgumentException) {
                    assertTrue { throws }
                } catch (e: InvocationTargetException) {
                    assertTrue { throws }
                }
            }
        } catch (e: InstantiationException) {
            assertTrue { (feature is PrimaryConstructorFeature || feature is SecondaryConstructorFeature) && classFeature!!.hasModifier { it == ClassModifier.ABSTRACT } }
        }
    }

    private fun toValue(type: Type, suspendFun: suspend (Int) -> Int = { x -> x }): Any? {
        when (type) {
            INT -> return 0
            LONG -> return 0L
            BOOLEAN -> return false
            FLOAT -> return 0f
            DOUBLE -> return 0.0
            STRING -> return ""
            GENERIC -> return ""
            NULLABLE_GENERIC -> return null
            LAMBDA -> return { x: Int -> x }
            SUSPEND_LAMBDA -> return suspendFun
            VOID -> return Unit
            LIST_INT -> return emptyList<Int>()
            INT_ARRAY -> return listOf(0).toIntArray()
            LONG_ARRAY -> return listOf(0L).toLongArray()
            DOUBLE_ARRAY -> return listOf(0.0).toDoubleArray()
            FLOAT_ARRAY -> return listOf(0f).toFloatArray()
            STRING_ARRAY -> return listOf("").toTypedArray()
            BOOLEAN_ARRAY -> return listOf(true).toBooleanArray()
            LAMBDA_ARRAY -> return listOf({ x: Int -> x }).toTypedArray()
            SUSPEND_LAMBDA_ARRAY -> return listOf({ x: Int -> x } as suspend (Int) -> Int).toTypedArray()
            LIST_INT_ARRAY -> return listOf(listOf(0)).toTypedArray()
            GENERIC_ARRAY -> return listOf("").toTypedArray()
            NULLABLE_GENERIC_ARRAY -> return listOf("", null).toTypedArray()
            else -> {
                if (Regex(""".+\(.*\)""").matches(type.defaultValue)) {
                    return instances[type.typeName]
                } else {
                    throw IllegalArgumentException("Unknown type: $type")
                }
            }
        }
    }

    private fun toClass(type: Type): Class<*> {
        when (type) {
            INT -> return Int::class.java
            LONG -> return Long::class.java
            BOOLEAN -> return Boolean::class.java
            FLOAT -> return Float::class.java
            DOUBLE -> return Double::class.java
            STRING -> return String::class.java
            GENERIC -> return Any::class.java
            NULLABLE_GENERIC -> return Any::class.java
            LAMBDA -> return Function1::class.java
            SUSPEND_LAMBDA -> return Function2::class.java
            VOID -> return Unit::class.java
            LIST_INT -> return List::class.java
            INT_ARRAY -> return IntArray::class.java
            LONG_ARRAY -> return LongArray::class.java
            DOUBLE_ARRAY -> return DoubleArray::class.java
            FLOAT_ARRAY -> return FloatArray::class.java
            STRING_ARRAY -> return Array::class.java
            BOOLEAN_ARRAY -> return BooleanArray::class.java
            LAMBDA_ARRAY -> return Array::class.java
            SUSPEND_LAMBDA_ARRAY -> return Array::class.java
            LIST_INT_ARRAY -> return Array::class.java
            GENERIC_ARRAY -> return Array::class.java
            NULLABLE_GENERIC_ARRAY -> return Array::class.java
            else -> {
                if (Regex(""".+\(.*\)""").matches(type.defaultValue)) {
                    return clazzes[type.typeName]!!
                } else {
                    throw IllegalArgumentException("Unknown type: $type")
                }
            }
        }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun test(data: FuzzedDataProvider) {
        try {
            val (className, sourceCode) = data.generateSourceCode()
            compileAndLoad(className, sourceCode)

            covered.clear()
            qualifiedNameToFeature.forEach { (key, _) -> covered[key] = false }

            runBlocking {
                handleClass(clazzes[className]!!, className, data)
            }

            assertTrue { covered.all { (_, value) -> value } }
        } catch (e: FuzzingStateException) {
            // We can't do anything here
        }
    }
}
