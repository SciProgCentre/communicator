package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZMQ

internal actual class ZmqSocket(internal val backendSocket: ZMQ.Socket) : Closeable {
    actual fun connect(zmqAddress: String) {
        backendSocket.connect(zmqAddress)
    }

    actual fun bind(zmqAddress: String) {
        backendSocket.bind(zmqAddress)
    }

    actual fun setIdentity(identity: ByteArray) {
        backendSocket.identity = identity
    }

    actual override fun close(): Unit = backendSocket.close()
    actual fun recv(): ByteArray = backendSocket.recv()
}
