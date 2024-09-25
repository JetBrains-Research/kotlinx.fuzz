package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.assertTrue

object JsonTests {
    private const val MAX_JSON_DEPTH = 10
    private const val MAX_STR_LENGTH = 100

    @FuzzTest(maxDuration = "1h")
    fun `json parsing`(data: FuzzedDataProvider) {
        val jsonString = generateJson(data)
        try {
            val serializer = Json.Default
            serializer.parseToJsonElement(jsonString)
        } catch (e: SerializationException) {
            if (e.javaClass.name != "kotlinx.serialization.json.internal.JsonDecodingException") {
                assertTrue(false, "Unexpected exception type: $e")
            } else {
                System.err.println(jsonString)
                e.printStackTrace(System.err)
            }
        }
    }

    private fun generateJson(data: FuzzedDataProvider): String = when {
        data.consumeBoolean() -> generateObject(data, 0)
        else -> generateArray(data, 0)
    }

    private fun generateObject(data: FuzzedDataProvider, depth: Int): String = buildString {
        appendLine("{")
        if (depth < MAX_JSON_DEPTH) {
            val elements = data.consumeInt(1, 100)
            repeat(elements) {
                val key = data.consumeAsciiString(MAX_STR_LENGTH)
                val value = generateElement(data, depth + 1)
                appendLine("\"$key\": $value${if (it < elements - 1) "," else ""}")
            }
        }
        appendLine("}")
    }

    private fun generateArray(data: FuzzedDataProvider, depth: Int): String = buildString {
        appendLine("[")
        if (depth < MAX_JSON_DEPTH) {
            val elements = data.consumeInt(1, 100)
            repeat(elements) {
                val value = generateElement(data, depth + 1)
                appendLine("$value${if (it < elements - 1) "," else ""}")
            }
        }
        appendLine("]")
    }

    private fun generateElement(data: FuzzedDataProvider, depth: Int): String = buildString {
        appendLine("{")
        val elements = data.consumeInt(1, 100)
        repeat(elements) {
            val key = data.consumeAsciiString(MAX_STR_LENGTH)
            val value = when (data.consumeInt(0, 6)) {
                0 -> data.consumeInt().toString()
                1 -> data.consumeLong().toString()
                2 -> data.consumeBoolean().toString()
                3 -> data.consumeDouble().toString()
                4 -> data.consumeAsciiString(MAX_STR_LENGTH)
                5 -> generateObject(data, depth + 1)
                else -> generateArray(data, depth + 1)
            }
            appendLine("\"$key\": \"$value\"${if (it < elements - 1) "," else ""}")
        }
        appendLine("}")

    }
}
