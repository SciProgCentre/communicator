package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.czmq.zsock_new_dealer
import org.zeromq.czmq.zsock_new_router
import org.zeromq.czmq.zsys_init

internal actual class ZmqContext actual constructor() : Closeable {
    private val sockets: MutableList<ZmqSocket> = mutableListOf()

    init {
        zsys_init()
    }

    actual fun createRouterSocket(): ZmqSocket = ZmqSocket(checkNotNull(zsock_new_router(null))).also { sockets += it }
    actual fun createDealerSocket(): ZmqSocket = ZmqSocket(checkNotNull(zsock_new_dealer(null))).also { sockets += it }
    actual override fun close(): Unit = sockets.forEach(ZmqSocket::close)
}
