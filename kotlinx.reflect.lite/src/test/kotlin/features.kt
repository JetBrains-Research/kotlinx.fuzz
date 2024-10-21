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

private val primitiveTypes =
    listOf(INT, LONG, DOUBLE, FLOAT, STRING, VOID, NULLABLE_GENERIC, GENERIC, LAMBDA, SUSPEND_LAMBDA)

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

abstract class Feature(open val name: String) {
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
) : Feature(name) {
    private val parameters = mutableListOf<ParameterFeature>()
    private var body: String = "{ return ${returnType.defaultValue} }"
    private val hiddenParameters = mutableListOf<ParameterFeature>()

    fun addHiddenParameter(parameter: ParameterFeature) {
        hiddenParameters.add(parameter)
    }

    override fun toShortString(): String {
        val parametersString = (hiddenParameters + parameters).joinToString(", ") { it.toShortString() }
        return "$name($parametersString)"
    }

    fun getParameterTypes() = parameters.map { p -> p.type }

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            !hasModifier { m -> m == FunctionModifier.INLINE } && parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.VISIBILITY } ||
            parameter.mutability == MutabilityQualifier.VAL || parameter.mutability == MutabilityQualifier.VAR) return
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
            modifier == FunctionModifier.INFIX && parameters.size != 1
        ) return

        if (modifier == FunctionModifier.TAILREC) {
            parameters.add(ParameterFeature(INT, MutabilityQualifier.EMPTY, "n_"))
            if (returnType != VOID) {
                parameters.add(ParameterFeature(returnType, MutabilityQualifier.EMPTY, "accumulator_"))
                body =
                    "{ return if (n_ <= 1) { accumulator_ } else { $name(${parameters.joinToString(", ") { if (it.name == "n_") "n_=n_ - 1" else "${it.name} = ${it.name}" }}) } }"
            } else {
                body =
                    "{ return if (n_ <= 1) { println(n) } else { $name(${parameters.joinToString(", ") { if (it.name == "n_") "n_=n_ - 1" else "${it.name} = ${it.name}" }}) } }"
            }
        }

        if (modifier == FunctionModifier.ABSTRACT) {
            body = ""
        }

        modifiers.add(modifier)
    }

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val parametersString = parameters.joinToString(", ") { it.toString() }
        val genericString =
            if ((returnType == NULLABLE_GENERIC || returnType == GENERIC || parameters.any { it.type == NULLABLE_GENERIC || it.type == GENERIC }) && genericIfNeeded) {
                if (hasModifier { m -> m == FunctionModifier.INLINE } && reifiedIfNeeded) "<reified T: Any>"
                else "<T: Any>"
            } else ""
        return "$annotationsString\n$modifiersString fun $genericString $name($parametersString) ${if (returnType == VOID) "" else ": ${returnType.typeName}"} $body"

    }
}

