package kotlinx.fuzz.serialization

import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
annotation class KFuzzerOptions(val klass: KClass<*>) {
    fun interface Provider {
        fun KFuzzerSerialization.Builder.apply()
    }
}
