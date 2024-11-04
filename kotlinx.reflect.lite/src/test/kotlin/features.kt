package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlin.random.Random

enum class MutabilityQualifier(val qualifier: String) {
    EMPTY(""),
    VAL("val"),
    VAR("var"),
    VARARG("vararg"),
}

data class Type(val typeName: String, val defaultValue: String)

val VOID = Type("Unit", "")
val BOOLEAN = Type("Boolean", "false")
val INT = Type("Int", "0")
val FLOAT = Type("Float", "0f")
val LONG = Type("Long", "0L")
val DOUBLE = Type("Double", "0.0")
val STRING = Type("String", "\"\"")
val NULLABLE_GENERIC = Type("T?", "null")
val GENERIC = Type("T", "")
val LAMBDA = Type("(Int) -> Int", "{ x -> x }")
val SUSPEND_LAMBDA = Type("suspend (Int) -> Int", "{ x -> x }")
val LIST_INT = Type("List<Int>", "emptyList()")

val INT_ARRAY = Type("Int", "*listOf(0).toIntArray()")
val LONG_ARRAY = Type("Long", "*listOf(0L).toLongArray()")
val DOUBLE_ARRAY = Type("Double", "*listOf(0.0).toDoubleArray()")
val FLOAT_ARRAY = Type("Float", "*listOf(0f).toFloatArray()")
val STRING_ARRAY = Type("String", "*listOf(\"\").toTypedArray()")
val BOOLEAN_ARRAY = Type("Boolean", "*listOf(true).toBooleanArray()")
val LAMBDA_ARRAY = Type("(Int) -> Int", "*listOf({ x: Int -> x }).toTypedArray()")
val SUSPEND_LAMBDA_ARRAY =
    Type("suspend (Int) -> Int", "*listOf({ x: Int -> x } as suspend (Int) -> Int).toTypedArray()")
val LIST_INT_ARRAY = Type("List<Int>", "*listOf(listOf(0)).toTypedArray()")
val GENERIC_ARRAY = Type("T", "generic")
val NULLABLE_GENERIC_ARRAY = Type("T?", "*listOf(\"\", null).toTypedArray()")

val arrayTypes = listOf(
    INT_ARRAY,
    LONG_ARRAY,
    DOUBLE_ARRAY,
    FLOAT_ARRAY,
    STRING_ARRAY,
    BOOLEAN_ARRAY,
    LAMBDA_ARRAY,
    SUSPEND_LAMBDA_ARRAY,
    GENERIC_ARRAY,
    NULLABLE_GENERIC_ARRAY,
    LIST_INT_ARRAY
)
val primitiveTypes =
    listOf(INT, LONG, DOUBLE, FLOAT, STRING, VOID, NULLABLE_GENERIC, GENERIC, LAMBDA, SUSPEND_LAMBDA, LIST_INT)
val composedTypes = listOf(LAMBDA, SUSPEND_LAMBDA, LIST_INT) + arrayTypes

interface Modifier {
    val modifierName: String
    val modifierType: ModifierType
}

enum class ModifierType {
    VISIBILITY, INHERITANCE, INLINE, STRUCTURE, DEFAULT
}

enum class FunctionModifier(override val modifierName: String, override val modifierType: ModifierType) : Modifier {
    PUBLIC("public", ModifierType.VISIBILITY),
    PRIVATE("private", ModifierType.VISIBILITY),
    PROTECTED("protected", ModifierType.VISIBILITY),
    INTERNAL("internal", ModifierType.VISIBILITY),
    OVERRIDE("override", ModifierType.DEFAULT),
    OPEN("open", ModifierType.INHERITANCE),
    FINAL("final", ModifierType.INHERITANCE),
    ABSTRACT("open abstract", ModifierType.INHERITANCE),
    INLINE("inline", ModifierType.DEFAULT),
    TAILREC("tailrec", ModifierType.DEFAULT),
    SUSPEND("suspend", ModifierType.DEFAULT),
    INFIX("infix", ModifierType.DEFAULT),
}

enum class ParameterModifier(override val modifierName: String, override val modifierType: ModifierType) : Modifier {
    NOINLINE("noinline", ModifierType.INLINE),
    CROSSINLINE("crossinline", ModifierType.INLINE),
    PUBLIC("public", ModifierType.VISIBILITY),
    PRIVATE("private", ModifierType.VISIBILITY),
    PROTECTED("protected", ModifierType.VISIBILITY),
    INTERNAL("internal", ModifierType.VISIBILITY),
    LATEINIT("lateinit", ModifierType.DEFAULT),
}

enum class ConstructorModifier(override val modifierName: String, override val modifierType: ModifierType) : Modifier {
    PUBLIC("public", ModifierType.VISIBILITY),
    PRIVATE("private", ModifierType.VISIBILITY),
    PROTECTED("protected", ModifierType.VISIBILITY),
    INTERNAL("internal", ModifierType.VISIBILITY)
}

enum class ClassModifier(override val modifierName: String, override val modifierType: ModifierType) : Modifier {
    PUBLIC("public", ModifierType.VISIBILITY),
    PRIVATE("private", ModifierType.VISIBILITY),
    PROTECTED("protected", ModifierType.VISIBILITY),
    INTERNAL("internal", ModifierType.VISIBILITY),
    OPEN("open", ModifierType.INHERITANCE),
    FINAL("final", ModifierType.INHERITANCE),
    ABSTRACT("abstract", ModifierType.INHERITANCE),
    SEALED("sealed", ModifierType.INHERITANCE),
    VALUE("value", ModifierType.STRUCTURE),
    DATA("data", ModifierType.STRUCTURE),
    INNER("inner", ModifierType.DEFAULT),
    ENUM("enum", ModifierType.STRUCTURE),
    ANNOTATION("annotation", ModifierType.STRUCTURE)
}

abstract class Feature(open val name: String?) {
    internal val modifiers: MutableList<Modifier> = mutableListOf()
    internal val annotations: MutableList<String> = mutableListOf()

    abstract fun addModifier(modifier: Modifier)
    fun addAnnotation(annotation: String) {
        if (!annotations.contains(annotation))
            annotations.add(annotation)
    }

    fun hasModifier(predicate: (Modifier) -> Boolean) = modifiers.any(predicate)
    abstract override fun toString(): String
    abstract fun toShortString(): String
}