class OperatorFeature(
    val extends: String?,
    private val reifiedIfNeeded: Boolean,
    private val genericIfNeeded: Boolean,
    private val superClasses: List<ClassFeature>,
    private var body: String,
    val returnType: Type,
    override val name: String,
) : Feature(name) {
    private val parameters = mutableListOf<ParameterFeature>()
    private val hiddenParameters = mutableListOf<ParameterFeature>()

    fun addHiddenParameter(parameter: ParameterFeature) {
        hiddenParameters.add(parameter)
    }

    override fun toShortString(): String {
        val parametersString = (hiddenParameters + parameters).joinToString(", ") { it.toShortString() }
        return "${if (extends != null) "$extends." else ""}$name($parametersString)"
    }

    fun getParameterTypes() = parameters.map { p -> p.type }

    fun getParametersNumber() = parameters.size

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            !hasModifier { m -> m == FunctionModifier.INLINE } && parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.VISIBILITY } ||
            parameter.mutability == MutabilityQualifier.VAL || parameter.mutability == MutabilityQualifier.VAR) return
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
            modifier == FunctionModifier.INFIX && parameters.size != 1
        ) return

        if (modifier == FunctionModifier.TAILREC) {
            parameters.add(ParameterFeature(INT, MutabilityQualifier.EMPTY, "n_"))
            if (returnType != VOID) {
                parameters.add(ParameterFeature(returnType, MutabilityQualifier.EMPTY, "accumulator_"))
                body =
                    " = if (n <= 1) { accumulator_ } else { $name(${parameters.joinToString(", ") { if (it.name == "n_") "n_=n_ - 1" else "${it.name} = ${it.name}" }}) }"
            } else {
                parameters.add(ParameterFeature(INT, MutabilityQualifier.EMPTY, "accumulator_"))
                body =
                    " = if (n <= 1) { println(accumulator_) } else { $name(${parameters.joinToString(", ") { if (it.name == "n_") "n_=n_ - 1" else "${it.name} = ${it.name}" }}) }"
            }
        }

        if (modifier == FunctionModifier.ABSTRACT) {
            body = ""
        }

        modifiers.add(modifier)
    }

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val parametersString = parameters.joinToString(", ") { it.toString() }
        val genericString =
            if ((returnType == NULLABLE_GENERIC || returnType == GENERIC || parameters.any { it.type == NULLABLE_GENERIC || it.type == GENERIC }) && genericIfNeeded) {
                if (hasModifier { m -> m == FunctionModifier.INLINE } && reifiedIfNeeded) "<reified T: Any>"
                else "<T: Any>"
            } else ""
        return "$annotationsString\n$modifiersString operator fun $genericString ${if (extends != null) "$extends." else ""}$name($parametersString) $body"

    }
}

class ParameterFeature(val type: Type, val mutability: MutabilityQualifier, override val name: String) :
    Feature(name) {
    init {
        if (type == VOID) {
            throw IllegalArgumentException("Parameter type must not be VOID")
        }
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ParameterModifier) throw IllegalArgumentException("Modifier must be a ParameterModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            mutability == MutabilityQualifier.EMPTY && modifier.modifierType == ModifierType.VISIBILITY ||
            type != LAMBDA && modifier.modifierType == ModifierType.INLINE ||
            type != SUSPEND_LAMBDA && modifier.modifierType == ModifierType.INLINE ||
            modifier == ParameterModifier.LATEINIT
        ) return

        modifiers.add(modifier)
    }

    override fun toShortString() = type.typeName

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        return if (type.defaultValue != "" && mutability != MutabilityQualifier.VARARG)
            "$annotationsString $modifiersString ${mutability.qualifier} $name: ${type.typeName} = ${type.defaultValue}"
        else
            "$annotationsString $modifiersString ${mutability.qualifier} $name: ${type.typeName}"
    }
}

class SecondaryConstructorFeature(
    private val isClassSealed: Boolean,
    private val parameterTypes: List<Type>,
    override var name: String = ""
) : Feature(name) {
    internal val parameters = mutableListOf<ParameterFeature>()
    private var body = "{}"

    fun isGeneric() = parameters.any { p -> p.type == NULLABLE_GENERIC || p.type == GENERIC }

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.VISIBILITY } ||
            parameter.mutability != MutabilityQualifier.EMPTY) return
        parameters.add(parameter)
        name = "(${parameters.joinToString(", ") { it.type.typeName }})"
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ConstructorModifier) throw IllegalArgumentException("Modifier must be a ConstructorModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } ||
            isClassSealed && modifier == ConstructorModifier.PUBLIC
        ) return

        modifiers.add(modifier)
        name = "(${parameters.joinToString(", ") { it.type.typeName }})"
    }

    override fun toShortString(): String {
        val parametersString = parameters.joinToString(", ") { it.toShortString() }
        return "($parametersString)"
    }

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        val primaryParametersString = parameterTypes.joinToString(", ") { it.defaultValue }
        val parametersString = parameters.joinToString(", ") { it.toString() }
        return "$annotationsString\n$modifiersString constructor ($parametersString) : this($primaryParametersString) $body"
    }
}

class PrimaryConstructorFeature(private val isAnnotation: Boolean, override var name: String = "") : Feature(name) {
    internal val parameters = mutableListOf<ParameterFeature>()

    fun getNotEmptyParameters() = parameters.filterNot { it.mutability == MutabilityQualifier.EMPTY }

    fun getParameterTypes() = parameters.map { p -> p.type }

