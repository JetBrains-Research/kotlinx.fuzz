package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.rpc.internal.utils.InternalRPCApi
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

import kotlinx.rpc.krpc.internal.logging.CommonLogger
import kotlin.math.log
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Things we would like to try:
 *  - return flows of all kinds
 *  - throw exceptions in server
 *      - and in general all kinds of cancel / finish
 *      - transport end
 *  - multiple concurrent clients to mix the control messages
 *      - (need separate transports? not that interesting if yes)
 *  - string / binary
 *      - they have separate control messages
 *  - fuzz all config params
 *
 * Things we DON'T want to try:
 *  - many return types (it only tests serialization)
 *  - ktor
 */

@Rpc
interface AwesomeService : RemoteService {
    suspend fun getNews(city: String): String
}

class AImpl(override val coroutineContext: CoroutineContext) : AwesomeService {
    override suspend fun getNews(city: String): String {
        return "$city news"
    }
}

class A {

//    @Test
//    fun haha() {
////        val f: Unit.(block: Unit.() -> Unit) -> Result<Unit> = ::runCatching
//        val f: (() -> Unit) -> Result<Unit> = ::runCatching
//        println(f::class.java.name)
//        val a = runCatching { 5 }
//        a.isFailure
//    }

//    @BeforeTest
//    fun mock() {
//        mockLogger()
//    }

//    @Test
//    fun test() {
    @FuzzTest
    fun test(data: FuzzedDataProvider) {

//        val k = KLAL()
//        k.haha()
//        println("oh no $k")

        runBlocking {
            val channel = MessageChannel()

//            val serializationType = data.pickValue(SerializationType.entries.toTypedArray())
//            val waitForServices = data.consumeBoolean()
            // TODO: if true, maybe init server after client? maybe randomly?
            val serializationType = SerializationType.BINARY
            val waitForServices = true

            val server = SimpleKRPCServer(channel, serializationType) {
                this.waitForServices = waitForServices
            }
            server.registerService(AwesomeService::class) { ctx -> AImpl(ctx) }

            val client = SimpleKRPCClient(channel, serializationType) {
                this.waitForServices = waitForServices
            }
            val clientService = client.withService<AwesomeService>()

            val city = "LA"
            val result = clientService.getNews(city)
            assertEquals("$city news", result)
        }
    }

}