class MethodFeature(
    private val reifiedIfNeeded: Boolean,
    private val genericIfNeeded: Boolean,
    private val superClasses: List<ClassFeature>,
    val returnType: Type,
    override val name: String,
    val extends: ClassFeature?,
) : Feature(name) {
    val parameters = mutableListOf<ParameterFeature>()
    private var body: String = "{ return ${returnType.defaultValue} }"
    val hiddenParameters = mutableListOf<ParameterFeature>()

    fun addHiddenParameter(parameter: ParameterFeature) {
        hiddenParameters.add(parameter)
    }

    override fun toShortString(): String {
        val parametersString = (hiddenParameters + parameters).joinToString(", ") { it.toShortString() }
        return "${if (extends == null) "" else "${extends.name}."}$name($parametersString)"
    }

    fun getParameterTypes() = parameters.map { p -> p.getType() }

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            !hasModifier { m -> m == FunctionModifier.INLINE } && parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.VISIBILITY } ||
            parameter.mutability == MutabilityQualifier.VAL || parameter.mutability == MutabilityQualifier.VAR ||
            parameter.mutability == MutabilityQualifier.VARARG && parameters.any { it.mutability == MutabilityQualifier.VARARG }) return
        parameters.add(parameter)
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is FunctionModifier) throw IllegalArgumentException("Modifier must be a FunctionModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            modifier == FunctionModifier.OVERRIDE && (superClasses.all { c ->
                !c.hasOpenMethod(
                    returnType,
                    getParameterTypes(),
                    name
                )
            } || superClasses.isEmpty()) ||
            modifier == FunctionModifier.TAILREC && hasModifier { mod -> mod == FunctionModifier.OPEN || mod == FunctionModifier.ABSTRACT } ||
            modifier == FunctionModifier.OPEN && hasModifier { mod -> mod == FunctionModifier.TAILREC } ||
            modifier == FunctionModifier.ABSTRACT && hasModifier { mod -> mod == FunctionModifier.TAILREC } ||
            modifier == FunctionModifier.INFIX && parameters.size != 1 ||
            modifier == FunctionModifier.INFIX && parameters.any { it.mutability == MutabilityQualifier.VARARG }
        ) return

        if (modifier == FunctionModifier.TAILREC) {
            parameters.add(ParameterFeature(INT, MutabilityQualifier.EMPTY, "n_", true, false))
            if (returnType != VOID) {
                parameters.add(ParameterFeature(returnType, MutabilityQualifier.EMPTY, "accumulator_", true, false))
                body =
                    "{ return if (n_ <= 1) { accumulator_ } else { $name(${parameters.joinToString(", ") { if (it.name == "n_") "n_=n_ - 1" else "${it.name} = ${it.name}" }}) } }"
            } else {
                body =
                    "{ return if (n_ <= 1) { println(n) } else { $name(${parameters.joinToString(", ") { if (it.name == "n_") "n_=n_ - 1" else "${it.name} = ${it.name}" }}) } }"
            }
        }

        modifiers.add(modifier)
    }

    fun isReified() =
        ((returnType == NULLABLE_GENERIC || returnType == GENERIC || parameters.any { it.getType() == NULLABLE_GENERIC || it.getType() == GENERIC }) && genericIfNeeded) && (hasModifier { m -> m == FunctionModifier.INLINE } && reifiedIfNeeded)

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val parametersString = parameters.joinToString(", ") { it.toString() }
        val genericString =
            if ((returnType == NULLABLE_GENERIC || returnType == GENERIC || parameters.any { it.getType() == NULLABLE_GENERIC || it.getType() == GENERIC }) && genericIfNeeded) {
                if (hasModifier { m -> m == FunctionModifier.INLINE } && reifiedIfNeeded) "<reified T: Any>"
                else "<T: Any>"
            } else ""
        return "$annotationsString\n$modifiersString fun $genericString ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name($parametersString) ${if (returnType == VOID) "" else ": @UnsafeVariance ${returnType.typeName}"} ${if (hasModifier { it == FunctionModifier.ABSTRACT }) "" else body}"

    }
}

class OperatorFeature(
    private val reifiedIfNeeded: Boolean,
    private val genericIfNeeded: Boolean,
    private val superClasses: List<ClassFeature>,
    private var body: String,
    val returnType: Type,
    override val name: String,
    private val removeLastDefaultValue: Boolean = false,
    val extends: ClassFeature?,
) : Feature(name) {
    val parameters = mutableListOf<ParameterFeature>()
    val hiddenParameters = mutableListOf<ParameterFeature>()

    fun addHiddenParameter(parameter: ParameterFeature) {
        hiddenParameters.add(parameter)
    }

    override fun toShortString(): String {
        val parametersString = (hiddenParameters + parameters).joinToString(", ") { it.toShortString() }
        return "${if (extends == null) "" else "${extends.name}."}$name($parametersString)"
    }

    fun getParameterTypes() = parameters.map { p -> p.getType() }

    fun getParametersNumber() = parameters.size

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            !hasModifier { m -> m == FunctionModifier.INLINE } && parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.VISIBILITY } ||
            parameter.mutability == MutabilityQualifier.VAL || parameter.mutability == MutabilityQualifier.VAR ||
            parameter.mutability == MutabilityQualifier.VARARG && parameters.any { it.mutability == MutabilityQualifier.VARARG }) return
        parameters.add(parameter)
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is FunctionModifier) throw IllegalArgumentException("Modifier must be a FunctionModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            modifier == FunctionModifier.OVERRIDE && (superClasses.all { c ->
                !c.hasOpenMethod(
                    returnType,
                    getParameterTypes(),
                    name
                )
            } || superClasses.isEmpty()) ||
            modifier == FunctionModifier.TAILREC ||
            modifier == FunctionModifier.OPEN && hasModifier { mod -> mod == FunctionModifier.TAILREC } ||
            modifier == FunctionModifier.ABSTRACT && hasModifier { mod -> mod == FunctionModifier.TAILREC } ||
            modifier == FunctionModifier.INFIX && parameters.size != 1 ||
            modifier == FunctionModifier.INFIX && parameters.any { it.mutability == MutabilityQualifier.VARARG }
        ) return

        if (modifier == FunctionModifier.ABSTRACT) {
            body = ""
        }

        modifiers.add(modifier)
    }

    fun isReified() =
        ((returnType == NULLABLE_GENERIC || returnType == GENERIC || parameters.any { it.getType() == NULLABLE_GENERIC || it.getType() == GENERIC }) && genericIfNeeded) && (hasModifier { m -> m == FunctionModifier.INLINE } && reifiedIfNeeded)

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val parametersString = if (!removeLastDefaultValue) parameters.joinToString(", ") { it.toString() }
        else parameters.indices.joinToString(", ") { i -> if (i == parameters.lastIndex) "${parameters[i].name}: ${parameters[i].getType().typeName}" else parameters[i].toString() }
        val genericString =
            if ((returnType == NULLABLE_GENERIC || returnType == GENERIC || parameters.any { it.getType() == NULLABLE_GENERIC || it.getType() == GENERIC }) && genericIfNeeded) {
                if (hasModifier { m -> m == FunctionModifier.INLINE } && reifiedIfNeeded) "<reified T: Any>"
                else "<T: Any>"
            } else ""
        return "$annotationsString\n$modifiersString operator fun $genericString ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name($parametersString) $body"

    }
}

