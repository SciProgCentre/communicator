package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() : Closeable {
    actual fun createRouterSocket(): ZmqSocket = TODO("Not yet implemented")
    actual fun createDealerSocket(): ZmqSocket = TODO("Not yet implemented")
    actual override fun close(): Unit = TODO()
}
