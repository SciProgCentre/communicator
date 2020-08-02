package scientifik.communicator.zmq.platform

import org.zeromq.ZMQ
import org.zeromq.ZMsg

internal actual class ZmqSocket(internal val backendSocket: ZMQ.Socket) {

    actual fun connect(zmqAddress: String) {
        backendSocket.connect(zmqAddress)
    }

    actual fun bind(zmqAddress: String) {
        backendSocket.bind(zmqAddress)
    }

    actual fun setIdentity(identity: ByteArray) {
        backendSocket.identity = identity
    }

    actual fun close() {
        backendSocket.close()
    }

    /** zmsg_recv method (CZMQ) */
    actual fun recvMsg(): ZmqMsg =
            ZmqMsg(ZMsg.recvMsg(backendSocket))


}