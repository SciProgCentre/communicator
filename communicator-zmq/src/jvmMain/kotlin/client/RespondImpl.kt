package space.kscience.communicator.zmq.client

import kotlinx.coroutines.CompletableDeferred

internal actual suspend fun ZmqTransport.respondImpl(
    address: String,
    name: String,
    payload: ByteArray,
): ByteArray {
    val deferred = CompletableDeferred<ByteArray>()

    makeQuery(Query(
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
