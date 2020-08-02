package scientifik.communicator.zmq.platform

import org.zeromq.SocketType
import org.zeromq.ZContext

/** Constructor must create a context with its init method */
actual class ZmqContext actual constructor() {

    val backupContext = ZContext()

    actual fun createRouterSocket(): ZmqSocket =
            ZmqSocket(backupContext.createSocket(SocketType.ROUTER))

    actual fun createDealerSocket(): ZmqSocket =
            ZmqSocket(backupContext.createSocket(SocketType.DEALER))


    actual fun close() {
        backupContext.close()
    }

}