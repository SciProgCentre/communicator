package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal expect class ZmqMsg() : Closeable, MutableCollection<ZmqFrame> {
    fun add(data: ByteArray): Boolean
    fun pop(): ZmqFrame
    fun send(socket: ZmqSocket)
    fun copy(): ZmqMsg
    override fun close()
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String

    companion object {
        fun recvMsg(socket: ZmqSocket): ZmqMsg
    }
}
