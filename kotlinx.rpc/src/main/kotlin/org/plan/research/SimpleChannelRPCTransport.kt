package org.plan.research

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.rpc.krpc.RPCTransport
import kotlinx.rpc.krpc.RPCTransportMessage
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
