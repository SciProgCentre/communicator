package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame : Closeable {
    actual val data: ByteArray
        get() = TODO("Not yet implemented")

    override fun close(): Unit = TODO()

    actual companion object {
        actual fun recvFrame(socket: ZmqSocket): ZmqFrame = TODO()
    }
}
