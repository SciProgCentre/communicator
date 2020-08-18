package scientifik.communicator.zmq.platform

import org.zeromq.SocketType
import org.zeromq.ZContext

/** Constructor must create a context with its init method */
internal actual class ZmqContext actual constructor() {

    internal val backendContext = ZContext()

    actual fun createRouterSocket(): ZmqSocket =
            ZmqSocket(backendContext.createSocket(SocketType.ROUTER))

    actual fun createDealerSocket(): ZmqSocket =
            ZmqSocket(backendContext.createSocket(SocketType.DEALER))


    actual fun close() {
        backendContext.close()
    }

}