package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable

internal enum class ZmqSocketType {
    ROUTER, DEALER, PAIR, PUSH, PULL, REPLY
}

internal enum class ZmqProtocol(val str: String) {
    inproc("inproc"), tcp("tcp")
}

internal fun makeZmqAddress(protocol: ZmqProtocol, address: String): String {
    return makeZmqAddress(protocol.str, address)
}

internal fun makeZmqAddress(protocol: String, address: String): String {
    return "$protocol://$address"
}

/** Constructor must create a context with its init method */
internal expect class ZmqContext() : Closeable {
    fun createSocket(type: ZmqSocketType): ZmqSocket
    fun createLoop(): ZmqLoop

    override fun close()
}