    fun isGeneric() = parameters.any { p -> p.type == NULLABLE_GENERIC || p.type == GENERIC }

    fun addParameter(parameter: ParameterFeature) {
        if (parameters.any { p -> p.name == parameter.name } ||
            parameter.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
            isAnnotation && parameter.mutability != MutabilityQualifier.VAL ||
            isAnnotation && parameter.hasModifier { it == ParameterModifier.INTERNAL || it == ParameterModifier.PROTECTED || it == ParameterModifier.PRIVATE } ||
            isAnnotation && (parameter.type == LAMBDA || parameter.type == SUSPEND_LAMBDA || !primitiveTypes.contains(
                parameter.type
            )) ||
            isAnnotation && (parameter.type == NULLABLE_GENERIC) ||
            isAnnotation && (parameter.type == GENERIC)) return
        parameters.add(parameter)
        name = "(${parameters.joinToString(", ") { it.type.typeName }})"
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
    private val mutability: MutabilityQualifier,
    override val name: String,
    val isLazy: Boolean,
    val isGetterSetter: Boolean
) : Feature(name) {
    init {
        if (type == VOID || mutability == MutabilityQualifier.EMPTY || mutability == MutabilityQualifier.VARARG) {
            throw IllegalArgumentException("Field must not be VOID, EMPTY, VARARG or not nullable generic")
        }
        if (type == GENERIC && mutability == MutabilityQualifier.VAR) {
            addModifier(ParameterModifier.LATEINIT)
        } else if (type == GENERIC) {
            throw IllegalArgumentException("Field must not be VAL and non-nullable generic")
        }
    }

    override fun addModifier(modifier: Modifier) {
        if (modifier !is ParameterModifier) throw IllegalArgumentException("Modifier must be a ParameterModifier")

        if ((modifier.modifierType != ModifierType.DEFAULT || hasModifier { m -> m == modifier }) &&
            hasModifier { m -> m.modifierType == modifier.modifierType } || modifier.modifierType == ModifierType.INLINE ||
            modifier == ParameterModifier.LATEINIT && primitiveTypes.contains(type) ||
            modifier == ParameterModifier.LATEINIT && mutability != MutabilityQualifier.VAL ||
            mutability == MutabilityQualifier.EMPTY ||
            mutability == MutabilityQualifier.VARARG
        )
            return

        modifiers.add(modifier)
    }

    override fun toShortString() = name

    override fun toString(): String {
        val annotationsString = annotations.joinToString("\n") { "@$it" }
        val modifiersString = modifiers.joinToString(" ") { it.modifierName }
        return if (type == STRING && isLazy)
            "$annotationsString\n$modifiersString ${mutability.qualifier} $name: ${type.typeName} by lazy { ${type.defaultValue} }"
        else if (isGetterSetter && !name.contains('.'))
            "$annotationsString\n$modifiersString ${mutability.qualifier} $name: ${type.typeName} = ${type.defaultValue}"
        else if (mutability == MutabilityQualifier.VAL)
            "$annotationsString\n$modifiersString ${mutability.qualifier} $name: ${type.typeName}\nget() = ${type.defaultValue}"
        else
            "$annotationsString\n$modifiersString ${mutability.qualifier} $name: ${type.typeName}\nget() = ${type.defaultValue}\nset(value) {}"
    }
}

class CompanionObjectFeature(override val name: String = "Companion") : Feature(name) {
    private val fields: MutableList<FieldFeature> = mutableListOf()
    private val methods: MutableList<MethodFeature> = mutableListOf()
    private val innerClasses: MutableList<ClassFeature> = mutableListOf()

    fun getDeclaredClasses(): List<Pair<String, ClassFeature>> {
        val classes = mutableListOf<Pair<String, ClassFeature>>()
        for (innerClass in innerClasses) {
            classes.addAll(innerClass.getDeclaredClasses().map { s -> "$name.${s.first}" to s.second })
        }
        return classes
    }

