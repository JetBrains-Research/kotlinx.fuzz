package kotlinx.fuzz

import com.github.curiousoddman.rgxgen.config.RgxGenOption
import com.github.curiousoddman.rgxgen.config.RgxGenProperties
import com.github.curiousoddman.rgxgen.model.RgxGenCharsDefinition
import com.github.curiousoddman.rgxgen.model.SymbolRange
import com.github.curiousoddman.rgxgen.model.WhitespaceChar
import com.github.curiousoddman.rgxgen.util.chars.CharList
import java.lang.reflect.Method
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.fuzz.KFuzzer.RegexConfiguration
import kotlinx.fuzz.config.KFuzzConfig

fun String?.toBooleanOrTrue(): Boolean = this?.toBoolean() != false
fun String?.toBooleanOrFalse(): Boolean = this?.toBoolean() == true

fun KFuzzConfig.reproducerPathOf(method: Method): Path =
    Path(global.reproducerDir.absolutePathString(), method.declaringClass.simpleName, method.name).absolute()

fun Path.listCrashes(): List<Path> = if (this.exists()) listDirectoryEntries("{crash-*,timeout-*,slow-unit-*}") else emptyList()

internal fun String.asList(separator: String = ",") =
    this.split(separator)
        .map(String::trim)
        .filter(String::isNotEmpty)

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : Comparable<T>> ClosedRange<T>.isNotEmpty(): Boolean = this.isEmpty() == false

internal fun CharacterSet.toRgxGenProperties(): RgxGenCharsDefinition = RgxGenCharsDefinition.of(
    ranges.map { SymbolRange.range(it.first.code, it.last.code) },
    CharList.charList(symbols.joinToString("")),
)

internal fun RegexConfiguration.asRegexProperties(): RgxGenProperties {
    val properties = RgxGenProperties()
    RgxGenOption.INFINITE_PATTERN_REPETITION.setInProperties(
        properties,
        this.maxInfinitePatternLength,
    )

    RgxGenOption.CASE_INSENSITIVE.setInProperties(properties, caseInsensitive)
    allowedCharacters?.let {
        RgxGenOption.DOT_MATCHES_ONLY.setInProperties(
            properties,
            it.toRgxGenProperties(),
        )
    }

    val rgxGenWhiteSpaces = WhitespaceChar.entries.associateBy { it.get() }
    RgxGenOption.WHITESPACE_DEFINITION.setInProperties(
        properties,
        allowedWhitespaces.map {
            rgxGenWhiteSpaces[it]
                ?: error("$it is not a valid whitespace character, valid characters are: ${WhitespaceChar.entries.map { it.get() }}")
        },
    )
    return properties
}
