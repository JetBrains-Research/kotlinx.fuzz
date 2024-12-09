package org.plan.research

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals

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

    @FuzzTest
    fun test(data: FuzzedDataProvider) {

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