    fun addField(field: FieldFeature) {
        if (fields.any { f -> f.name == field.name }) return
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

class ClassFeature(
    val isInterface: Boolean,
    val isFunctionalInterface: Boolean,
    private val isInner: Boolean,
    override val name: String
) : Feature(name) {
    private var primaryConstructor = PrimaryConstructorFeature(false)
    private var companionObject: CompanionObjectFeature? = null
    private val secondaryConstructors: MutableList<SecondaryConstructorFeature> = mutableListOf()
    private val fields: MutableList<FieldFeature> = mutableListOf()
    private val hiddenFields: MutableList<FieldFeature> = mutableListOf()
    private val methods: MutableList<MethodFeature> = mutableListOf()
    private val operators: MutableList<OperatorFeature> = mutableListOf()
    private val superClasses: MutableList<ClassFeature> = mutableListOf()
    private val innerClasses: MutableList<ClassFeature> = mutableListOf()

    fun methodsNumber() = methods.size + operators.size

    fun getFeatures() =
        listOf(this as Feature) + (if (isInterface || isFunctionalInterface) emptyList() else listOf(primaryConstructor as Feature)) + (if (companionObject == null) emptyList() else listOf(
            companionObject as Feature
        )) + (fields as List<Feature>) + (hiddenFields as List<Feature>) + (methods as List<Feature>) + (operators as List<Feature>)

    fun getDeclaredClasses(): List<Pair<String, ClassFeature>> {
        val classes = mutableListOf(name to this)
        for (superClass in superClasses) {
            classes.addAll(superClass.getDeclaredClasses())
        }
        for (innerClass in innerClasses) {
            classes.addAll(innerClass.getDeclaredClasses().map { s -> "$name.${s.first}" to s.second })
        }
        if (companionObject != null) classes.addAll(
            companionObject!!.getDeclaredClasses().map { s -> "$name.${s.first}" to s.second })
        return classes
    }

    fun hasOpenMethod(returnType: Type, parameterTypes: List<Type>, name: String) =
        methods.any { m -> m.returnType == returnType && m.getParameterTypes() == parameterTypes && m.name == name && m.hasModifier { mod -> mod == FunctionModifier.OPEN || mod == FunctionModifier.ABSTRACT } } ||
                operators.any { m -> m.returnType == returnType && m.getParameterTypes() == parameterTypes && m.name == name && m.hasModifier { mod -> mod == FunctionModifier.OPEN || mod == FunctionModifier.ABSTRACT } }

    fun isGeneric() =
        primaryConstructor.isGeneric() || secondaryConstructors.any { c -> c.isGeneric() } || fields.any { it.type == NULLABLE_GENERIC || it.type == GENERIC }

    fun getPrimaryConstructor() = primaryConstructor

    fun setPrimaryConstructor(primaryConstructor: PrimaryConstructorFeature) {
        this.primaryConstructor = primaryConstructor
        primaryConstructor.getNotEmptyParameters().forEach { parameter ->
            try {
                val feature = FieldFeature(parameter.type, parameter.mutability, parameter.name, false, false)
                parameter.annotations.forEach { feature.addAnnotation(it) }
                parameter.modifiers.forEach { feature.addModifier(it) }
                hiddenFields.add(feature)
            } catch (_: IllegalArgumentException) {

            }
        }
    }

    fun setCompanionObject(companionObject: CompanionObjectFeature) {
        this.companionObject = companionObject
    }

    private fun parameterMatch(a: List<ParameterFeature>, b: List<ParameterFeature>): Boolean {
        var aIndex = 0
        var bIndex = 0
        while (aIndex < a.size && bIndex < b.size) {
            if (a[aIndex].type != b[bIndex].type) return false
            if (a[aIndex].mutability == MutabilityQualifier.VARARG) {
                while (bIndex < b.size && b[bIndex].type == a[aIndex].type) bIndex += 1
                bIndex -= 1
            }
            if (b[bIndex].mutability == MutabilityQualifier.VARARG) {
                while (aIndex < a.size && b[bIndex].type == a[aIndex].type) aIndex += 1
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
            secondaryConstructors.any { parameterMatch(it.parameters, secondaryConstructor.parameters) }
        ) return
        secondaryConstructors.add(secondaryConstructor)
    }

    fun addField(field: FieldFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (fields.any { f -> f.name == field.name } ||
            isInterface && field.isLazy ||
            isInterface && !field.isGetterSetter ||
            isFunctionalInterface && !field.isGetterSetter ||
            isFunctionalInterface && field.isLazy) return

        fields.add(field)
    }

    fun addMethod(method: MethodFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (isFunctionalInterface && methods.size + operators.size == 1) return
        if (methods.any { f -> f.name == method.name && f.getParameterTypes() == method.getParameterTypes() } ||
            method.hasModifier { m -> m == FunctionModifier.ABSTRACT } && !hasModifier { m -> m == ClassModifier.ABSTRACT } ||
            (isInterface || isFunctionalInterface) && !method.hasModifier { m -> m == FunctionModifier.ABSTRACT }) return
        method.addHiddenParameter(ParameterFeature(possibleTypes[name]!!, MutabilityQualifier.VAL, "this"))
        if (method.name.contains('.'))
            method.addHiddenParameter(
                ParameterFeature(
                    possibleTypes[method.name.split('.')[0]]!!,
                    MutabilityQualifier.VAL,
                    "this"
                )
            )
        methods.add(method)
    }

    fun addOperator(operator: OperatorFeature) {
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (isFunctionalInterface && methods.size + operators.size == 1) return
        if (operators.any { op -> op.name == operator.name && op.getParameterTypes() == operator.getParameterTypes() } ||
            operator.hasModifier { m -> m == FunctionModifier.ABSTRACT } && !hasModifier { m -> m == ClassModifier.ABSTRACT } ||
            (isInterface || isFunctionalInterface) && !operator.hasModifier { m -> m == FunctionModifier.ABSTRACT }) return
        operator.addHiddenParameter(ParameterFeature(possibleTypes[name]!!, MutabilityQualifier.VAL, "this"))
        if (operator.extends != null)
            operator.addHiddenParameter(
                ParameterFeature(
                    possibleTypes[operator.extends]!!,
                    MutabilityQualifier.VAL,
                    "this"
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
        if (hasModifier { m -> m == ClassModifier.ANNOTATION }) return
        if (name == innerClass.name || innerClasses.any { c -> c.name == innerClass.name }) return
        if (isInterface && innerClass.hasModifier { m -> m == ClassModifier.INNER } ||
            isFunctionalInterface && innerClass.hasModifier { m -> m == ClassModifier.INNER }) return
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
        val secondaryConstructorsString = secondaryConstructors.joinToString("\n")
        val fieldsString = fields.joinToString("\n")
        val methodsString = methods.joinToString("\n")
        val operatorsString = operators.joinToString("\n")
        val superClassesListString = superClasses.joinToString(", ") { "${it.name}()" }
        val superClassesString = superClasses.joinToString("\n")
        val innerClassesString = innerClasses.joinToString("\n")
        val genericString = if (isGeneric()) "<T: Any>" else ""
        val enumConstants = if (hasModifier { m -> m == ClassModifier.ENUM }) "A_(${
            primaryConstructor.getParameterTypes().joinToString(separator = ", ") { t -> t.defaultValue }
        }), B_(${
            primaryConstructor.getParameterTypes().joinToString(separator = ", ") { t -> t.defaultValue }
        }), C_(${
            primaryConstructor.getParameterTypes().joinToString(separator = ", ") { t -> t.defaultValue }
        });" else ""

        return "${if (hasModifier { m -> m == ClassModifier.VALUE }) "@JvmInline" else ""}\n$annotationsString\n$superClassesString\n$modifiersString ${if (isFunctionalInterface) "fun interface" else if (isInterface) "interface" else "class"} $name $genericString ${
            if (isInterface || isFunctionalInterface) "" else if (hasModifier { it == ClassModifier.ANNOTATION }) "(${
                primaryConstructor.parameters.joinToString(
                    ", "
                ) { if (it.mutability == MutabilityQualifier.VARARG) "vararg val ${it.name}: ${it.type}" else it.toString() }
            })" else primaryConstructor.toString()
        } ${if (superClassesListString != "") ": $superClassesListString" else ""} {\n$enumConstants\n$secondaryConstructorsString\n$fieldsString\n$methodsString\n$operatorsString\n$innerClassesString\n${companionObject ?: ""}\n}"
    }
}

val possibleTypes = mutableMapOf<String, Type>()
private val classNames = mutableListOf<String>()
val qualifiedNameToFeature = mutableMapOf<String, Feature>()

private fun <T> FuzzedDataProvider.pickFromArray(arr: Array<T>) = arr[consumeInt(0, arr.lastIndex)]

private fun FuzzedDataProvider.generateType(): Type {
    return pickFromArray(possibleTypes.values.toTypedArray())
}

private fun FuzzedDataProvider.generateMutabilityQualifier(): MutabilityQualifier {
    return pickFromArray(MutabilityQualifier.values())
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
    declaredClasses: List<String>,
    isClassGeneric: Boolean,
    superClasses: List<ClassFeature>,
    availableAnnotations: List<String>
): MethodFeature {
    val methodFeature = MethodFeature(
        consumeBoolean(),
        !isClassGeneric || consumeBoolean(),
        superClasses,
        generateType(),
        "${if (consumeBoolean()) "${pickFromArray(declaredClasses.toTypedArray())}." else ""}${generateName()}"
    )

    val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
    for (i in 0 until parametersNumber) {
        val param = generateParameterFeature(availableAnnotations) ?: continue
        methodFeature.addParameter(param)
    }

    val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
    for (i in 0 until modifiersNumber) {
        methodFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
    }

    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            methodFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
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
    declaredClasses: List<String>,
    isClassGeneric: Boolean,
    superClasses: List<ClassFeature>,
    availableAnnotations: List<String>,
    className: String,
    operatorName: String
): OperatorFeature {
    when (operatorName) {
        "unaryPlus", "unaryMinus", "not", "inc", "dec" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= this",
                Type(className, ""),
                operatorName
            )

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "plus", "minus", "times", "div", "rem", "rangeTo" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= this",
                Type(className, ""),
                operatorName
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for binary operator")
                val parameter = generateParameterFeature(availableAnnotations, Type(className, "")) ?: continue
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
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "compareTo" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= 0",
                INT,
                operatorName
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for compareTo")
                val parameter = generateParameterFeature(availableAnnotations, Type(className, "")) ?: continue
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
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for binary assignment")
                val parameter = generateParameterFeature(availableAnnotations, Type(className, "")) ?: continue
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
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "invoke" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName
            )

