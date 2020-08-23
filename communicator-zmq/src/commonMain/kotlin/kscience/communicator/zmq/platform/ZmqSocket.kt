package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

internal expect class ZmqSocket : Closeable {
    fun connect(zmqAddress: String)
    fun bind(zmqAddress: String)
    fun setIdentity(identity: ByteArray)
    fun recv(): ByteArray
}
