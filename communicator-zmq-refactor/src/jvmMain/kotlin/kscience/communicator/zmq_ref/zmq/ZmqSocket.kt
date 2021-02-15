package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable
import org.zeromq.ZMQ

internal actual class ZmqSocket(private val actualSocket: ZMQ.Socket) : Closeable {
    actual fun connect(zmqAddress: String) {
        actualSocket.connect(zmqAddress)
    }

    actual fun bind(zmqAddress: String) {
        actualSocket.connect(zmqAddress)
    }

    actual fun setIdentity(identity: ByteArray) {
        actualSocket.identity = identity
    }

    actual fun recv(): ZmqMessage {
        val msg = actualSocket.recv()
        //TODO: not implemented
    }

    actual suspend fun suspendRecv(): ZmqMessage {
        TODO("Not yet implemented")
    }

    actual fun send(message: ZmqMessage, block: Boolean) {
    }

    actual override fun close() {

    }

}