            val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
            for (i in 0 until parametersNumber) {
                val param = generateParameterFeature(availableAnnotations) ?: continue
                operatorFeature.addParameter(param)
            }

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "get" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName
            )

            var count = 0
            while (operatorFeature.getParametersNumber() < 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for get operator")

                val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
                for (i in 0 until parametersNumber) {
                    val param = generateParameterFeature(availableAnnotations) ?: continue
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
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "set" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                operatorName
            )

            var count = 0
            while (operatorFeature.getParametersNumber() < 2) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for set operator")

                val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
                for (i in 0 until parametersNumber) {
                    val param = generateParameterFeature(availableAnnotations) ?: continue
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
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "contains" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "= true",
                BOOLEAN,
                operatorName
            )

            var count = 0
            while (operatorFeature.getParametersNumber() != 1) {
                if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate enough arguments for contains operator")
                val parameter = generateParameterFeature(availableAnnotations) ?: continue
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
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        "component" -> {
            val operatorFeature = OperatorFeature(
                if (consumeBoolean()) pickFromArray(declaredClasses.toTypedArray()) else null,
                consumeBoolean(),
                !isClassGeneric || consumeBoolean(),
                superClasses,
                "{}",
                VOID,
                "$operatorName${consumeInt(1, 10)}"
            )

            val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
            for (i in 0 until modifiersNumber) {
                operatorFeature.addModifier(generateModifier(FunctionModifier::class.java) as Modifier)
            }

            if (availableAnnotations.isNotEmpty()) {
                val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
                for (i in 0 until annotationsNumber) {
                    operatorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
                }
            }

            return operatorFeature
        }

        else -> throw IllegalArgumentException("name must be an operator")
    }
}

