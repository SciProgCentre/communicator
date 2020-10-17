package space.kscience.communicator.zmq.platform

import io.ktor.utils.io.core.Closeable

internal expect class ZmqSocket : Closeable {
    fun connect(zmqAddress: String)
    fun bind(zmqAddress: String)
    fun setIdentity(identity: ByteArray)
    fun recv(): ByteArray
    override fun close()
}
