package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** zframe_t object (CZMQ). */
internal expect class ZmqFrame : Closeable, Iterable<Byte> {
    val data: ByteArray

    fun copy(): ZmqFrame
    override fun close()
    override fun iterator(): Iterator<Byte>
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String

    companion object {
        fun recvFrame(socket: ZmqSocket): ZmqFrame
    }
}
