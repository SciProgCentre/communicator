package scientifik.communicator.zmq.platform

actual class ZmqSocket {
    actual fun connect(zmqAddress: String) {
    }

    actual fun bind(zmqAddress: String) {
    }

    actual fun setIdentity(identity: ByteArray) {
    }

    actual fun close() {
    }

    /** zmsg_recv method (CZMQ) */
    actual fun recvMsg(): ZmqMsg {
        TODO("Not yet implemented")
    }


}