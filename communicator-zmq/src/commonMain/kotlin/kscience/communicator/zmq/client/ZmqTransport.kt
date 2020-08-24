package kscience.communicator.zmq.client

import kscience.communicator.api.Payload
import kscience.communicator.api.Transport

/**
 * Implements transport with ZeroMQ-based machinery. Associated server transport is
 * [kscience.communicator.zmq.server.ZmqTransportServer].
 */
class ZmqTransport : Transport {
    internal val client = Client()

    override suspend fun respond(address: String, name: String, payload: Payload): Payload =
        respondImpl(address, name, payload)

    override fun close(): Unit = client.close()
}

internal expect suspend fun ZmqTransport.respondImpl(
    address: String,
    name: String,
    payload: ByteArray
): ByteArray
