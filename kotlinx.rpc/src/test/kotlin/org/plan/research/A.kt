package org.plan.research

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.rpc.krpc.client.KRPCClient
import kotlinx.rpc.krpc.rpcClientConfig
import kotlinx.rpc.krpc.rpcServerConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.server.KRPCServer
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

@Rpc
interface AwesomeService : RemoteService {
    suspend fun getNews(city: String): String
}

class AImpl(override val coroutineContext: CoroutineContext) : AwesomeService {
    override suspend fun getNews(city: String): String {
        return "$city news"
    }
}

class SimpleKRPCClient(channel: MessageChannel) : KRPCClient(
    rpcClientConfig {
        serialization {
            json()
        }
    },
    SimpleChannelRPCTransport(channel),
)

class SimpleKRPCServer(channel: MessageChannel) : KRPCServer(
    rpcServerConfig {
        serialization {
            json()
        }
    },
    SimpleChannelRPCTransport(channel),
)

class A {

    @Test
    fun test(): Unit = runBlocking {
        val channel = MessageChannel()

        val server = SimpleKRPCServer(channel)
        val serverService = server.registerService(AwesomeService::class) { ctx -> AImpl(ctx) }

        val client = SimpleKRPCClient(channel)
        val clientService = client.withService<AwesomeService>()

        val city = "Verhnie Pupki"
        val result = clientService.getNews(city)
        assertEquals("$city news", result)
    }

}
