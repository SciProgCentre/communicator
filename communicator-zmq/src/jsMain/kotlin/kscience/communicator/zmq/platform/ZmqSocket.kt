package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

internal actual class ZmqSocket : Closeable {
    actual fun connect(zmqAddress: String): Unit = TODO()
    actual fun bind(zmqAddress: String): Unit = TODO()
    actual fun setIdentity(identity: ByteArray): Unit = TODO()
    override fun close(): Unit = TODO()
    actual fun recv(): ByteArray = TODO()
}
