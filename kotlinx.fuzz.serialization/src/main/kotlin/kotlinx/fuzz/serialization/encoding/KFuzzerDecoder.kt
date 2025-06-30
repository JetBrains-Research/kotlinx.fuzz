package kotlinx.fuzz.serialization.encoding

import kotlinx.fuzz.serialization.KFuzzerOptions
import kotlinx.fuzz.serialization.KFuzzerSerialization
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

private val List<Annotation>.optionsProvider: KFuzzerOptions.Provider?
    get() {
        val options = firstNotNullOfOrNull { annotation ->
            annotation as? KFuzzerOptions
        } ?: return null
        val klass = options.klass
        val instance = klass.objectInstance
        instance ?: error("Make ${klass.qualifiedName} an object to be able to use it as KClassOptions.Provider")
        instance as? KFuzzerOptions.Provider ?: error("${klass.qualifiedName} doesn't implement KFuzzerOptions.Provider interface")
        return instance
    }

@OptIn(ExperimentalSerializationApi::class)
internal class KFuzzerDecoder(
    private val serialization: KFuzzerSerialization,
    private val descriptor: SerialDescriptor,
) : Decoder, CompositeDecoder {
    override val serializersModule: SerializersModule
        get() = serialization.serializersModule

    private var elementIndex = 0

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        val isNull = serialization.isNull
            ?: throw SerializationException("Null fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return !isNull(serialization.fuzzer)
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean {
        val boolean = serialization.boolean
            ?: throw SerializationException("Boolean fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return boolean(serialization.fuzzer)
    }

    override fun decodeByte(): Byte {
        val byte = serialization.byte
            ?: throw SerializationException("Byte fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return byte(serialization.fuzzer)
    }

    override fun decodeShort(): Short {
        val short = serialization.short
            ?: throw SerializationException("Short fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return short(serialization.fuzzer)
    }

    override fun decodeChar(): Char {
        val char = serialization.char
            ?: throw SerializationException("Char fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return char(serialization.fuzzer)
    }

    override fun decodeInt(): Int {
        val int = serialization.int
            ?: throw SerializationException("Int fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return int(serialization.fuzzer)
    }

    override fun decodeLong(): Long {
        val long = serialization.long
            ?: throw SerializationException("Long fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return long(serialization.fuzzer)
    }

    override fun decodeFloat(): Float {
        val float = serialization.float
            ?: throw SerializationException("Float fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return float(serialization.fuzzer)
    }

    override fun decodeDouble(): Double {
        val double = serialization.double
            ?: throw SerializationException("Double fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return double(serialization.fuzzer)
    }

    override fun decodeString(): String {
        val string = serialization.string
            ?: throw SerializationException("String fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return string(serialization.fuzzer)
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val range = 0 ..< enumDescriptor.elementsCount
        return serialization.fuzzer.int(range)
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        val annotations = descriptor.getElementAnnotations(0)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        return KFuzzerDecoder(serialization, descriptor)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // [this.serialization] should not change, since
        // decodeSerializableValue will be invoked before
        // beginStructure any time and it will construct a
        // new decoder.
        return KFuzzerDecoder(serialization, descriptor)
    }

    override fun endStructure(descriptor: SerialDescriptor) {}

    override fun <T> decodeSerializableValue(
        deserializer: DeserializationStrategy<T>,
    ): T {
        val annotations = deserializer.descriptor.annotations
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, deserializer.descriptor)
        return deserializer.deserialize(decoder)
    }

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        val collectionSize = serialization.collectionSize
            ?: throw SerializationException("Collection fuzzing was not properly set up. It might be done inside fuzzer.serialization { ... } block")
        return collectionSize(serialization.fuzzer)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (descriptor.elementsCount == elementIndex) {
            return CompositeDecoder.DECODE_DONE
        }
        return elementIndex++
    }

    override fun decodeBooleanElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Boolean {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeBoolean()
    }

    override fun decodeByteElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Byte {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeByte()
    }

    override fun decodeCharElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Char {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeChar()
    }

    override fun decodeShortElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Short {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeShort()
    }

    override fun decodeIntElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Int {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeInt()
    }

    override fun decodeLongElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Long {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeLong()
    }

    override fun decodeFloatElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Float {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeFloat()
    }

    override fun decodeDoubleElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): Double {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeDouble()
    }

    override fun decodeStringElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): String {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeString()
    }

    override fun decodeInlineElement(
        descriptor: SerialDescriptor,
        index: Int,
    ): KFuzzerDecoder {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        return KFuzzerDecoder(serialization, descriptor)
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeSerializableValue(deserializer)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? {
        val annotations = this.descriptor.getElementAnnotations(index)
        val optionsProvider = annotations.optionsProvider
        val serialization = this.serialization.apply(optionsProvider)
        val decoder = KFuzzerDecoder(serialization, descriptor)
        return decoder.decodeNullableSerializableValue(deserializer)
    }
}

private fun KFuzzerSerialization.apply(
    provider: KFuzzerOptions.Provider?,
): KFuzzerSerialization {
    provider ?: return this
    val builder = KFuzzerSerialization.Builder(this)
    with(provider) {
        builder.apply()
    }
    return builder.build()
}
