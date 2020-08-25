package kscience.communicator.zmq.platform

import czmq.zsock_new_dealer
import czmq.zsock_new_router
import czmq.zsys_init
import kotlinx.io.Closeable

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() : Closeable {
    private val sockets: MutableList<ZmqSocket> = mutableListOf()

    init {
        zsys_init()
    }

    actual fun createRouterSocket(): ZmqSocket = ZmqSocket(checkNotNull(zsock_new_router(null))).also { sockets += it }
    actual fun createDealerSocket(): ZmqSocket = ZmqSocket(checkNotNull(zsock_new_dealer(null))).also { sockets += it }
    actual override fun close(): Unit = sockets.forEach(ZmqSocket::close)
}