class ParameterFeature(
    private var type: Type,
    val mutability: MutabilityQualifier,
    override val name: String?,
    var withDefaultValue: Boolean,
    private val notVarargVal: Boolean,
) :
    Feature(name) {
    init {
        if (type == VOID) {
            throw IllegalArgumentException("Parameter type must not be VOID")
        }
        if (isClassInterface[type.typeName] == true || mutability == MutabilityQualifier.VARARG || type == GENERIC) withDefaultValue = false
        if (mutability == MutabilityQualifier.VARARG && type == NULLABLE_GENERIC) type = GENERIC // TODO
    }

    fun getType(purpose: Boolean = true): Type {
        return if (purpose)
            type
        else if (mutability == MutabilityQualifier.VARARG)
            if (type in primitiveTypes)
                arrayTypes.single { type.typeName == it.typeName }
            else
                Type(type.typeName, "*listOf(${type.defaultValue}).toTypedArray()")
        else
            type
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ParameterModifier) throw IllegalArgumentException("Modifier must be a ParameterModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            (mutability == MutabilityQualifier.EMPTY || (mutability == MutabilityQualifier.VARARG && notVarargVal)) && modifier.modifierType == ModifierType.VISIBILITY ||
            type != LAMBDA && modifier.modifierType == ModifierType.INLINE ||
            type != SUSPEND_LAMBDA && modifier.modifierType == ModifierType.INLINE ||
            modifier == ParameterModifier.LATEINIT
        ) return

        modifiers.add(modifier)
    }

    override fun toShortString() = type.typeName

    fun toStringWithUnsafeVariance(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        return if ((type.defaultValue != "generic" && type.defaultValue != "") && mutability != MutabilityQualifier.VARARG && withDefaultValue)
            "$annotationsString $modifiersString ${mutability.qualifier} $name: @UnsafeVariance ${type.typeName}${if (isTypeGeneric[type] == true) "<@UnsafeVariance T>" else ""} = ${type.defaultValue}"
        else
            "$annotationsString $modifiersString ${mutability.qualifier} $name: @UnsafeVariance ${type.typeName}${if (isTypeGeneric[type] == true) "<@UnsafeVariance T>" else ""}"
    }

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        return if ((type.defaultValue != "generic" && type.defaultValue != "") && mutability != MutabilityQualifier.VARARG && withDefaultValue)
            "$annotationsString $modifiersString ${mutability.qualifier} $name: @UnsafeVariance ${type.typeName}${if (isTypeGeneric[type] == true) "<@UnsafeVariance T>" else ""} = ${type.defaultValue}"
        else
            "$annotationsString $modifiersString ${mutability.qualifier} $name: @UnsafeVariance ${type.typeName}${if (isTypeGeneric[type] == true) "<@UnsafeVariance T>" else ""}"
    }
}

class SecondaryConstructorFeature(
    private val isClassSealed: Boolean,
    private val parameterTypes: List<ParameterFeature>,
    override var name: String = ""
) : Feature(name) {
    internal val parameters = mutableListOf<ParameterFeature>()
    private var body = "{}"

    fun isGeneric() = parameters.any { p -> p.getType() == NULLABLE_GENERIC || p.getType() == GENERIC }

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.VISIBILITY } ||
            parameter.mutability == MutabilityQualifier.VAL ||
            parameter.mutability == MutabilityQualifier.VAR ||
            parameter.mutability == MutabilityQualifier.VARARG && parameters.any { it.mutability == MutabilityQualifier.VARARG }) return
        parameters.add(parameter)
        name = "(${parameters.joinToString(", ") { it.getType().typeName }})"
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ConstructorModifier) throw IllegalArgumentException("Modifier must be a ConstructorModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            isClassSealed && modifier == ConstructorModifier.PUBLIC
        ) return

        modifiers.add(modifier)
        name = "(${parameters.joinToString(", ") { it.getType().typeName }})"
    }

    override fun toShortString(): String {
        val parametersString = parameters.joinToString(", ") { it.toShortString() }
        return "($parametersString)"
    }

    fun toStringWithoutBody(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val primaryParametersString = parameterTypes.joinToString(", ") { "${it.name} = ${it.getType(false).defaultValue}" }
        val parametersString = parameters.joinToString(", ") { it.toString() }
        return "$annotationsString\n$modifiersString constructor ($parametersString) : this($primaryParametersString)"
    }

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val primaryParametersString = parameterTypes.joinToString(", ") { "${it.name} = ${it.getType(false).defaultValue}" }
        val parametersString = parameters.joinToString(", ") { it.toString() }
        return "$annotationsString\n$modifiersString constructor ($parametersString) : this($primaryParametersString) $body"
    }
}

class PrimaryConstructorFeature(
    private val isAnnotation: Boolean,
    private val isValue: Boolean,
    override var name: String = "",
    val varargVal: Boolean
) : Feature(name) {
    internal val parameters = mutableListOf<ParameterFeature>()

    fun getNotEmptyParameters() =
        parameters.filterNot { it.mutability == MutabilityQualifier.EMPTY || it.mutability == MutabilityQualifier.VARARG && !varargVal }

    fun getParameterTypes() = parameters.map { p -> p.getType() }

    fun isGeneric() = parameters.any { p -> p.getType() == NULLABLE_GENERIC || p.getType() == GENERIC }

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            isAnnotation && parameter.mutability != MutabilityQualifier.VAL ||
            isAnnotation && parameter.hasModifier { it == ParameterModifier.INTERNAL || it == ParameterModifier.PROTECTED || it == ParameterModifier.PRIVATE } ||
            isAnnotation && (parameter.getType() == LAMBDA || parameter.getType() == SUSPEND_LAMBDA || parameter.getType() == LIST_INT || !primitiveTypes.contains(
                parameter.getType()
            )) ||
            isAnnotation && (parameter.getType() == NULLABLE_GENERIC) ||
            isAnnotation && (parameter.getType() == GENERIC) ||
            parameter.mutability == MutabilityQualifier.VARARG && parameters.any { it.mutability == MutabilityQualifier.VARARG } ||
            isValue && parameter.mutability != MutabilityQualifier.VAL) return
        parameters.add(parameter)
        name = "(${parameters.joinToString(", ") { it.getType().typeName }})"
    }

    override fun addModifier(modifier: Modifier) {
        throw RuntimeException("addModifier should not be called for PrimaryConstructor")
    }


    override fun toShortString(): String {
        val parametersString = parameters.joinToString(", ") { it.toShortString() }
        return "($parametersString)"
    }

    override fun toString(): String {
        val parametersString = parameters.joinToString(", ")
        return "($parametersString)"
    }
}

