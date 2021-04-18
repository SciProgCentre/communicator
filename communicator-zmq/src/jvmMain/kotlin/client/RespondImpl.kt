package space.kscience.communicator.zmq.client

import kotlinx.coroutines.CompletableDeferred

internal actual suspend fun ZmqTransportClient.respondImpl(
    host: String,
    port: Int,
    name: String,
    payload: ByteArray,
): ByteArray {
    val deferred = CompletableDeferred<ByteArray>()

    makeQuery(Query(
        functionName = name,
        host = host,
        port = port,
        arg = payload,
        callback = ResultCallback(
            onResult = { deferred.complete(it) },
            onError = { deferred.completeExceptionally(it) }
        )
    ))

    return deferred.await()
}
