package kotlinx.fuzz.serialization

import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.serialization.encoding.KFuzzerDecoder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

val KFuzzer.serialization: KFuzzerSerialization
    get() = KFuzzerSerialization(fuzzer = this)

@Suppress("USE_DATA_CLASS")
class KFuzzerSerialization(
    val fuzzer: KFuzzer,
    val serializersModule: SerializersModule = EmptySerializersModule(),
    val boolean: ((KFuzzer) -> Boolean)? = { fuzzer -> fuzzer.boolean() },
    val byte: ((KFuzzer) -> Byte)? = { fuzzer -> fuzzer.byte() },
    val short: ((KFuzzer) -> Short)? = { fuzzer -> fuzzer.short() },
    val char: ((KFuzzer) -> Char)? = { fuzzer -> fuzzer.char() },
    val int: ((KFuzzer) -> Int)? = { fuzzer -> fuzzer.int() },
    val long: ((KFuzzer) -> Long)? = { fuzzer -> fuzzer.long() },
    val float: ((KFuzzer) -> Float)? = { fuzzer -> fuzzer.float() },
    val double: ((KFuzzer) -> Double)? = { fuzzer -> fuzzer.double() },
    val string: ((KFuzzer) -> String)? = null,
    val collectionSize: ((KFuzzer) -> Int)? = null,
    val isNull: ((KFuzzer) -> Boolean)? = { fuzzer -> fuzzer.boolean() },
) {
    class Builder(serialization: KFuzzerSerialization) {
        var fuzzer: KFuzzer = serialization.fuzzer
        var serializersModule = serialization.serializersModule
        private var boolean: ((KFuzzer) -> Boolean)? = serialization.boolean
        private var byte: ((KFuzzer) -> Byte)? = serialization.byte
        private var short: ((KFuzzer) -> Short)? = serialization.short
        private var char: ((KFuzzer) -> Char)? = serialization.char
        private var int: ((KFuzzer) -> Int)? = serialization.int
        private var long: ((KFuzzer) -> Long)? = serialization.long
        private var float: ((KFuzzer) -> Float)? = serialization.float
        private var double: ((KFuzzer) -> Double)? = serialization.double
        private var string: ((KFuzzer) -> String)? = serialization.string
        private var collectionSize: ((KFuzzer) -> Int)? = serialization.collectionSize
        private var isNull: ((KFuzzer) -> Boolean)? = serialization.isNull
        fun boolean(block: ((KFuzzer) -> Boolean)?) {
            boolean = block
        }
        fun byte(block: ((KFuzzer) -> Byte)?) {
            byte = block
        }
        fun short(block: ((KFuzzer) -> Short)?) {
            short = block
        }
        fun char(block: ((KFuzzer) -> Char)?) {
            char = block
        }
        fun int(block: ((KFuzzer) -> Int)?) {
            int = block
        }
        fun long(block: ((KFuzzer) -> Long)?) {
            long = block
        }
        fun float(block: ((KFuzzer) -> Float)?) {
            float = block
        }
        fun double(block: ((KFuzzer) -> Double)?) {
            double = block
        }
        fun string(block: ((KFuzzer) -> String)?) {
            string = block
        }
        fun collectionSize(block: ((KFuzzer) -> Int)?) {
            collectionSize = block
        }
        fun isNull(block: ((KFuzzer) -> Boolean)?) {
            isNull = block
        }

        fun build(): KFuzzerSerialization = KFuzzerSerialization(
            fuzzer = fuzzer,
            serializersModule = serializersModule,
            boolean = boolean,
            byte = byte,
            short = short,
            char = char,
            int = int,
            long = long,
            float = float,
            double = double,
            string = string,
            collectionSize = collectionSize,
            isNull = isNull,
        )
    }
}

inline fun KFuzzer.serialization(
    block: KFuzzerSerialization.Builder.() -> Unit,
): KFuzzerSerialization {
    val serialization = KFuzzerSerialization(fuzzer = this)
    val builder = KFuzzerSerialization.Builder(serialization)
    builder.apply(block)
    return builder.build()
}

fun <T> KFuzzerSerialization.fuzz(serializer: DeserializationStrategy<T>): T {
    val decoder = KFuzzerDecoder(
        serialization = this,
        descriptor = serializer.descriptor,
    )
    return decoder.decodeSerializableValue(serializer)
}

inline fun <reified T> KFuzzerSerialization.fuzz(): T = fuzz(serializersModule.serializer<T>())
