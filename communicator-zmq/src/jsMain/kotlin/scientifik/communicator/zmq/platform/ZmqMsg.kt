package scientifik.communicator.zmq.platform

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
actual class ZmqMsg actual constructor() {
    actual fun add(data: ByteArray) {
    }

    actual fun add(frame: ZmqFrame) {
    }

    actual fun pop(): ZmqFrame {
        TODO("Not yet implemented")
    }

    actual fun send(socket: ZmqSocket) {
    }

    actual fun close() {
    }

}