class FieldFeature(
    val type: Type,
    val mutability: MutabilityQualifier,
    override val name: String,
    val isLazy: Boolean,
    var isDefaultValue: Boolean,
    private var isGetterSetter: Boolean,
    isHidden: Boolean,
    val extends: ClassFeature?
) : Feature(name) {
    val parameters = mutableListOf<ParameterFeature>()

    init {
        if (type == VOID || mutability == MutabilityQualifier.EMPTY || mutability == MutabilityQualifier.VARARG) {
            throw IllegalArgumentException("Field must not be VOID, EMPTY, VARARG or not nullable generic")
        }
        if (type == GENERIC && mutability == MutabilityQualifier.VAR) {
            addModifier(ParameterModifier.LATEINIT)
        } else if (type == GENERIC && !isHidden) {
            throw IllegalArgumentException("Field must not be VAL and non-nullable generic")
        }
        if (extends != null) isDefaultValue = false
        if (!isGetterSetter && !isDefaultValue) isGetterSetter = true
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ParameterModifier) throw IllegalArgumentException("Modifier must be a ParameterModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } || modifier.modifierType == ModifierType.INLINE ||
            modifier == ParameterModifier.LATEINIT ||
            mutability == MutabilityQualifier.EMPTY ||
            mutability == MutabilityQualifier.VARARG
        )
            return

        modifiers.add(modifier)
    }

    fun addParameter(parameter: ParameterFeature) {
        parameters.add(parameter)
    }

    override fun toShortString() = "${if (extends == null) "" else "${extends.name}."}$name"

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        return if (type == STRING && isLazy)
            "$annotationsString\n$modifiersString ${mutability.qualifier} ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name: ${type.typeName} by lazy { ${type.defaultValue} }"
        else if (isDefaultValue && !isGetterSetter)
            "$annotationsString\n$modifiersString ${mutability.qualifier} ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name: @UnsafeVariance ${type.typeName} = ${type.defaultValue}"
        else if (isDefaultValue && isGetterSetter && mutability == MutabilityQualifier.VAR)
            "$annotationsString\n$modifiersString ${mutability.qualifier} ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name: @UnsafeVariance ${type.typeName} = ${type.defaultValue}\nget() = field\nset(value) {}"
        else if (isDefaultValue && isGetterSetter && mutability == MutabilityQualifier.VAL)
            "$annotationsString\n$modifiersString ${mutability.qualifier} ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name: @UnsafeVariance ${type.typeName} = ${type.defaultValue}\nget() = field"
        else if (!isDefaultValue && isGetterSetter && mutability == MutabilityQualifier.VAR)
            "$annotationsString\n$modifiersString ${mutability.qualifier} ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name: @UnsafeVariance ${type.typeName}\nget() = ${type.defaultValue}\nset(value) {}"
        else if (!isDefaultValue && isGetterSetter && mutability == MutabilityQualifier.VAL)
            "$annotationsString\n$modifiersString ${mutability.qualifier} ${if (extends == null) "" else if (extends.isGeneric()) "${extends.name}<@UnsafeVariance T>." else "${extends.name}."}$name: @UnsafeVariance ${type.typeName}\nget() = ${type.defaultValue}"
        else
            throw IllegalStateException("Field should have either getter/setter or default value")
    }
}

class CompanionObjectFeature(override val name: String = "Companion") : Feature(name) {
    private val fields: MutableList<FieldFeature> = mutableListOf()
    private val methods: MutableList<MethodFeature> = mutableListOf()
    private val innerClasses: MutableList<ClassFeature> = mutableListOf()

    fun getDeclaredClasses(): List<Pair<String, ClassFeature>> {
        val classes = mutableListOf<Pair<String, ClassFeature>>()
        for (innerClass in innerClasses) {
            classes.addAll(innerClass.getDeclaredClasses())
        }
        return classes
    }

    fun addField(field: FieldFeature) {
        if (fields.any { f -> f.name == field.name } || field.type == GENERIC || field.type == NULLABLE_GENERIC) return
        fields.add(field)
    }

    fun addMethod(method: MethodFeature) {
        if (methods.any { f -> f.name == method.name && f.getParameterTypes() == method.getParameterTypes() } ||
            method.hasModifier { m -> m == FunctionModifier.ABSTRACT }) return
        methods.add(method)
    }

    fun addInnerClass(innerClass: ClassFeature) {
        if (name == innerClass.name || innerClasses.any { c -> c.name == innerClass.name }) return
        innerClasses.add(innerClass)
    }

    override fun addModifier(modifier: Modifier) {
        throw RuntimeException("addModifier should not be called for CompanionObject")
    }

    override fun toShortString() = "Companion"

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val fieldsString = fields.joinToString("\n")
        val methodsString = methods.joinToString("\n")
        val innerClassesString = innerClasses.joinToString("\n")
        return "$annotationsString\ncompanion object {\n$fieldsString\n$methodsString\n$innerClassesString\n}"
    }
}

enum class TypeVariance {
    IN, OUT, INVARIANT
}

