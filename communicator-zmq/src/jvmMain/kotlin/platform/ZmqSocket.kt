package space.kscience.communicator.zmq.platform

import io.ktor.utils.io.core.Closeable
import org.zeromq.ZMQ

internal actual class ZmqSocket(internal val handle: ZMQ.Socket) : Closeable {
    actual fun connect(zmqAddress: String) {
        handle.connect(zmqAddress)
    }

    actual fun bind(zmqAddress: String) {
        handle.bind(zmqAddress)
    }

    actual fun setIdentity(identity: ByteArray) {
        handle.identity = identity
    }

    actual override fun close(): Unit = handle.close()
    actual fun recv(): ByteArray = handle.recv()
}