private fun FuzzedDataProvider.generateParameterFeature(
    availableAnnotations: List<String>,
    type: Type? = null
): ParameterFeature? {
    try {
        val parameterFeature = ParameterFeature(type ?: generateType(), generateMutabilityQualifier(), generateName())
        val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
        for (i in 0 until modifiersNumber) {
            parameterFeature.addModifier(generateModifier(ParameterModifier::class.java) as Modifier)
        }

        if (availableAnnotations.isNotEmpty()) {
            val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
            for (i in 0 until annotationsNumber) {
                parameterFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
            }
        }
        return parameterFeature
    } catch (e: IllegalArgumentException) {
        return null
    }
}

private fun FuzzedDataProvider.generateSecondaryConstructorFeature(
    isClassSealed: Boolean,
    parameterTypes: List<Type>,
    availableAnnotations: List<String>
): SecondaryConstructorFeature {
    val secondaryConstructorFeature = SecondaryConstructorFeature(isClassSealed, parameterTypes)

    val parametersNumber = consumeInt(0, MAX_PARAMETERS_NUMBER)
    for (i in 0 until parametersNumber) {
        val param = generateParameterFeature(availableAnnotations) ?: continue
        secondaryConstructorFeature.addParameter(param)
    }

    val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
    for (i in 0 until modifiersNumber) {
        secondaryConstructorFeature.addModifier(generateModifier(ConstructorModifier::class.java) as Modifier)
    }

    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            secondaryConstructorFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
        }
    }

    return secondaryConstructorFeature
}

