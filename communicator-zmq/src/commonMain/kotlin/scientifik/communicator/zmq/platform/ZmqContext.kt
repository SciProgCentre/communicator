package scientifik.communicator.zmq.platform

/** Constructor must create a context with its init method */
expect class ZmqContext() {

    fun createRouterSocket(): ZmqSocket
    fun createDealerSocket(): ZmqSocket

    fun close()

}