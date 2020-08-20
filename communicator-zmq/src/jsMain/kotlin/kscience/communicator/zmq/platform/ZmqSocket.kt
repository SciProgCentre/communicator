package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

internal actual class ZmqSocket : Closeable {
    actual fun connect(zmqAddress: String): Unit = TODO()
    actual fun bind(zmqAddress: String): Unit = TODO()
    actual fun setIdentity(identity: ByteArray): Unit = TODO()
    actual override fun close(): Unit = TODO()

    /** zmsg_recv method (CZMQ) */
    actual fun recvMsg(): ZmqMsg = TODO()
}
