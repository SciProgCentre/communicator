package kscience.communicator.zmq.client

internal actual suspend fun ZmqTransport.respondImpl(
    address: String,
    name: String,
    payload: ByteArray
): ByteArray = TODO()
