@file:Suppress("RemoveRedundantCallsOfConversionMethods")

package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.plan.research.Constants.MAX_DURATION
import org.plan.research.Constants.MAX_OPERATIONS_NUMBER
import kotlin.test.assertEquals

object BuffersKt {
    val manyBytes by lazy {org.plan.research.utils.largeByteString}

    val arr by lazy {ByteArray(manyBytes.size * 100)}

    @FuzzTest(maxDuration = MAX_DURATION)
    fun snapshot(data: FuzzedDataProvider): Unit = with(data) {
        val buf = Buffer()
        val n = consumeInt(0, MAX_OPERATIONS_NUMBER)
        var written = 0
        repeat(n) {
            val from = consumeInt(0, manyBytes.size - 10)
            val to = consumeInt(from, manyBytes.size)
            val len = to - from
            buf.write(manyBytes, from, to)

            if (written + len > arr.size) return

            manyBytes.copyInto(arr, written, from, to)
            written += len
            assertEquals(buf.snapshot(), ByteString(arr).substring(0, written))
        }
    }

    val buf = Buffer().apply { write(manyBytes) }


    private inline fun ignoreIndexExceptions(block: () -> Unit) = try {
        block()
    } catch (e: RuntimeException) {
        if (e.message?.contains("Index") == false) throw e
        else Unit
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun indexOfByte(data: FuzzedDataProvider): Unit = with(data) {
        val b = consumeByte()
        val idx = consumeLong()
        ignoreIndexExceptions { buf.indexOf(b, idx) }
    }

    @FuzzTest(maxDuration = MAX_DURATION)
    fun indexOfByteString(data: FuzzedDataProvider): Unit = with(data) {
        if (data.consumeInt(0, 9) != 9) {
            indexOfByteStringSmallTarget()
            return
        }

        val size = (8192 * consumeRegularFloat(1.0f, 3f)).toInt()
        val startIndex = consumeInt(0, manyBytes.size.toInt() - size - 1)
        val isOriginal = data.consumeBoolean()
        val bs = if (isOriginal) {
            manyBytes.substring(startIndex.toInt(), startIndex.toInt() + size.toInt())
        } else { // mutating random byte
            manyBytes.toByteArray(startIndex.toInt(), startIndex.toInt() + size.toInt()).apply {
                val indx = consumeInt(0, this.size - 1)
                this[indx] = (this[indx] + 10).toByte()
            }.let { ByteString(it) }
        }
        val res = buf.indexOf(bs)
        if (isOriginal) {
            assertEquals(startIndex, res.toInt())
        } else {
            assertEquals(-1L, res)
        }
    }

    private fun FuzzedDataProvider.indexOfByteStringSmallTarget() {
        val bytes = ByteString(consumeBytes(consumeInt(1, 1000)))
        val idx = consumeLong()
        ignoreIndexExceptions { buf.indexOf(bytes, idx) }
    }
}
