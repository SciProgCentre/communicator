package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.SocketType
import org.zeromq.ZContext

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() : Closeable {
    internal val backendContext: ZContext = ZContext()

    actual fun createRouterSocket(): ZmqSocket = ZmqSocket(backendContext.createSocket(SocketType.ROUTER))
    actual fun createDealerSocket(): ZmqSocket = ZmqSocket(backendContext.createSocket(SocketType.DEALER))
    actual override fun close(): Unit = backendContext.close()
}