class ClassFeature(
    val isInterface: Boolean,
    val isFunctionalInterface: Boolean,
    private val isInner: Boolean,
    val variance: TypeVariance,
    override val name: String,
    val varargVal: Boolean
) : Feature(name) {
    private var primaryConstructor =
        PrimaryConstructorFeature(isAnnotation = false, isValue = false, varargVal = varargVal)
    private var companionObject: CompanionObjectFeature? = null
    private val secondaryConstructors: MutableList<SecondaryConstructorFeature> = mutableListOf()
    private val fields: MutableList<FieldFeature> = mutableListOf()
    private val hiddenFields: MutableList<FieldFeature> = mutableListOf()
    private val methods: MutableList<MethodFeature> = mutableListOf()
    private val operators: MutableList<OperatorFeature> = mutableListOf()
    val superClasses: MutableList<ClassFeature> = mutableListOf()
    private val innerClasses: MutableList<ClassFeature> = mutableListOf()

    fun methodsNumber() = methods.size + operators.size

    fun getFeatures() =
        listOf(this as Feature) + (if (isInterface || isFunctionalInterface) emptyList() else listOf(primaryConstructor as Feature)) + (if (companionObject == null) emptyList() else listOf(
            companionObject as Feature
        )) + (fields as List<Feature>) + (hiddenFields as List<Feature>) + (methods as List<Feature>) + (operators as List<Feature>) + (secondaryConstructors as List<Feature>)

    fun getDeclaredClasses(): List<Pair<String, ClassFeature>> {
        val classes = mutableListOf(name to this)
        for (superClass in superClasses) {
            classes.addAll(superClass.getDeclaredClasses())
        }
        for (innerClass in innerClasses) {
            classes.addAll(innerClass.getDeclaredClasses())
        }
        if (companionObject != null)
            classes.addAll(companionObject!!.getDeclaredClasses())
        return classes
    }

    fun hasOpenMethod(returnType: Type, parameterTypes: List<Type>, name: String) =
        methods.any { m -> m.returnType == returnType && m.getParameterTypes() == parameterTypes && m.name == name && m.hasModifier { mod -> mod == FunctionModifier.OPEN || mod == FunctionModifier.ABSTRACT } } ||
                operators.any { m -> m.returnType == returnType && m.getParameterTypes() == parameterTypes && m.name == name && m.hasModifier { mod -> mod == FunctionModifier.OPEN || mod == FunctionModifier.ABSTRACT } }

    fun isGeneric() =
        primaryConstructor.isGeneric() || secondaryConstructors.any { c -> c.isGeneric() } || fields.any { it.type == NULLABLE_GENERIC || it.type == GENERIC }

    fun getPrimaryConstructor() = primaryConstructor

    fun setPrimaryConstructor(primaryConstructor: PrimaryConstructorFeature) {
        if (isInterface || isFunctionalInterface) return

        this.primaryConstructor = primaryConstructor
        primaryConstructor.getNotEmptyParameters().forEach { parameter ->

            val feature = FieldFeature(
                parameter.getType(),
                if (parameter.mutability == MutabilityQualifier.VARARG) MutabilityQualifier.VAL else parameter.mutability,
                parameter.name!!,
                isLazy = false,
                isDefaultValue = false,
                isHidden = true,
                isGetterSetter = false,
                extends = null
            )
            parameter.annotations.forEach { feature.addAnnotation(it) }
            parameter.modifiers.forEach { feature.addModifier(it) }
            feature.addParameter(ParameterFeature(Type(
                name, "$name(${
                    primaryConstructor.parameters.joinToString(", ") { "${it.name}=${it.getType().defaultValue}" }
                })"), MutabilityQualifier.VAL, null, false, varargVal))
            if (feature.extends != null)
                throw IllegalStateException("Extension field is impossible as a constructor argument")
            hiddenFields.add(feature)

        }
    }

    fun setCompanionObject(companionObject: CompanionObjectFeature) {
        this.companionObject = companionObject
    }

    private fun parameterMatch(a: List<ParameterFeature>, b: List<ParameterFeature>): Boolean {
        var aIndex = 0
        var bIndex = 0
        while (aIndex < a.size && bIndex < b.size) {
            if (a[aIndex].getType() != b[bIndex].getType()) return false
            if (a[aIndex].mutability == MutabilityQualifier.VARARG) {
                while (bIndex < b.size && b[bIndex].getType() == a[aIndex].getType()) bIndex += 1
                bIndex -= 1
            }
            if (b[bIndex].mutability == MutabilityQualifier.VARARG) {
                while (aIndex < a.size && b[bIndex].getType() == a[aIndex].getType()) aIndex += 1
                aIndex -= 1
            }
            aIndex += 1
            bIndex += 1
        }
        return aIndex == a.size && bIndex == b.size
    }

    fun addSecondaryConstructor(secondaryConstructor: SecondaryConstructorFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION } || isInterface || isFunctionalInterface) return
        if (parameterMatch(secondaryConstructor.parameters, primaryConstructor.parameters) ||
            secondaryConstructors.any { parameterMatch(it.parameters, secondaryConstructor.parameters) } ||
            hasModifier { it == ClassModifier.ENUM } && secondaryConstructor.hasModifier { it == ConstructorModifier.PUBLIC || it == ConstructorModifier.PROTECTED || it == ConstructorModifier.INTERNAL }
        ) return
        secondaryConstructors.add(secondaryConstructor)
    }

    fun addField(field: FieldFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (fields.any { f -> f.name == field.name } ||
            isInterface && field.isLazy ||
            isInterface && !field.isDefaultValue ||
            isFunctionalInterface && !field.isDefaultValue ||
            isFunctionalInterface && field.isLazy) return

        field.addParameter(ParameterFeature(possibleTypes[name]!!, MutabilityQualifier.VAL, null, false, varargVal))
        if (field.extends != null)
            field.addParameter(
                ParameterFeature(
                    possibleTypes[field.extends.name]!!,
                    MutabilityQualifier.VAL,
                    null,
                    false,
                    varargVal
                )
            )
        fields.add(field)
    }

    fun addMethod(method: MethodFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (isFunctionalInterface && methods.size + operators.size == 1) return
        if (methods.any { f -> f.name == method.name && f.getParameterTypes() == method.getParameterTypes() } ||
            method.hasModifier { m -> m == FunctionModifier.ABSTRACT } && !hasModifier { m -> m == ClassModifier.ABSTRACT } ||
            (isInterface || isFunctionalInterface) && !method.hasModifier { m -> m == FunctionModifier.ABSTRACT }) return
        method.addHiddenParameter(ParameterFeature(possibleTypes[name]!!, MutabilityQualifier.VAL, null, false, varargVal))
        if (method.extends != null) {
            method.addHiddenParameter(
                ParameterFeature(
                    possibleTypes[method.extends.name]!!,
                    MutabilityQualifier.VAL,
                    null,
                    false,
                    varargVal
                )
            )
        }

        methods.add(method)
    }

    fun addOperator(operator: OperatorFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (isFunctionalInterface && methods.size + operators.size == 1) return
        if (operators.any { op -> op.name == operator.name && op.getParameterTypes() == operator.getParameterTypes() } ||
            operator.hasModifier { m -> m == FunctionModifier.ABSTRACT } && !hasModifier { m -> m == ClassModifier.ABSTRACT } ||
            (isInterface || isFunctionalInterface) && !operator.hasModifier { m -> m == FunctionModifier.ABSTRACT }) return
        operator.addHiddenParameter(ParameterFeature(possibleTypes[name]!!, MutabilityQualifier.VAL, null, false, varargVal))
        if (operator.extends != null)
            operator.addHiddenParameter(
                ParameterFeature(
                    possibleTypes[operator.extends.name]!!,
                    MutabilityQualifier.VAL,
                    null,
                    false,
                    varargVal
                )
            )
        operators.add(operator)
    }

    fun addSuperClass(superClass: ClassFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (isInterface && superClass.isInterface || isFunctionalInterface && superClass.isFunctionalInterface ||
            isFunctionalInterface && superClass.isInterface || isInterface && superClass.isFunctionalInterface
        ) {
            if (name == superClass.name || superClasses.any { c -> c.name == superClass.name } || !superClass.hasModifier { m -> m == ClassModifier.OPEN } ||
                superClass.hasModifier { m -> m == ClassModifier.PRIVATE || m == ClassModifier.INTERNAL } && hasModifier { m -> m == ClassModifier.PUBLIC || m == ClassModifier.PROTECTED } ||
                superClass.hasModifier { m -> m == ClassModifier.PROTECTED } && (hasModifier { m -> m == ClassModifier.PUBLIC } || !hasModifier { m -> m.modifierType == ModifierType.VISIBILITY })
            ) return
            superClasses.add(superClass)
        }
    }

    fun addInnerClass(innerClass: ClassFeature) {
        if (name == innerClass.name || innerClasses.any { c -> c.name == innerClass.name }) return
        if ((isInterface || isFunctionalInterface || hasModifier { it == ClassModifier.ANNOTATION }) &&
            innerClass.hasModifier { m -> m == ClassModifier.INNER } ||
            hasModifier { it == ClassModifier.ANNOTATION } && innerClass.hasModifier { it == ClassModifier.PRIVATE || it == ClassModifier.INTERNAL || it == ClassModifier.PROTECTED }
        ) return
        innerClasses.add(innerClass)
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ClassModifier) throw IllegalArgumentException("Modifier must be a ClassModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            !isInner && modifier == ClassModifier.PROTECTED ||
            !isInner && modifier == ClassModifier.INNER ||
            modifier.modifierType == ModifierType.STRUCTURE && modifiers.any { m -> m.modifierType == ModifierType.INHERITANCE } ||
            modifier.modifierType == ModifierType.INHERITANCE && modifiers.any { m -> m.modifierType == ModifierType.STRUCTURE } ||
            modifier == ClassModifier.ANNOTATION && modifiers.any { m -> m.modifierType != ModifierType.VISIBILITY } ||
            modifiers.any { m -> m == ClassModifier.ANNOTATION } && modifier.modifierType != ModifierType.VISIBILITY ||
            isInterface && modifier.modifierType != ModifierType.VISIBILITY ||
            isFunctionalInterface && modifier.modifierType != ModifierType.VISIBILITY
        ) return

        modifiers.add(modifier)
    }

    override fun toShortString() = name

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val secondaryConstructorsString = secondaryConstructors.joinToString("\n") {  if (hasModifier { it == ClassModifier.VALUE }) it.toStringWithoutBody() else it.toString() }
        val fieldsString =
            fields.joinToString("\n") 
        val methodsString = methods.joinToString("\n")
        val operatorsString = operators.joinToString("\n")
        val superClassesListString = superClasses.joinToString(", ") { "${it.name}${if(isClassInterface[it.name] == true) "" else "()"}" }
        val superClassesString = superClasses.joinToString("\n")
        val innerClassesString = innerClasses.joinToString("\n")
        val genericString = if (isGeneric()) {
            when (variance) {
                TypeVariance.IN -> "<in T: Any>"
                TypeVariance.OUT -> "<out T: Any>"
                TypeVariance.INVARIANT -> "<T: Any>"
            }
        } else ""
        val enumConstants = if (hasModifier { m -> m == ClassModifier.ENUM }) "A_(${
            primaryConstructor.getParameterTypes().joinToString(separator = ", ") { t -> t.defaultValue }
        }), B_(${
            primaryConstructor.getParameterTypes().joinToString(separator = ", ") { t -> t.defaultValue }
        }), C_(${
            primaryConstructor.getParameterTypes().joinToString(separator = ", ") { t -> t.defaultValue }
        });" else ""

        return "${if (hasModifier { m -> m == ClassModifier.VALUE }) "@JvmInline" else ""}\n$annotationsString\n$superClassesString\n$modifiersString ${if (isFunctionalInterface) "fun interface" else if (isInterface) "interface" else "class"} ${
            name.split(
                '.'
            ).last()
        } $genericString ${
            if (isInterface || isFunctionalInterface) "" else "(${
                primaryConstructor.parameters.joinToString(
                    ", "
                ) {
                    if (variance != TypeVariance.INVARIANT && it.mutability != MutabilityQualifier.EMPTY)
                        if (it.mutability == MutabilityQualifier.VARARG && varargVal)
                            "vararg val ${it.name}: @UnsafeVariance ${it.getType().typeName}"
                        else
                            it.toStringWithUnsafeVariance()
                    else
                        if (it.mutability == MutabilityQualifier.VARARG && varargVal)
                            "vararg val ${it.name}: ${it.getType().typeName}"
                        else
                            it.toString()
                }
            })"
        } ${if (superClassesListString != "") ": $superClassesListString" else ""} {\n$enumConstants\n$secondaryConstructorsString\n$fieldsString\n$methodsString\n$operatorsString\n$innerClassesString\n${companionObject ?: ""}\n}"
    }
}

