package kotlinx.fuzz

internal fun String?.toBooleanOrTrue(): Boolean = this?.toBoolean() != false
internal fun String?.toBooleanOrFalse(): Boolean = this?.toBoolean() == true

internal fun String.asList(separator: String = ",") =
    this.split(separator)
        .map(String::trim)
        .filter(String::isNotEmpty)

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : Comparable<T>> ClosedRange<T>.isNotEmpty(): Boolean = this.isEmpty() == false
