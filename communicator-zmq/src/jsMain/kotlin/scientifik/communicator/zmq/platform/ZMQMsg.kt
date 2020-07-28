package scientifik.communicator.zmq.platform

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
actual class ZMQMsg actual constructor() {
    actual fun add(data: ByteArray) {
    }

    actual fun add(frame: ZMQFrame) {
    }

    actual fun pop(): ZMQFrame {
        TODO("Not yet implemented")
    }

    actual fun send(socket: ZMQSocket) {
    }

    actual fun close() {
    }

}