private fun FuzzedDataProvider.generatePrimaryConstructorFeature(
    minArguments: Int,
    maxArguments: Int,
    availableAnnotations: List<String>,
    isAnnotation: Boolean,
): PrimaryConstructorFeature {
    val primaryConstructorFeature = PrimaryConstructorFeature(isAnnotation)

    val parametersNumber = consumeInt(minArguments, maxArguments)
    var count1 = 0
    var count2 = 0
    while (count1 != parametersNumber) {
        count2 += 1
        if (count2 > MAX_TRIES) {
            throw FuzzingStateException("Can't generate enough arguments for primary constructor")
        }
        val param = generateParameterFeature(availableAnnotations) ?: continue
        if ((param.hasModifier { m -> m.modifierType == ModifierType.INLINE } ||
                    param.mutability == MutabilityQualifier.EMPTY || param.mutability == MutabilityQualifier.VARARG)
            && minArguments != 0) continue
        primaryConstructorFeature.addParameter(param)
        count1 += 1
    }
    return primaryConstructorFeature
}

private fun FuzzedDataProvider.generateFieldFeature(
    availableAnnotations: List<String>,
    declaredClasses: List<String>
): FieldFeature? {
    try {
        val fieldFeature = FieldFeature(
            generateType(),
            generateMutabilityQualifier(),
            "${if (consumeBoolean()) "${pickFromArray(declaredClasses.toTypedArray())}." else ""}${generateName()}",
            consumeBoolean(),
            consumeBoolean()
        )
        val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
        for (i in 0 until modifiersNumber) {
            fieldFeature.addModifier(generateModifier(ParameterModifier::class.java) as Modifier)
        }

        if (availableAnnotations.isNotEmpty()) {
            val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
            for (i in 0 until annotationsNumber) {
                fieldFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
            }
        }
        return fieldFeature
    } catch (e: IllegalArgumentException) {
        return null
    }
}

