package scientifik.communicator.zmq.platform

import org.zeromq.ZMQ
import org.zeromq.ZMsg

actual class ZMQSocket(val backupSocket: ZMQ.Socket) {

    actual fun connect(zmqAddress: String) {
        backupSocket.connect(zmqAddress)
    }

    actual fun bind(zmqAddress: String) {
        backupSocket.bind(zmqAddress)
    }

    actual fun setIdentity(identity: ByteArray) {
        backupSocket.identity = identity
    }

    actual fun close() {
        backupSocket.close()
    }

    /** zmsg_recv method (CZMQ) */
    actual fun recvMsg(): ZMQMsg =
            ZMQMsg(ZMsg.recvMsg(backupSocket))


}