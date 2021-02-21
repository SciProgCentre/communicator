package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable
import org.zeromq.ZMQ
import org.zeromq.ZMsg

internal actual class ZmqSocket(internal val actualSocket: ZMQ.Socket) : Closeable {
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
        return ZmqMessage(ZMsg.recvMsg(actualSocket))
    }

    actual suspend fun suspendRecv(): ZmqMessage {
        TODO("Not yet implemented")
    }

    actual fun send(message: ZmqMessage, block: Boolean) {
        //TODO: make non-blocking send work
        message.actualMessage.send(actualSocket)
    }

    actual override fun close() {
        actualSocket.close()
    }

}