private fun FuzzedDataProvider.generateCompanionObject(
    declaredClasses: List<String>,
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
        val field = generateFieldFeature(availableAnnotations, declaredClasses) ?: continue
        companionObjectFeature.addField(field)
    }

    val methodsNumber = consumeInt(0, MAX_METHODS_NUMBER)
    for (i in 0 until methodsNumber) {
        companionObjectFeature.addMethod(
            generateMethodFeature(
                declaredClasses,
                false,
                emptyList(),
                availableAnnotations
            )
        )
    }

    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            companionObjectFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
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

    val classFeature = ClassFeature(maxDepth != MAX_DEPTH, consumeBoolean(), namePrefix != "", name)

    if (isAnnotation) classFeature.addModifier(ClassModifier.ANNOTATION)

    val modifiersNumber = consumeInt(0, MAX_MODIFIERS_NUMBER)
    for (i in 0 until modifiersNumber) {
        classFeature.addModifier(generateModifier(ClassModifier::class.java) as Modifier)
    }

    val superClassesNumber = consumeInt(0, MAX_SUPERCLASSES_NUMBER)
    val superClasses: MutableList<ClassFeature> = mutableListOf()
    for (i in 0 until superClassesNumber) {
        val superClass = generateClassFeature(maxDepth, false, availableAnnotations, namePrefix)
        superClasses.add(superClass)
        classFeature.addSuperClass(superClass)
    }

    val primaryConstructor =
        generatePrimaryConstructorFeature(
            if (classFeature.hasModifier { m -> m == ClassModifier.DATA || m == ClassModifier.VALUE }) 1 else 0,
            if (classFeature.hasModifier { m -> m == ClassModifier.VALUE }) 1 else MAX_PARAMETERS_NUMBER,
            availableAnnotations,
            classFeature.hasModifier { m -> m == ClassModifier.ANNOTATION }
        )
    classFeature.setPrimaryConstructor(primaryConstructor)

    val constructorsNumber = consumeInt(0, MAX_CONSTRUCTORS_NUMBER)
    for (i in 0 until constructorsNumber) {
        classFeature.addSecondaryConstructor(
            generateSecondaryConstructorFeature(
                classFeature.hasModifier { m -> m == ClassModifier.SEALED },
                primaryConstructor.getParameterTypes(),
                availableAnnotations
            )
        )
    }

    if (!isAnnotation)
        possibleTypes[name] = Type("$namePrefix$name", "$namePrefix$name(${
            primaryConstructor.getParameterTypes().joinToString(", ") { t -> t.defaultValue }
        })")

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
        val field = generateFieldFeature(availableAnnotations, declaredClasses.map { x -> x.first }) ?: continue
        classFeature.addField(field)
    }

    var count = 0
    while (true) {
        val methodsNumber = consumeInt(0, MAX_METHODS_NUMBER)
        for (i in 0 until methodsNumber) {
            classFeature.addMethod(
                generateMethodFeature(
                    declaredClasses.map { x -> x.first },
                    classFeature.isGeneric(),
                    superClasses,
                    availableAnnotations
                )
            )
        }

        val operatorsNumber = consumeInt(0, MAX_OPERATORS_NUMBER)
        for (i in 0 until operatorsNumber) {
            val operatorName = pickFromArray(operatorNames.toTypedArray())
            val operator = generateOperatorFeature(
                declaredClasses.map { x -> x.first },
                classFeature.isGeneric(),
                superClasses,
                availableAnnotations,
                name,
                operatorName
            )
            classFeature.addOperator(operator)
        }

        if (!classFeature.isFunctionalInterface) break
        if (classFeature.methodsNumber() == 1) break
        if (count >= MAX_TRIES) throw FuzzingStateException("Can't generate method for fun interface")
        count += 1
    }

    if (consumeBoolean()) {
        val companionObject = generateCompanionObject(
            declaredClasses.map { x -> x.first },
            maxDepth,
            availableAnnotations,
            "$namePrefix$name."
        )
        classFeature.setCompanionObject(companionObject)
    }


    if (availableAnnotations.isNotEmpty()) {
        val annotationsNumber = consumeInt(0, MAX_ANNOTATIONS_NUMBER)
        for (i in 0 until annotationsNumber) {
            classFeature.addAnnotation(pickFromArray(availableAnnotations.toTypedArray()))
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
    val annotations = mutableListOf<ClassFeature>()
    for (i in 0 until MAX_ANNOTATIONS_NUMBER) {
        annotations.add(generateClassFeature(MAX_DEPTH, true, annotations.map { a -> a.name }, ""))
    }

    val classFeature = generateClassFeature(MAX_DEPTH, false, annotations.map { a -> a.name }, "")

    for (annotation in annotations) {
        for ((qualifiedClassName, declaredClass) in annotation.getDeclaredClasses()) {
            for (feature in declaredClass.getFeatures()) {
                qualifiedNameToFeature["$qualifiedClassName.${feature.toShortString()}"] = feature
            }
        }
    }
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