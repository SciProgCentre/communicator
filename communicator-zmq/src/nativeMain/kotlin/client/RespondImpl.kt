package space.kscience.communicator.zmq.client

import kotlinx.coroutines.delay
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

internal actual suspend fun ZmqTransportClient.respondImpl(
    host: String,
    port: Int,
    name: String,
    payload: ByteArray
): ByteArray {
    val atom = AtomicReference<Result<ByteArray>?>(null)

    makeQuery(Query(
        functionName = name,
        host = host,
        port = port,
        arg = payload,

        callback = ResultCallback(
            onResult = { atom.value = Result.success(it).freeze() },
            onError = { atom.value = Result.failure<ByteArray>(it).freeze() }
        )
    ))

    while (atom.value == null) delay(1)
    return checkNotNull(atom.value).getOrThrow()
}