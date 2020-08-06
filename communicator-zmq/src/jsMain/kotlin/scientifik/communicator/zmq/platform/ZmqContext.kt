package scientifik.communicator.zmq.platform

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() {
    actual fun createRouterSocket(): ZmqSocket {
        TODO("Not yet implemented")
    }

    actual fun createDealerSocket(): ZmqSocket {
        TODO("Not yet implemented")
    }

    actual fun close() {
    }

}