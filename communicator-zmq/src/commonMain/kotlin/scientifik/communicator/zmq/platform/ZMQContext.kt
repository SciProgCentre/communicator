package scientifik.communicator.zmq.platform

/** Constructor must create a context with its init method */
expect class ZMQContext() {

    fun createRouterSocket(): ZMQSocket
    fun createDealerSocket(): ZMQSocket

    fun close()

}