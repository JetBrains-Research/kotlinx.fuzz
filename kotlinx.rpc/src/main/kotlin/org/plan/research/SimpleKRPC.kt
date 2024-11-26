package org.plan.research

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.rpc.krpc.RPCConfig
import kotlinx.rpc.krpc.RPCConfigBuilder
import kotlinx.rpc.krpc.RPCTransport
import kotlinx.rpc.krpc.RPCTransportMessage
import kotlinx.rpc.krpc.client.KRPCClient
import kotlinx.rpc.krpc.rpcClientConfig
import kotlinx.rpc.krpc.rpcServerConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.serialization.protobuf.protobuf
import kotlinx.rpc.krpc.server.KRPCServer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.coroutines.CoroutineContext

typealias MessageChannel = Channel<RPCTransportMessage>

fun MessageChannel() = Channel<RPCTransportMessage>()

class SimpleChannelRPCTransport(
    private val channel: MessageChannel
) : RPCTransport {
    override val coroutineContext: CoroutineContext = Job()

    override suspend fun send(message: RPCTransportMessage) {
        channel.send(message)
    }

    override suspend fun receive(): RPCTransportMessage {
        return channel.receive()
    }
}

enum class SerializationType {
    STRING, BINARY
}

@OptIn(ExperimentalSerializationApi::class)
class SimpleKRPCClient(
    channel: MessageChannel,
    serializationType: SerializationType,
    clientConfig: RPCConfigBuilder.Client.() -> Unit
) : KRPCClient(
    rpcClientConfig {
        serialization {
            when(serializationType) {
                SerializationType.STRING -> json()
                SerializationType.BINARY -> protobuf()
            }
        }
        clientConfig()
    },
    SimpleChannelRPCTransport(channel),
)

@OptIn(ExperimentalSerializationApi::class)
class SimpleKRPCServer(
    channel: MessageChannel,
    serializationType: SerializationType,
    serverConfig: RPCConfigBuilder.Server.() -> Unit
) : KRPCServer(
    rpcServerConfig {
        serialization {
            when(serializationType) {
                SerializationType.STRING -> json()
                SerializationType.BINARY -> protobuf()
            }
        }
        serverConfig()
    },
    SimpleChannelRPCTransport(channel),
)