val possibleTypes = mutableMapOf<String, Type>()
val isClassInterface = mutableMapOf<String, Boolean>()
private val classNames = mutableListOf<String>()
val qualifiedNameToFeature = mutableMapOf<String, Feature>()
val isTypeGeneric = mutableMapOf<Type, Boolean>()

private fun <T> FuzzedDataProvider.pickFromArray(arr: Array<T>): T? {
    if (arr.isEmpty())
        return null
    return arr[consumeInt(0, arr.lastIndex)]
}

private fun FuzzedDataProvider.generateNonAbstractType(): Type? {
    return pickFromArray(possibleTypes.values.filter { it in primitiveTypes || isClassInterface[it.typeName] == false }
        .toTypedArray())
}

private fun FuzzedDataProvider.generateMutabilityQualifier(): MutabilityQualifier {
    return pickFromArray(MutabilityQualifier.values())!!
}

private fun <T> FuzzedDataProvider.generateModifier(clazz: Class<T>) = when (clazz) {
    FunctionModifier::class.java -> pickFromArray(FunctionModifier.values())
    ParameterModifier::class.java -> pickFromArray(ParameterModifier.values())
    ConstructorModifier::class.java -> pickFromArray(ConstructorModifier.values())
    ClassModifier::class.java -> pickFromArray(ClassModifier.values())
    else -> throw IllegalArgumentException("Class must be a Modifier")
}

private fun generateName(): String {
    val result = StringBuilder()
    val length = Random.nextInt(2, MAX_STR_LENGTH)

    while (result.length < length) {
        result.append('A' + Random.nextInt(0, 26))
    }

    return result.toString()
}

private fun generateClassName(namePrefix: String): String {
    var name = generateName()
    var count = 0
    while (classNames.contains("$namePrefix$name")) {
        if (count >= MAX_TRIES) {
            throw FuzzingStateException("Can't generate class name")
        }
        count += 1
        name = generateName()
    }
    return name
}

private fun FuzzedDataProvider.generateMethodFeature(
    declaredClasses: List<Pair<String, ClassFeature>>,
    isClassGeneric: Boolean,
    superClasses: List<ClassFeature>,
    availableAnnotations: List<String>
): MethodFeature? {
    val classToExtend = pickFromArray(declaredClasses.filter { !isClassInterface[it.first]!! }.toTypedArray())
    val returnType = generateNonAbstractType() ?: return null
    if (returnType == GENERIC) return null
    val methodFeature = MethodFeature(
        consumeBoolean(),
        !isClassGeneric || consumeBoolean(),
        superClasses,
        returnType,
        generateName(),
        if (consumeBoolean() && classToExtend != null) classToExtend.second else null
    )

    val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
    for (i in 0 until parametersNumber) {
        val param = generateParameterFeature(availableAnnotations, consumeBoolean(), false) ?: continue
        methodFeature.addParameter(param)
    }

    val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
    for (i in 0 until modifiersNumber) {
        methodFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
    }

    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            methodFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
        }
    }

    return methodFeature
}

private val operatorNames = listOf(
    "unaryPlus",
    "unaryMinus",
    "not",
    "inc",
    "dec",
    "plus",
    "minus",
    "times",
    "div",
    "rem",
    "rangeTo",
    "compareTo",
    "plusAssign",
    "minusAssign",
    "timesAssign",
    "divAssign",
    "remAssign",
    "get",
    "set",
    "invoke",
    "contains",
    "component"
)

