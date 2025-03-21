package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.plan.research.utils.ReflectionUtils.removeLongOps
import org.plan.research.utils.ReflectionUtils.sourceFunctions
import org.plan.research.utils.splitIntoChunks
import org.plan.research.utils.template

object RealSourceVsBuffer {
    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOps(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        template(source, buf, data, sourceFunctions)
    }

    val manyBytes by lazy { org.plan.research.utils.manyBytes}

    val manyBuf by lazy {Buffer().apply { write(manyBytes) }}
    val fastOps by lazy {
        sourceFunctions
            .removeLongOps()
            .filterNot { it.name == "readAtMostTo" }
            .toTypedArray()
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun randomOpsManyData(data: FuzzedDataProvider): Unit = with(data) {
        val source = manyBytes.inputStream().asSource().buffered()
        val buf: Source = manyBuf.copy()
        template(source, buf, data, fastOps)
    }


    val chunkedFunctions = sourceFunctions.splitIntoChunks(6, 20)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun chunkedTemplate(data: FuzzedDataProvider, functionsNumber: Int) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)

        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }

        template(source, buf, data, chunkedFunctions[functionsNumber])
    }

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch0(data: FuzzedDataProvider): Unit = chunkedTemplate(data, 0)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch1(data: FuzzedDataProvider): Unit = chunkedTemplate(data, 1)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch2(data: FuzzedDataProvider): Unit = chunkedTemplate(data, 2)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch3(data: FuzzedDataProvider): Unit = chunkedTemplate(data, 3)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch4(data: FuzzedDataProvider): Unit = chunkedTemplate(data, 4)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun ch5(data: FuzzedDataProvider): Unit = chunkedTemplate(data, 5)

    @FuzzTest(maxDuration = Constants.MAX_DURATION)
    fun readLineStrict(data: FuzzedDataProvider): Unit = with(data) {
        val initBytes = data.consumeBytes(Constants.INIT_BYTES_COUNT)
        val source = initBytes.inputStream().asSource().buffered()
        val buf: Source = Buffer().apply { write(initBytes) }
        template(source, buf, data, arrayOf(Source::readLineStrict))
    }
}


