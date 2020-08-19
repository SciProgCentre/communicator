package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable

internal expect class ZmqSocket : Closeable {
    fun connect(zmqAddress: String)
    fun bind(zmqAddress: String)
    fun setIdentity(identity: ByteArray)

    /** zmsg_recv method (CZMQ) */
    fun recvMsg(): ZmqMsg

    override fun close()
}
