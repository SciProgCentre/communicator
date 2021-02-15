package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable
import org.zeromq.SocketType
import org.zeromq.ZContext

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() : Closeable {
    private val actualContext: ZContext = ZContext()

    actual fun createSocket(type: ZmqSocketType) {
        val actualType = when (type) {
            ZmqSocketType.PUSH -> SocketType.PUSH
            ZmqSocketType.PULL -> SocketType.PUSH
            ZmqSocketType.ROUTER -> SocketType.ROUTER
            ZmqSocketType.DEALER -> SocketType.DEALER
            ZmqSocketType.PAIR -> SocketType.PAIR
            ZmqSocketType.REPLY -> SocketType.REP
        }
        val actualSocket = actualContext.createSocket(actualType)
    }
    actual fun createLoop() {

    }

    override actual fun close() {

    }
}