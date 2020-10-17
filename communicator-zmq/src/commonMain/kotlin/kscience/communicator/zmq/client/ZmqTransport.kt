package kscience.communicator.zmq.client

import kscience.communicator.api.Payload
import kscience.communicator.api.Transport

/**
 * Implements transport with ZeroMQ-based machinery. Associated server transport is
 * [kscience.communicator.zmq.server.ZmqTransportServer].
 */
public class ZmqTransport : Transport {
    internal val client = Client()

    public override suspend fun respond(address: String, name: String, payload: Payload): Payload =
        respondImpl(address, name, payload)

    public override fun close(): Unit = client.close()
}

internal expect suspend fun ZmqTransport.respondImpl(
    address: String,
    name: String,
    payload: ByteArray
): ByteArray
