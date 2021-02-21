package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZLoop

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() : Closeable {
    private val actualContext: ZContext = ZContext()

    actual fun createSocket(type: ZmqSocketType): ZmqSocket {
        val actualType = when (type) {
            ZmqSocketType.PUSH -> SocketType.PUSH
            ZmqSocketType.PULL -> SocketType.PUSH
            ZmqSocketType.ROUTER -> SocketType.ROUTER
            ZmqSocketType.DEALER -> SocketType.DEALER
            ZmqSocketType.PAIR -> SocketType.PAIR
            ZmqSocketType.REPLY -> SocketType.REP
        }
        val actualSocket = actualContext.createSocket(actualType)
        return ZmqSocket(actualSocket)
    }

    actual fun createLoop(): ZmqLoop {
        val actualLoop = ZLoop(actualContext)
        return ZmqLoop(actualLoop)
    }

    actual override fun close() {
        actualContext.close()
    }
}