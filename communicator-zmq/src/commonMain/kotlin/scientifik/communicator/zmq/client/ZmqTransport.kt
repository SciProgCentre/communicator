package scientifik.communicator.zmq.client

import kotlinx.coroutines.CompletableDeferred
import scientifik.communicator.api.Payload
import scientifik.communicator.api.Transport

class ZmqTransport : Transport {
    private val client = Client()

    override suspend fun respond(address: String, name: String, payload: Payload): Payload {
        val deferred = CompletableDeferred<ByteArray>()

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
}
