package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable

/** Constructor must create a context with its init method */
internal expect class ZmqContext() : Closeable {
    fun createRouterSocket(): ZmqSocket
    fun createDealerSocket(): ZmqSocket
    override fun close()
}
