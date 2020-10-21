package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.SocketType
import org.zeromq.ZContext

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() : Closeable {
    internal val handle: ZContext = ZContext()

    actual fun createRouterSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.ROUTER))
    actual fun createDealerSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.DEALER))
    actual override fun close(): Unit = handle.close()
    actual fun createPairSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.PAIR))
    actual fun createPubSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.PUB))
    actual fun createSubSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.SUB))
    actual fun createReqSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.REQ))
    actual fun createRepSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.REP))
    actual fun createPullSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.PULL))
    actual fun createPushSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.PUSH))
    actual fun createXPubSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.XPUB))
    actual fun createXSubSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.XSUB))
    actual fun createStreamSocket(): ZmqSocket = ZmqSocket(handle.createSocket(SocketType.STREAM))
}
