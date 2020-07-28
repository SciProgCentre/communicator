package scientifik.communicator.zmq.platform

/** Constructor must create a context with its init method */
actual class ZMQContext actual constructor() {
    actual fun createRouterSocket(): ZMQSocket {
        TODO("Not yet implemented")
    }

    actual fun createDealerSocket(): ZMQSocket {
        TODO("Not yet implemented")
    }

    actual fun close() {
    }

}