private fun FuzzedDataProvider.generateOperatorFeature(
    declaredClasses: List<Pair<String, ClassFeature>>,
    isClassGeneric: Boolean,
    superClasses: List<ClassFeature>,
    availableAnnotations: List<String>,
    className: String,
    operatorName: String,
    classFeature: ClassFeature
): OperatorFeature? {
    if (classFeature.hasModifier { it == ClassModifier.ANNOTATION })
        return null

    val classToExtend = pickFromArray(declaredClasses.filter { !isClassInterface[it.first]!! }.toTypedArray())
    when (operatorName) {
        "unaryPlus", "unaryMinus", "not", "inc", "dec" -> {
            if (isClassInterface[className] == true) return null

            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= this",
                possibleTypes[className]!!,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "plus", "minus", "times", "div", "rem", "rangeTo" -> {
            if (isClassInterface[className] == true) return null
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= this",
                possibleTypes[className]!!,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for binary operator")
                val parameter =
                    generateParameterFeature(availableAnnotations, false, false, possibleTypes[className]!!) ?: continue
                if (parameter.mutability != MutabilityQualifier.VARARG) {
                    operatorFeature.addParameter(parameter)
                }
                count += 1
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "compareTo" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= 0",
                INT,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for compareTo")
                val parameter =
                    generateParameterFeature(availableAnnotations, false, false, possibleTypes[className]!!) ?: continue
                if (parameter.mutability != MutabilityQualifier.VARARG) {
                    operatorFeature.addParameter(parameter)
                }
                count += 1
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for binary assignment")
                val parameter =
                    generateParameterFeature(availableAnnotations, false, false, possibleTypes[className]!!) ?: continue
                if (parameter.mutability != MutabilityQualifier.VARARG) {
                    operatorFeature.addParameter(parameter)
                }
                count += 1
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "invoke" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
            for (i in 0 until parametersNumber) {
                val param = generateParameterFeature(availableAnnotations, consumeBoolean(), false) ?: continue
                operatorFeature.addParameter(param)
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "get" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            var count = 0
            while (operatorFeature.getParametersNumber() < 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for get operator")

                val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
                for (i in 0 until parametersNumber) {
                    val param = generateParameterFeature(availableAnnotations, consumeBoolean(), false) ?: continue
                    operatorFeature.addParameter(param)
                }
                count += 1
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "set" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            var count = 0
            while (operatorFeature.getParametersNumber() < 2) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for set operator")

                val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
                for (i in 0 until parametersNumber) {
                    val param = generateParameterFeature(availableAnnotations, consumeBoolean(), false) ?: continue
                    operatorFeature.addParameter(param)
                }

                while (operatorFeature.getParametersNumber() >= 2 && operatorFeature.parameters.last().mutability == MutabilityQualifier.VARARG)
                    operatorFeature.parameters.drop(1)

                count += 1
            }
            operatorFeature.parameters.last().withDefaultValue = false

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "contains" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= true",
                BOOLEAN,
                operatorName,
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for contains operator")
                val parameter = generateParameterFeature(availableAnnotations, false, false) ?: continue
                operatorFeature.addParameter(parameter)
                count += 1
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        "component" -> {
            val operatorFeature = OperatorFeature(
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                "$operatorName${consumeInt(1, 10)}",
                extends = if (consumeBoolean() && classToExtend != null) classToExtend.second else null
            )

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
                }
            }

            return operatorFeature
        }

        else -> throw IllegalArgumentException("name must be an operator")
    }
}

private fun FuzzedDataProvider.generateParameterFeature(
    availableAnnotations: List<String>,
    withDefaultValue: Boolean,
    varargVal: Boolean,
    type: Type? = null,
): ParameterFeature? {
    try {
        val parameterFeature = ParameterFeature(
            type ?: generateNonAbstractType() ?: return null,
            generateMutabilityQualifier(),
            generateName(),
            withDefaultValue,
            varargVal
        )
        val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
        for (i in 0 until modifiersNumber) {
            parameterFeature.addModifier(generateModifier(ParameterModifier::class.java) as Modifier)
        }

        if (availableAnnotations.isNotEmpty()) {
            val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
            for (i in 0 until annotationsNumber) {
                parameterFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
            }
        }
        return parameterFeature
    } catch (e: IllegalArgumentException) {
        return null
    }
}

private fun FuzzedDataProvider.generateSecondaryConstructorFeature(
    isClassSealed: Boolean,
    parameterTypes: List<ParameterFeature>,
    availableAnnotations: List<String>
): SecondaryConstructorFeature {
    val secondaryConstructorFeature = SecondaryConstructorFeature(isClassSealed, parameterTypes)

    val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
    for (i in 0 until parametersNumber) {
        val param = generateParameterFeature(availableAnnotations, true, false) ?: continue
        secondaryConstructorFeature.addParameter(param)
    }

    val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
    for (i in 0 until modifiersNumber) {
        secondaryConstructorFeature.addModifier(generateModifier(ConstructorModifier::class.java) as Modifier)
    }

    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            secondaryConstructorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
        }
    }

    return secondaryConstructorFeature
}

private fun FuzzedDataProvider.generatePrimaryConstructorFeature(
    minArguments: Int,
    maxArguments: Int,
    availableAnnotations: List<String>,
    isAnnotation: Boolean,
    isValue: Boolean,
    varargVal: Boolean
): PrimaryConstructorFeature {
    val primaryConstructorFeature = PrimaryConstructorFeature(isAnnotation, isValue, varargVal = varargVal)

    val parametersNumber = consumeInt(minArguments, maxArguments)
    var count = 0
    while (primaryConstructorFeature.parameters.size != parametersNumber) {
        count += 1
        if (count > MAX_TRIES) {
            throw FuzzingStateException("Can't generate enough arguments for primary constructor")
        }
        val param = generateParameterFeature(availableAnnotations, true, !varargVal) ?: continue
        if ((param.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
                    param.mutability == MutabilityQualifier.EMPTY || param.mutability == MutabilityQualifier.VARARG)
            && minArguments != 0) continue
        primaryConstructorFeature.addParameter(param)
    }
    return primaryConstructorFeature
}

private fun FuzzedDataProvider.generateFieldFeature(
    availableAnnotations: List<String>,
    declaredClasses: List<Pair<String, ClassFeature>>,
    thisClass: String,
    isValue: Boolean
): FieldFeature? {
    try {
        val classToExtend = pickFromArray(declaredClasses.filter { !isClassInterface[it.first]!! }.toTypedArray())
        val type = generateNonAbstractType() ?: return null
        val fieldFeature = FieldFeature(
            type,
            generateMutabilityQualifier(),
            generateName(),
            consumeBoolean(),
            if (isValue || thisClass == type.typeName) false else consumeBoolean(),
            if (isValue || thisClass == type.typeName) true else consumeBoolean(),
            false,
            if (consumeBoolean() && classToExtend != null) classToExtend.second else null
        )
        val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
        for (i in 0 until modifiersNumber) {
            fieldFeature.addModifier(generateModifier(ParameterModifier::class.java) as Modifier)
        }

        if (availableAnnotations.isNotEmpty()) {
            val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
            for (i in 0 until annotationsNumber) {
                fieldFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
            }
        }
        return fieldFeature
    } catch (e: IllegalArgumentException) {
        return null
    }
}

private fun FuzzedDataProvider.generateCompanionObject(
    declaredClasses: List<Pair<String, ClassFeature>>,
    maxDepth: Int,
    availableAnnotations: List<String>,
    namePrefix: String
): CompanionObjectFeature {
    val companionObjectFeature = CompanionObjectFeature()

    if (maxDepth != 0) {
        val innerClassesNumber = consumeInt(0, MAX_INNER_CLASSES_NUMBER)
        for (i in 0 until innerClassesNumber) {
            val innerClass =
                generateClassFeature(maxDepth - 1, consumeBoolean(), availableAnnotations, "${namePrefix}Companion.")
            companionObjectFeature.addInnerClass(innerClass)
        }
    }

    val fieldsNumber = consumeInt(0, MAX_FIELDS_NUMBER)
    for (i in 0 until fieldsNumber) {
        val field = generateFieldFeature(availableAnnotations, declaredClasses, "", false) ?: continue
        companionObjectFeature.addField(field)
    }

    val methodsNumber = consumeInt(0, MAX_METHODS_NUMBER)
    for (i in 0 until methodsNumber) {
        val method = generateMethodFeature(
            declaredClasses,
            false,
            emptyList(),
            availableAnnotations
        ) ?: continue
        companionObjectFeature.addMethod(method)
    }

    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            companionObjectFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
        }
    }

    return companionObjectFeature
}

