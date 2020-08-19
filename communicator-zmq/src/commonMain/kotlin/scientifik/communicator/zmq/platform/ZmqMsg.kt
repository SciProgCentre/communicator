package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal expect class ZmqMsg() : Closeable {
    fun add(data: ByteArray)
    fun add(frame: ZmqFrame)
    fun pop(): ZmqFrame
    fun send(socket: ZmqSocket)
    override fun close()
}
