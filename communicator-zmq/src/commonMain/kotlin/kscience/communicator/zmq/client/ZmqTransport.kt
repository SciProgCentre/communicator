package kscience.communicator.zmq.client

import kotlinx.coroutines.CompletableDeferred
import kscience.communicator.api.Payload
import kscience.communicator.api.Transport

/**
 * Implements transport with ZeroMQ-based machinery. Associated server transport is
 * [scientifik.communicator.zmq.server.ZmqTransportServer].
 */
class ZmqTransport : Transport {
    private val client = Client()

    override suspend fun respond(address: String, name: String, payload: Payload): Payload {
        val deferred = CompletableDeferred<Payload>()

        client.makeQuery(Query(
            functionName = name,
            address = address,
            arg = payload,
            callback = ResultCallback(
                onResult = { deferred.complete(it) },
                onError = { deferred.completeExceptionally(it) }
            )
        ))

        return deferred.await()
    }

    override fun close(): Unit = client.close()
}