private fun FuzzedDataProvider.generateClassFeature(
    maxDepth: Int,
    isAnnotation: Boolean,
    availableAnnotations: List<String>,
    namePrefix: String
): ClassFeature {
    val name = generateClassName(namePrefix)
    classNames.add("$namePrefix$name")

    val classFeature = ClassFeature(
        if (maxDepth != MAX_DEPTH) consumeBoolean() else false,
        if (maxDepth != MAX_DEPTH) consumeBoolean() else false,
        if (maxDepth != MAX_DEPTH) consumeBoolean() else false,
        pickFromArray(TypeVariance.values())!!,
        "$namePrefix$name",
        consumeBoolean()
    )

    if (isAnnotation) classFeature.addModifier(ClassModifier.ANNOTATION)

    val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
    for (i in 0 until modifiersNumber) {
        val modifier = generateModifier(ClassModifier::class.java) as Modifier
        if (maxDepth == MAX_DEPTH && modifier == ClassModifier.ABSTRACT) continue
        classFeature.addModifier(modifier)
    }

    val superClasses: MutableList<ClassFeature> = mutableListOf()
    if (!classFeature.hasModifier { it == ClassModifier.ANNOTATION }) {
        val superClassesNumber = consumeInt(0, MAX_SUPERCLASSES_NUMBER)
        for (i in 0 until superClassesNumber) {
            val superClass = generateClassFeature(maxDepth, false, availableAnnotations, namePrefix)
            superClasses.add(superClass)
            classFeature.addSuperClass(superClass)
        }
    }

    val primaryConstructor =
        generatePrimaryConstructorFeature(
            if (classFeature.hasModifier { m -> m == ClassModifier.DATA || m == ClassModifier.VALUE }) 1 else 0,
            if (classFeature.hasModifier { m -> m == ClassModifier.VALUE }) 1 else MAX_PARAMETERS_NUMBER,
            availableAnnotations,
            classFeature.hasModifier { m -> m == ClassModifier.ANNOTATION },
            classFeature.hasModifier { m -> m == ClassModifier.VALUE },
            classFeature.varargVal
        )
    classFeature.setPrimaryConstructor(primaryConstructor)

    val constructorsNumber = consumeInt(0, MAX_CONSTRUCTORS_NUMBER)
    for (i in 0 until constructorsNumber) {
        classFeature.addSecondaryConstructor(
            generateSecondaryConstructorFeature(
                classFeature.hasModifier { m -> m == ClassModifier.SEALED },
                primaryConstructor.parameters,
                availableAnnotations
            )
        )
    }

    if (!classFeature.hasModifier { it == ClassModifier.ANNOTATION })
        possibleTypes["$namePrefix$name"] = Type("$namePrefix$name",
            if (classFeature.isInterface || classFeature.isFunctionalInterface) "" else "$namePrefix$name(${
                primaryConstructor.parameters.joinToString(", ") { "${it.name}=${it.getType(false).defaultValue}" }
            })")
    isClassInterface["$namePrefix$name"] = classFeature.isInterface || classFeature.isFunctionalInterface

    if (maxDepth != 0) {
        val innerClassesNumber = consumeInt(0, MAX_INNER_CLASSES_NUMBER)
        for (i in 0 until innerClassesNumber) {
            val innerClass =
                generateClassFeature(maxDepth - 1, consumeBoolean(), availableAnnotations, "$namePrefix$name.")
            classFeature.addInnerClass(innerClass)
        }
    }

    val declaredClasses = classFeature.getDeclaredClasses()

    val fieldsNumber = consumeInt(0, MAX_FIELDS_NUMBER)
    for (i in 0 until fieldsNumber) {
        val field = generateFieldFeature(availableAnnotations, declaredClasses, classFeature.name, classFeature.hasModifier { it == ClassModifier.VALUE }) ?: continue
        classFeature.addField(field)
    }

    if (possibleTypes["$namePrefix$name"] != null)
        isTypeGeneric[possibleTypes["$namePrefix$name"]!!] = classFeature.isGeneric()

    var count = 0
    while (true) {
        val methodsNumber = consumeInt(0, MAX_METHODS_NUMBER)
        for (i in 0 until methodsNumber) {
            val method = generateMethodFeature(
                declaredClasses,
                classFeature.isGeneric(),
                superClasses,
                availableAnnotations
            ) ?: continue
            classFeature.addMethod(method)
        }

        val operatorsNumber = consumeInt(0, MAX_OPERATORS_NUMBER)
        for (i in 0 until operatorsNumber) {
            val operatorName = pickFromArray(operatorNames.toTypedArray())!!
            val operator = generateOperatorFeature(
                declaredClasses,
                classFeature.isGeneric(),
                superClasses,
                availableAnnotations,
                "$namePrefix$name",
                operatorName,
                classFeature
            ) ?: continue
            classFeature.addOperator(operator)
        }

        if (!classFeature.isFunctionalInterface) break
        if (classFeature.methodsNumber() == 1) break
        if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate method for fun interface")
        count += 1
    }

    if (consumeBoolean()) {
        val companionObject = generateCompanionObject(
            declaredClasses,
            maxDepth,
            availableAnnotations,
            "$namePrefix$name."
        )
        classFeature.setCompanionObject(companionObject)
    }


    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            classFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray())!!)
        }
    }

    return classFeature
}

fun FuzzedDataProvider.generateSourceCode(): Pair<String, String> {
    classNames.clear()
    qualifiedNameToFeature.clear()
    possibleTypes.clear()
    primitiveTypes.forEach {
        possibleTypes[it.typeName] = it
    }
    isTypeGeneric.clear()
    isClassInterface.clear()
    val annotations = mutableListOf<ClassFeature>()
//    for (i in 0 until MAX_ANNOTATIONS_NUMBER) {
//        annotations.add(generateClassFeature(MAX_DEPTH, true, annotations.map { a -> a.name }, ""))
//    }

    val classFeature = generateClassFeature(MAX_DEPTH, false, annotations.map { a -> a.name }, "")

    for ((qualifiedClassName, declaredClass) in classFeature.getDeclaredClasses()) {
        for (feature in declaredClass.getFeatures()) {
            if (feature is ClassFeature)
                qualifiedNameToFeature[qualifiedClassName] = feature
            else
                qualifiedNameToFeature["$qualifiedClassName.${feature.toShortString()}"] = feature
        }
    }

    return classFeature.name to "${annotations.joinToString("\n")}\n$classFeature"
}

class FuzzingStateException(message: String) : RuntimeException(message)