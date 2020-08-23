package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** zframe_t object (CZMQ). */
internal expect class ZmqFrame : Closeable {
    val data: ByteArray

    companion object {
        fun recvFrame(socket: ZmqSocket): ZmqFrame
    }
}
